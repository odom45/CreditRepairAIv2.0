package com.creditrepairai.v2.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.ParsedReport
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException

class CreditReportParser(private val context: Context) {
    suspend fun parse(uri: Uri, fileName: String): ParsedReport = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Select a PDF through Android's document picker or share sheet." }
        val snapshot = File.createTempFile("credit-report-", ".pdf", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                snapshot.outputStream().use { output -> ParserLimits.copyPdf(input, output) }
            } ?: error("Could not open the selected PDF")
            currentCoroutineContext().ensureActive()
            val localUri = Uri.fromFile(snapshot)
            var extractionMethod = "Embedded PDF text"
            // Only extraction/format I/O failures may try OCR. Limits and cancellation propagate.
            var text = try { extractEmbeddedText(snapshot) } catch (_: IOException) { "" }
            if (text.count { !it.isWhitespace() } < 200) {
                extractionMethod = "On-device OCR"
                text = extractWithOcr(localUri)
            }

            require(text.count { !it.isWhitespace() } >= 80) {
                "The PDF did not contain enough readable text. Try an original downloaded report instead of a photo-only scan."
            }

            currentCoroutineContext().ensureActive()
            ParserLimits.text(text)
            val reader = ReportTextParser()
            val bureau = reader.detectBureau(fileName, text)
            val accounts = reader.parseAccounts(text, bureau)
            val score = reader.parseScore(text)
            ParsedReport(
                report = CreditReport(
                    fileName = fileName,
                    bureau = bureau,
                    score = score,
                    accountCount = accounts.size,
                    extractionMethod = extractionMethod,
                ),
                accounts = accounts,
                extractedCharacters = text.length,
            )
        } finally {
            snapshot.delete()
        }
    }

    private fun extractEmbeddedText(file: File): String {
        return PDDocument.load(file).use { document ->
            require(document.numberOfPages <= 200) { "PDF exceeds 200 pages. Export a bureau report separately." }
            val output = LimitedTextWriter()
            PDFTextStripper().writeText(document, output)
            output.toString()
        }
    }

    private suspend fun extractWithOcr(uri: Uri): String {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Could not open the selected PDF")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            PdfRenderer(descriptor).use { renderer ->
                buildString {
                    require(renderer.pageCount <= 30) { "Scanned PDF exceeds 30 pages. Import smaller report sections; no pages were silently skipped." }
                    val pageLimit = renderer.pageCount
                    for (index in 0 until pageLimit) {
                        currentCoroutineContext().ensureActive()
                        renderer.openPage(index).use { page ->
                            require(page.width > 0 && page.height > 0) { "PDF page dimensions are invalid." }
                            val targetWidth = 2048
                            val targetHeight = (targetWidth * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                            require(targetHeight <= 4096) { "PDF page is too tall for safe OCR. Export a standard-sized page." }
                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            try {
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                val pageText = withTimeout(30_000L) {
                                    orderedOcrText(recognizer.process(InputImage.fromBitmap(bitmap, 0)).await())
                                }
                                if (pageText.length + 1 > ParserLimits.TEXT_CHARACTERS - length) {
                                    throw ParserLimitException("Scanned PDF exceeds readable text limit. Import smaller report sections.")
                                }
                                appendLine(pageText)
                            } finally {
                                bitmap.recycle()
                            }
                        }
                    }
                }
            }
        } finally {
            recognizer.close()
            descriptor.close()
        }
    }

    private fun orderedOcrText(text: com.google.mlkit.vision.text.Text): String {
        data class Word(val text: String, val bounds: android.graphics.Rect)
        val words = mutableListOf<Word>()
        for (block in text.textBlocks) for (line in block.lines) for (element in line.elements) {
            if (words.size >= ParserLimits.OCR_WORDS) throw ParserLimitException("Scanned page exceeds OCR word limit.")
            element.boundingBox?.let { words.add(Word(element.text, it)) }
        }
        if (words.isEmpty()) return text.text
        data class Row(val words: MutableList<Word>, var centerSum: Double, var heightSum: Double)
        val rows = mutableListOf<Row>()
        for (word in words.sortedWith(compareBy<Word> { it.bounds.centerY() }.thenBy { it.bounds.left })) {
            val row = rows.lastOrNull()
            val center = row?.let { it.centerSum / it.words.size } ?: 0.0
            val height = minOf(word.bounds.height().toDouble(), row?.let { it.heightSum / it.words.size } ?: 0.0)
            if (row != null && kotlin.math.abs(word.bounds.centerY() - center) < (height * 0.45).coerceAtLeast(2.0)) {
                row.words.add(word)
                row.centerSum += word.bounds.centerY()
                row.heightSum += word.bounds.height()
            } else rows.add(Row(mutableListOf(word), word.bounds.centerY().toDouble(), word.bounds.height().toDouble()))
        }
        return rows.joinToString("\n") { row ->
            val ordered = row.words.sortedBy { it.bounds.left }
            buildString {
                ordered.forEachIndexed { index, word ->
                    val previous = ordered.getOrNull(index - 1)
                    if (previous != null) {
                        val advance = (previous.bounds.width().toDouble() / previous.text.length.coerceAtLeast(1) + word.bounds.width().toDouble() / word.text.length.coerceAtLeast(1)) / 2
                        val gap = word.bounds.left - previous.bounds.right
                        // Join only letter fragments separated by less than an estimated
                        // character advance. Real word spaces remain; no dictionary/name guess.
                        val contiguous = gap >= 0 && gap < advance * 0.60 && previous.text.lastOrNull()?.isLetter() == true && word.text.firstOrNull()?.isLetter() == true
                        if (!contiguous) append(' ')
                    }
                    append(word.text)
                }
            }
        }
    }
}
