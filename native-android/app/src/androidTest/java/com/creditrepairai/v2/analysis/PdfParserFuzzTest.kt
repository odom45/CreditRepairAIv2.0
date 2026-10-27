package com.creditrepairai.v2.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import com.creditrepairai.v2.model.Bureau
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.random.Random

/** Fixed seeds + bounded generated PDFs. Errors, assertion failures and timeouts fail CI. */
class PdfParserFuzzTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory get() = File(context.cacheDir, "fictional-parser-fixtures/fuzz").apply { mkdirs() }
    @After fun cleanup() { directory.deleteRecursively() }
    private fun uri(file: File) = FileProvider.getUriForFile(context, "${context.packageName}.test-fixtures", file)
    private fun pdf(file: File, scan: Boolean, seed: Int, mutateText: Boolean = false) {
        val random = Random(seed)
        val sourceLines = listOf("EQUIFAX CREDIT REPORT", "SYNTHETIC ONLY - NO CONSUMER DATA",
            "Fictional generated parser property fixture; every identifier is invented.",
            "Offline testing of bounded import and safe resource handling.", "Your Score: 640",
            "Creditor: Fictional Bank", "Account Number: XXXX1234", "Status: Open", "Balance: 1280",
            "Payment Status: Current", "Date Opened: 01/2020", "Date Reported: 09/2026")
        val lines = if (!mutateText) sourceLines else sourceLines.take(4) + listOf("Repeated synthetic description ensures embedded extraction remains above the OCR threshold for these mutation cases.") + sourceLines.drop(4).map { line ->
            when (random.nextInt(5)) {
                0 -> line.take(random.nextInt(line.length + 1))
                1 -> line.replace(" ", "\u00a0")
                2 -> line.replace("1280", "9999999999999999999999999999999999")
                3 -> line + " | " + "Synthetic annotation ".repeat(random.nextInt(5))
                else -> line
            }
        }.shuffled(random)
        PdfDocument().use { document ->
            repeat(1 + random.nextInt(3)) { pageIndex ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(1000, 1400, pageIndex + 1).create())
                val bitmap = if (scan) Bitmap.createBitmap(1000, 1400, Bitmap.Config.ARGB_8888) else null
                try {
                    val canvas = if (bitmap == null) page.canvas else Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 24f }
                    lines.forEachIndexed { index, text -> canvas.drawText(text, 24f, 45f + index * 40f, paint) }
                    if (bitmap != null) page.canvas.drawBitmap(bitmap, 0f, 0f, null)
                    document.finishPage(page)
                } finally { bitmap?.recycle() }
            }
            file.outputStream().use(document::writeTo)
        }
    }

    @Test(timeout = 180_000) fun mutatedAndTruncatedPdfBytesAcceptOrRejectWithoutCrashing() = runBlocking {
        val parser = CreditReportParser(context)
        val original = File(directory, "original.pdf")
        pdf(original, false, 42)
        val bytes = original.readBytes()
        val random = Random(20261026)
        repeat(48) { iteration ->
            val file = File(directory, "mutation-$iteration.pdf")
            val mutated = when (iteration % 4) {
                0 -> bytes.copyOf(random.nextInt(bytes.size)) // includes missing headers/trailers
                1 -> bytes.copyOf().apply { repeat(1 + random.nextInt(20)) { this[random.nextInt(size)] = random.nextInt(256).toByte() } }
                2 -> ByteArray(random.nextInt(1024)) { random.nextInt(256).toByte() }
                else -> bytes.copyOfRange(random.nextInt(bytes.size), bytes.size)
            }
            file.writeBytes(mutated)
            val started = System.nanoTime()
            try {
                val parsed = parser.parse(uri(file), "synthetic.pdf")
                assertTrue(parsed.extractedCharacters in 80..ParserLimits.MAX_TEXT_CHARS)
                assertTrue(parsed.accounts.size <= 150)
                assertEquals(parsed.accounts.size, parsed.report.accountCount)
            } catch (_: IOException) {
                // Damaged/unreadable PDF container.
            } catch (_: IllegalArgumentException) {
                // Invalid PDF, explicit input/resource bound, or insufficient readable text.
            }
            assertTrue("seed=20261026 mutation=$iteration exceeded 10s", (System.nanoTime() - started) / 1_000_000 < 10_000)
            file.delete()
        }
    }

    @Test(timeout = 180_000) fun mutatedReportTextAlsoRunsThroughTheRealPdfImporter() = runBlocking {
        for (seed in listOf(7, 42, 20261026)) repeat(8) { iteration ->
            val file = File(directory, "text-$seed-$iteration.pdf")
            pdf(file, false, seed + iteration, mutateText = true)
            val parsed = CreditReportParser(context).parse(uri(file), "synthetic.pdf")
            assertEquals("Embedded PDF text", parsed.report.extractionMethod)
            assertTrue(parsed.extractedCharacters in 80..ParserLimits.MAX_TEXT_CHARS)
            assertEquals(parsed.accounts.size, parsed.report.accountCount)
            assertTrue(parsed.accounts.size <= 150)
            assertTrue(parsed.accounts.all { it.accountSuffix.length <= 4 && (it.balance?.let { value -> value >= 0 } != false) })
            file.delete()
        }
    }

    @Test(timeout = 120_000) fun seededScansUseRealBundledOcrAcrossMultiplePages() = runBlocking {
        for (seed in listOf(7, 42, 20261026)) {
            val file = File(directory, "scan-$seed.pdf")
            pdf(file, true, seed)
            val parsed = CreditReportParser(context).parse(uri(file), "synthetic.pdf")
            assertEquals("On-device OCR", parsed.report.extractionMethod)
            assertEquals(Bureau.EQUIFAX, parsed.report.bureau)
            assertTrue(parsed.accounts.isNotEmpty())
            assertTrue(parsed.accounts.all { it.accountSuffix == "1234" && it.balance == 1280 })
            file.delete()
        }
    }

    @Test(timeout = 30_000) fun oversizedPdfIsRejectedBeforeOcrCanBypassTheByteLimit() = runBlocking {
        val file = File(directory, "oversized.pdf")
        file.outputStream().use { output ->
            output.write("%PDF-1.7\n".toByteArray())
            val block = ByteArray(1_000_000)
            repeat(50) { output.write(block) }
        }
        val error = runCatching { CreditReportParser(context).parse(uri(file), "synthetic.pdf") }.exceptionOrNull()
        assertTrue("Must reject the input size without trying OCR: $error", error is ParserLimitException)
    }
}
