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
        var extractionMethod = "Embedded PDF text"
        var text = runCatching { extractEmbeddedText(uri) }.getOrDefault("")
        if (text.filterNot(Char::isWhitespace).length < 200) {
            extractionMethod = "On-device OCR"
            text = extractWithOcr(uri)
        }

        require(text.filterNot(Char::isWhitespace).length >= 80) {
            "The PDF did not contain enough readable text. Try an original downloaded report instead of a photo-only scan."
        }

        val bureau = detectBureau(fileName, text)
        val accounts = parseAccounts(text, bureau)
        val score = parseScore(text)
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
            PDDocument.load(stream).use { document -> PDFTextStripper().getText(document) }
        }.orEmpty()
    }

    private suspend fun extractWithOcr(uri: Uri): String {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Could not open the selected PDF")
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            PdfRenderer(descriptor).use { renderer ->
                buildString {
                    val pageLimit = minOf(renderer.pageCount, 30)
                    for (index in 0 until pageLimit) {
                        renderer.openPage(index).use { page ->
                            val targetWidth = 1440
                            val targetHeight = (targetWidth * page.height.toFloat() / page.width).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                            try {
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                appendLine(recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text)
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
            Regex("(?i)(?:credit|fico|vantage)[®™ ]*(?:score)?[^0-9]{0,25}([3-8][0-9]{2})"),
            Regex("(?i)(?:your|current) score[^0-9]{0,15}([3-8][0-9]{2})"),
        )
        return scorePatterns.firstNotNullOfOrNull { pattern ->
            pattern.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 300..850 }
        }
    }

    internal fun parseAccounts(text: String, bureau: Bureau): List<CreditAccount> {
        val lines = text.lineSequence()
            .map { it.replace(Regex("\\s+"), " ").trim() }
            .filter { it.isNotBlank() }
            .toList()
        val candidateIndexes = lines.indices.filter { index ->
            val line = lines[index]
            ACCOUNT_HEADING.containsMatchIn(line) ||
                (line.length in 3..48 && line == line.uppercase(Locale.US) &&
                    lines.drop(index + 1).take(5).any { ACCOUNT_NUMBER.containsMatchIn(it) })
        }

        return candidateIndexes.mapNotNull { index ->
            val window = lines.drop(index).take(24)
            val headingMatch = ACCOUNT_HEADING.find(window.first())
            val creditor = sanitizeCreditor(
                headingMatch?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank)
                    ?: window.first(),
            )
            if (creditor.length < 3 || creditor in IGNORED_HEADINGS) return@mapNotNull null

            val accountLine = window.firstOrNull { ACCOUNT_NUMBER.containsMatchIn(it) }.orEmpty()
            val suffix = Regex("(?i)(?:account(?: number| #)?|acct)[^A-Z0-9]*([*Xx-]*[A-Z0-9-]{3,})")
                .find(accountLine)?.groupValues?.getOrNull(1).orEmpty().filter(Char::isLetterOrDigit).takeLast(4)
            val status = extractValue(window, "status") ?: window.firstOrNull { NEGATIVE_STATUS.containsMatchIn(it) }
                ?.let { NEGATIVE_STATUS.find(it)?.value } ?: "Unknown"
            val balance = window.firstNotNullOfOrNull { line ->
                BALANCE.find(line)?.groupValues?.getOrNull(1)?.let(::parseMoney)
            }
            val paymentStatus = extractValue(window, "payment status") ?: extractValue(window, "pay status") ?: ""
            val opened = extractValue(window, "date opened") ?: extractValue(window, "opened") ?: ""

            CreditAccount(
                creditor = creditor,
                accountSuffix = suffix,
                bureau = bureau,
                status = status.take(80),
                balance = balance,
                paymentStatus = paymentStatus.take(80),
                openedDate = opened.take(40),
            )
        }.distinctBy { Triple(normalizeName(it.creditor), it.accountSuffix, it.bureau) }.take(150)
    }

    private fun extractValue(lines: List<String>, label: String): String? {
        val pattern = Regex("(?i)^${Regex.escape(label)}\\s*[:#-]?\\s*(.+)$")
        return lines.firstNotNullOfOrNull { pattern.find(it)?.groupValues?.getOrNull(1)?.trim() }
    }

    private fun parseMoney(raw: String): Int? = runCatching {
        NumberFormat.getNumberInstance(Locale.US).parse(raw.replace("$", ""))?.toInt()
    }.getOrNull()

    private fun sanitizeCreditor(value: String): String = value
        .replace(ACCOUNT_HEADING, "")
        .replace(Regex("[^A-Za-z0-9 &.'/-]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim(' ', ':', '-')
        .take(64)

    private fun normalizeName(value: String) = value.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")

    companion object {
        private val ACCOUNT_HEADING = Regex("(?i)^(?:creditor|account name|company|subscriber)\\s*[:#-]?\\s*(.*)$")
        private val ACCOUNT_NUMBER = Regex("(?i)(?:account(?: number| #)?|acct)\\s*[:#-]")
        private val BALANCE = Regex("(?i)(?:current )?balance\\s*[:#-]?\\s*\\$?([0-9][0-9,]*)")
        private val NEGATIVE_STATUS = Regex("(?i)charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid")
        private val IGNORED_HEADINGS = setOf(
            "ACCOUNT INFORMATION", "ACCOUNT HISTORY", "CREDIT REPORT", "PERSONAL INFORMATION", "SUMMARY",
        )
    }
}
