package com.creditrepairai.v2.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.creditrepairai.v2.model.Bureau
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.Random

/** Actual PDFBox/PdfRenderer/ML Kit calls, not a mocked OCR success. Synthetic data only. */
class ParserRobustnessInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory = File(context.cacheDir, "fictional-parser-fixtures/robustness").apply { mkdirs() }
    private val parser = CreditReportParser(context)
    @After fun cleanup() { directory.deleteRecursively() }
    private fun uri(file: File): Uri = FileProvider.getUriForFile(context, "${context.packageName}.test-fixtures", file)

    private fun pdf(bureau: Bureau, layout: String, scanned: Boolean): File {
        val file = File(directory, "${bureau.name}-$layout-$scanned.pdf")
        val document = PdfDocument()
        try {
            val pageCount = if (layout == "multipage") 2 else 1
            repeat(pageCount) { pageIndex ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(1000, 1600, pageIndex + 1).create())
                val lines = mutableListOf(bureau.displayName.uppercase() + " CREDIT REPORT",
                    "SYNTHETIC TEST DOCUMENT - NOT A CONSUMER REPORT",
                    "Invented records for offline parser robustness validation only.",
                    "No actual personal identifiers or borrower information are present.",
                    "The balances and account identifiers below are synthetic test values.")
                if (layout != "missing-score") lines.add("Your Score: 640")
                val fields = listOf("Creditor" to "NORTHSTAR BANK", "Account Number" to "XXXX${1234 + pageIndex}",
                    "Balance" to "$500", "Status" to "Open", "Payment Status" to "Current",
                    "Date Opened" to "01/2020", "Date Reported" to "09/2026", "Credit Limit" to "$5,000",
                    "Account Type" to "Revolving", "Responsibility" to "Individual")
                lines += if (layout == "split-values") fields.flatMap { listOf(it.first + ":", it.second) }
                    else fields.map { it.first + ": " + it.second }
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK; textSize = if (layout == "single") 23f else 21f; typeface = Typeface.MONOSPACE
                }
                fun draw(canvas: Canvas) {
                    canvas.drawColor(Color.WHITE)
                    lines.forEachIndexed { i, text -> canvas.drawText(text, 28f, 44f + i * 32f, paint) }
                }
                if (scanned) {
                    val bitmap = Bitmap.createBitmap(1000, 1600, Bitmap.Config.ARGB_8888)
                    try { draw(Canvas(bitmap)); page.canvas.drawBitmap(bitmap, 0f, 0f, null) }
                    finally { bitmap.recycle() }
                } else draw(page.canvas)
                document.finishPage(page)
            }
            file.outputStream().use(document::writeTo)
        } finally { document.close() }
        return file
    }

    @Test(timeout = 240_000) fun expandedThreeBureauCorpusExercisesRealEmbeddedAndOcrPaths() = runBlocking {
        for (bureau in listOf(Bureau.EQUIFAX, Bureau.EXPERIAN, Bureau.TRANSUNION)) {
            for (layout in listOf("single", "multipage", "split-values", "missing-score")) {
                for (scanned in listOf(false, true)) {
                    val file = pdf(bureau, layout, scanned)
                    try {
                        val parsed = parser.parse(uri(file), "synthetic.pdf")
                        val label = "${bureau.name} $layout scanned=$scanned"
                        assertEquals(label, bureau, parsed.report.bureau)
                        assertEquals(label, if (scanned) "On-device OCR" else "Embedded PDF text", parsed.report.extractionMethod)
                        assertEquals(label, if (layout == "missing-score") null else 640, parsed.report.score)
                        assertEquals(label, if (layout == "multipage") 2 else 1, parsed.accounts.size)
                        assertEquals(parsed.accounts.size, parsed.report.accountCount)
                        parsed.accounts.forEachIndexed { index, account ->
                            assertEquals(label, "NORTHSTAR BANK", account.creditor)
                            assertEquals(label, (1234 + index).toString(), account.accountSuffix)
                            assertEquals(label, 500, account.balance)
                            assertEquals(label, 5000, account.creditLimit)
                            assertEquals(label, "Open", account.status)
                            assertEquals(label, "Current", account.paymentStatus)
                        }
                        assertTrue(parsed.extractedCharacters <= ParserLimits.TEXT_CHARACTERS)
                    } finally { file.delete() }
                }
            }
        }
    }

    @Test(timeout = 120_000) fun seededMalformedAndTruncatedPdfMutationsNeverEscapeAsUnexpectedFailures() = runBlocking {
        val seed = 0x504446L
        val random = Random(seed)
        val original = pdf(Bureau.EXPERIAN, "single", false).readBytes()
        repeat(48) { iteration ->
            val bytes = when (iteration % 4) {
                0 -> original.copyOf(random.nextInt(original.size))
                1 -> original.copyOf().apply { repeat(12) { this[random.nextInt(size)] = random.nextInt(256).toByte() } }
                2 -> ByteArray(random.nextInt(2048)).also { random.nextBytes(it) }
                else -> "%PDF-1.7\n1 0 obj\n<< /Length 999999999 >>\nstream\ntruncated".toByteArray()
            }
            val file = File(directory, "mutation-$iteration.pdf").apply { writeBytes(bytes) }
            try {
                val result = parser.parse(uri(file), "synthetic.pdf")
                assertTrue("seed=$seed case=$iteration", result.accounts.size <= ParserLimits.ACCOUNTS)
                assertTrue(result.extractedCharacters <= ParserLimits.TEXT_CHARACTERS)
                assertEquals(result.accounts.size, result.report.accountCount)
            } catch (_: IOException) {
                // Unsupported PDF. Errors, assertions, cancellation and unexpected exceptions fail the test.
            } catch (_: IllegalArgumentException) {
                // Invalid PDF or explicit importer resource/empty-content rejection.
            } finally { file.delete() }
        }
    }

    @Test(timeout = 30_000) fun blankAndEmptyPdfsRejectAndPrivateSnapshotsAreAlwaysDeleted() = runBlocking {
        val empty = File(directory, "empty.pdf").apply { writeBytes(byteArrayOf()) }
        val blank = File(directory, "blank.pdf")
        PdfDocument().use { document ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(720, 960, 1).create())
            document.finishPage(page)
            blank.outputStream().use(document::writeTo)
        }
        for (file in listOf(empty, blank)) {
            var rejected = false
            try { parser.parse(uri(file), "synthetic.pdf") }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue(rejected)
        }
        assertTrue(context.cacheDir.listFiles().orEmpty().none { it.name.startsWith("credit-report-") && it.name.endsWith(".pdf") })
    }
}
