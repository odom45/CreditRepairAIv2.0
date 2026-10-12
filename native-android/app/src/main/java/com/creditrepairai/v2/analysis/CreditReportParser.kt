package com.creditrepairai.v2.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
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
import java.text.NumberFormat
import java.util.Locale

class CreditReportParser(private val context: Context) {
    suspend fun parse(uri: Uri, fileName: String): ParsedReport = withContext(Dispatchers.IO) {
        require(uri.scheme == "content") { "Select a PDF through Android's document picker or share sheet." }
        var extractionMethod = "Embedded PDF text"
        var text = runCatching { extractEmbeddedText(uri) }.getOrDefault("")
        if (text.filterNot(Char::isWhitespace).length < 200) {
            extractionMethod = "On-device OCR"
            text = extractWithOcr(uri)
        }

        require(text.filterNot(Char::isWhitespace).length >= 80) {
            "The PDF did not contain enough readable text. Try an original downloaded report instead of a photo-only scan."
        }

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
    }

    private fun extractEmbeddedText(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytesLimited(50_000_000)
            require(bytes.take(5).toByteArray().toString(Charsets.US_ASCII) == "%PDF-") { "Selected file is not a PDF." }
            PDDocument.load(bytes).use { document ->
                require(document.numberOfPages <= 200) { "PDF exceeds 200 pages. Export a bureau report separately." }
                PDFTextStripper().getText(document)
            }
        }.orEmpty()
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
                        renderer.openPage(index).use { page ->
                            require(page.width > 0 && page.height > 0) { "PDF page dimensions are invalid." }
                            val targetWidth = 2048
                            val targetHeight = (targetWidth * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                            require(targetHeight <= 4096) { "PDF page is too tall for safe OCR. Export a standard-sized page." }
                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            try {
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                appendLine(orderedOcrText(recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()))
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

    private fun java.io.InputStream.readBytesLimited(limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            require(output.size() + count <= limit) { "PDF is larger than 50 MB. Export a smaller report." }
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun orderedOcrText(text: com.google.mlkit.vision.text.Text): String {
        data class Word(val text: String, val bounds: android.graphics.Rect)
        val words = text.textBlocks.flatMap { it.lines }.flatMap { it.elements }
            .mapNotNull { element -> element.boundingBox?.let { Word(element.text, it) } }
        if (words.isEmpty()) return text.text
        val rows = mutableListOf<MutableList<Word>>()
        for (word in words.sortedWith(compareBy<Word> { it.bounds.centerY() }.thenBy { it.bounds.left })) {
            val row = rows.lastOrNull()
            val center = row?.map { it.bounds.centerY() }?.average() ?: 0.0
            val height = minOf(word.bounds.height().toDouble(), row?.map { it.bounds.height() }?.average() ?: 0.0)
            if (row != null && kotlin.math.abs(word.bounds.centerY() - center) < (height * 0.45).coerceAtLeast(2.0)) row.add(word)
            else rows.add(mutableListOf(word))
        }
        return rows.joinToString("\n") { row ->
            val ordered = row.sortedBy { it.bounds.left }
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

/** Pure text parser used by Android import and permission-free JVM fixtures. */
class ReportTextParser {
    internal fun detectBureau(fileName: String, text: String): Bureau {
        val source = "$fileName\n${text.take(8_000)}".lowercase(Locale.US)
        return when {
            "transunion" in source || "trans union" in source -> Bureau.TRANSUNION
            "experian" in source -> Bureau.EXPERIAN
            "equifax" in source -> Bureau.EQUIFAX
            else -> Bureau.UNKNOWN
        }
    }

    internal fun parseScore(text: String): Int? {
        val scorePatterns = listOf(
            Regex("(?i)\\b(?:credit\\s+score|fico[®™ ]*(?:score)?|vantage(?:score)?[®™ ]*(?:score)?)[^0-9]{0,25}([3-8][0-9]{2})(?![0-9])"),
            Regex("(?i)(?:your|current) score[^0-9]{0,15}([3-8][0-9]{2})(?![0-9])"),
        )
        return scorePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 300..850 }
        }
    }

    internal fun parseAccounts(text: String, bureau: Bureau): List<CreditAccount> {
        val rawLines = text.lineSequence()
            .flatMap { it.split('|').asSequence() }
            .map { it.replace('\u00a0', ' ').replace('\u202f', ' ').replace(Regex("\\s+"), " ").trim()
                .replace(Regex("(?i)^date\\s+[o0]?pened\\b"), "Date Opened") }
            .filter { it.isNotBlank() }.toList()
        val lines = mutableListOf<String>()
        var cursor = 0
        while (cursor < rawLines.size) {
            val line = rawLines[cursor]
            val next = rawLines.getOrNull(cursor + 1)
            if (EMPTY_FIELD.matches(line) && next != null && !FIELD_PREFIX.containsMatchIn(next)) {
                lines.add("${line.trimEnd(':', '#', '-', ' ')}: $next")
                cursor += 2
            } else {
                lines.add(line)
                cursor += 1
            }
        }
        val candidateIndexes = lines.indices.filter { index ->
            val line = lines[index]
            ACCOUNT_HEADING.containsMatchIn(line) ||
                (line.length in 3..64 && line.any(Char::isLetter) && !line.contains(':') &&
                    line.uppercase(Locale.US) !in IGNORED_HEADINGS && !FIELD_PREFIX.containsMatchIn(line) && !MASKED_IDENTIFIER.matches(line) &&
                    lines.getOrNull(index + 1)?.let { ACCOUNT_NUMBER.containsMatchIn(it) } == true)
        }

        return candidateIndexes.mapIndexedNotNull { position, index ->
            val end = candidateIndexes.getOrNull(position + 1) ?: lines.size
            val window = lines.subList(index, end).take(40)
            val headingMatch = ACCOUNT_HEADING.find(window.first())
            val creditor = sanitizeCreditor(
                headingMatch?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank)
                    ?: window.first(),
            )
            if (creditor.length < 3 || creditor.uppercase(Locale.US) in IGNORED_HEADINGS) return@mapIndexedNotNull null

            val accountLine = window.firstOrNull { ACCOUNT_NUMBER.containsMatchIn(it) }.orEmpty()
            val suffix = Regex("(?i)^(?:account(?:\\s*(?:number|no\\.?|#))?|acct)\\s*[:#-]+\\s*([*Xx\\s-]*[A-Z0-9-]{3,})")
                .find(accountLine)?.groupValues?.getOrNull(1).orEmpty().filter(Char::isLetterOrDigit).takeLast(4)
            val status = extractValue(window, "account status") ?: extractValue(window, "status") ?: window.firstOrNull { NEGATIVE_STATUS.containsMatchIn(it) }
                ?.let { NEGATIVE_STATUS.find(it)?.value } ?: "Unknown"
            val balance = window.firstNotNullOfOrNull { line ->
                BALANCE.find(line)?.groupValues?.getOrNull(1)?.let(::parseMoney)
            }
            val paymentStatus = extractValue(window, "payment status") ?: extractValue(window, "pay status") ?: ""
            val opened = normalizeDate(extractValue(window, "date opened") ?: extractValue(window, "opened") ?: "")
            val accountType = extractValue(window, "account type") ?: extractValue(window, "type") ?: ""
            val responsibility = extractValue(window, "responsibility") ?: extractValue(window, "ownership") ?: ""
            val dateReported = normalizeDate(extractValue(window, "date reported") ?: extractValue(window, "last reported") ?: "")
            val originalCreditor = extractValue(window, "original creditor") ?: ""
            val pastDue = extractMoneyValue(window, "past due")
            val creditLimit = extractMoneyValue(window, "credit limit") ?: extractMoneyValue(window, "limit")
            val highBalance = extractMoneyValue(window, "high balance") ?: extractMoneyValue(window, "highest balance")
            val remarks = extractValue(window, "remarks") ?: extractValue(window, "comments") ?: ""

            CreditAccount(
                creditor = creditor,
                accountSuffix = suffix,
                bureau = bureau,
                status = status.take(80),
                balance = balance,
                paymentStatus = paymentStatus.take(80),
                openedDate = opened.take(40),
                accountType = accountType.take(80),
                responsibility = responsibility.take(80),
                dateReported = dateReported.take(40),
                originalCreditor = originalCreditor.take(80),
                pastDue = pastDue,
                creditLimit = creditLimit,
                highBalance = highBalance,
                remarks = remarks.take(200),
            )
        }.take(150)
    }

    private fun extractValue(lines: List<String>, label: String): String? {
        val flexibleLabel = label.split(' ').joinToString("\\s*") { Regex.escape(it) }
        val pattern = Regex("(?i)^${flexibleLabel}\\s*[:#-]?\\s*(.+)$")
        return lines.firstNotNullOfOrNull { pattern.find(it)?.groupValues?.getOrNull(1)?.trim() }
    }

    private fun extractMoneyValue(lines: List<String>, label: String): Int? {
        val flexibleLabel = label.split(' ').joinToString("\\s*") { Regex.escape(it) }
        val pattern = Regex("(?i)^${flexibleLabel}\\s*[:#-]?\\s*\\$?([0-9][0-9,. ]*)")
        return lines.firstNotNullOfOrNull { line ->
            pattern.find(line)?.groupValues?.getOrNull(1)?.let(::parseMoney)
        }
    }

    private fun parseMoney(raw: String): Int? = runCatching {
        val number = raw.replace("$", "").replace(" ", "").trim()
        val whole = when {
            Regex("[0-9]{1,3}(?:\\.[0-9]{3})+").matches(number) -> number.replace(".", "")
            Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]{1,2})?").matches(number) -> number.substringBefore('.').replace(",", "")
            else -> return@runCatching null
        }
        whole.toLongOrNull()
            ?.takeIf { it in 0..Int.MAX_VALUE.toLong() }?.toInt()
    }.getOrNull()

    // Rejoin only digits and separators already present; never infer a date digit.
    private fun normalizeDate(value: String) = value
        .replace(Regex("(?<=\\d)\\s+(?=\\d)"), "")
        .replace(Regex("\\s*/\\s*"), "/")

    private fun sanitizeCreditor(value: String): String = value
        .replace(ACCOUNT_HEADING, "")
        .replace(Regex("[^A-Za-z0-9 &.'/-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', ':', '-')
        .take(64)

    private fun normalizeName(value: String) = value.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

    companion object {
        private val ACCOUNT_HEADING = Regex("(?i)^(?:creditor|account name|company|subscriber)\\s*[:#-]?\\s*(.*)$")
        private val ACCOUNT_NUMBER = Regex("(?i)^(?:account(?:\\s*(?:number|no\\.?|#))?|acct)\\s*[:#-]")
        private val BALANCE = Regex("(?i)^(?:current\\s*)?balance\\s*[:#-]?\\s*\\$?([0-9][0-9,. ]*)")
        private const val LABELS = "account number|account no[.]?|account #|acct|account status|payment status|pay status|date opened|date reported|last reported|original creditor|account type|credit limit|high balance|highest balance|current balance|responsibility|ownership|past due|balance|status|opened|limit|type|remarks|comments|creditor|account name|company|subscriber"
        private val LABEL_PATTERN = LABELS.replace(" ", "\\s*")
        private val EMPTY_FIELD = Regex("(?i)^(?:${LABEL_PATTERN})\\s*[:#-]?$")
        private val FIELD_PREFIX = Regex("(?i)^(?:${LABEL_PATTERN})\\s*[:#-]")
        private val MASKED_IDENTIFIER = Regex("[*Xx -]*[0-9]{3,}")
        private val NEGATIVE_STATUS = Regex("(?i)charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid")
        private val IGNORED_HEADINGS = setOf(
            "ACCOUNT INFORMATION", "ACCOUNT HISTORY", "CREDIT REPORT", "PERSONAL INFORMATION", "SUMMARY",
        )
    }
}
