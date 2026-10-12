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
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy

/** Fictional PDFs generated on the test device; no network or consumer data. */
class PdfParserInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory get() = File(context.cacheDir, "fictional-parser-fixtures").apply { mkdirs() }
    @After fun cleanup() { directory.deleteRecursively() }
    private fun fixture(scanned: Boolean): android.net.Uri {
        val lines = listOf("EQUIFAX CREDIT REPORT", "Fictional parser fixture for device validation only",
            "This report contains invented information and is not a consumer report.",
            "This sample verifies readable extraction from a generated PDF page.",
            "YOUR SCORE: 640", "NORTHSTAR BANK", "Account Number: XXXX1234", "Balance: $500",
            "Account Status: Open", "Payment Status: Current", "Date Opened: 01/2020",
            "Date Reported: 09/2026", "Account Type: Revolving", "Responsibility: Individual")
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(720, 960, 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 20f }
        if (scanned) {
            val bitmap = Bitmap.createBitmap(720, 960, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
            lines.forEachIndexed { index, line -> canvas.drawText(line, 28f, 50f + index * 42f, paint) }
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            document.finishPage(page)
            val file = File(directory, "scanned-fictional.pdf")
            file.outputStream().use(document::writeTo); document.close(); bitmap.recycle()
            return FileProvider.getUriForFile(context, "${context.packageName}.test-fixtures", file)
        }
        lines.forEachIndexed { index, line -> page.canvas.drawText(line, 28f, 50f + index * 42f, paint) }
        document.finishPage(page)
        val file = File(directory, "embedded-fictional.pdf")
        file.outputStream().use(document::writeTo); document.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.test-fixtures", file)
    }
    @Test fun embeddedPdfParsesOnDevice() = runBlocking {
        val parsed = CreditReportParser(context).parse(fixture(false), "Equifax-fictional.pdf")
        assertEquals(Bureau.EQUIFAX, parsed.report.bureau)
        assertEquals("Embedded PDF text", parsed.report.extractionMethod)
        assertEquals(640, parsed.report.score)
        assertTrue(parsed.accounts.any { it.accountSuffix == "1234" })
    }
    @Test fun imageOnlyPdfUsesBundledOnDeviceOcr() = runBlocking {
        val parsed = CreditReportParser(context).parse(fixture(true), "Equifax-fictional.pdf")
        assertEquals("On-device OCR", parsed.report.extractionMethod)
        assertEquals(Bureau.EQUIFAX, parsed.report.bureau)
        assertTrue(parsed.extractedCharacters >= 80)
    }
    @Test fun encryptedFixtureWithEmptyUserPasswordStillParsesAfterCryptoUpgrade() = runBlocking {
        val parser = CreditReportParser(context)
        val input = fixture(false)
        val encrypted = File(directory, "encrypted-fictional.pdf")
        context.contentResolver.openInputStream(input)!!.use { stream ->
            PDDocument.load(stream).use { pdf ->
                val policy = StandardProtectionPolicy(java.util.UUID.randomUUID().toString(), "", AccessPermission())
                policy.setEncryptionKeyLength(256)
                policy.setPreferAES(true)
                pdf.protect(policy)
                pdf.save(encrypted)
            }
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.test-fixtures", encrypted)
        val parsed = parser.parse(uri, "Equifax-fictional.pdf")
        assertEquals(Bureau.EQUIFAX, parsed.report.bureau)
        assertEquals(640, parsed.report.score)
        assertTrue(parsed.accounts.any { it.accountSuffix == "1234" })
    }
}
