package com.creditrepairai.v2.analysis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/** Generated fictional PDFs exercise the real Android importer, PDFBox and bundled ML Kit. */
class SyntheticParserBatchTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val directory = File(context.cacheDir, "fictional-parser-fixtures/synthetic-batch").apply { mkdirs() }
    private val parser = CreditReportParser(context)
    private val layouts = listOf("upper", "creditor", "company", "subscriber", "title-case", "split-values", "whitespace", "account-status", "no-score", "missing-balance", "inline-fields", "aliases")
    private val scans = listOf("upper", "creditor", "no-score", "split-values")
    private data class Case(val id: String, val bureau: Bureau, val layout: String = "upper", val scanned: Boolean = false, val encrypted: Boolean = false, val multipage: Boolean = false, val reject: String? = null)
    private val expectedNames = listOf("NORTHSTAR BANK", "HARBOR COLLECTIONS", "PINE AUTO")
    // Test-only inspection of this generated scan when field accuracy fails.
    // Production importer does not log or export extracted consumer text.
    private suspend fun diagnosticOcr(uri: android.net.Uri): String = suspendCoroutine { continuation ->
        try {
            val method = CreditReportParser::class.java.getDeclaredMethod("extractWithOcr", android.net.Uri::class.java, Continuation::class.java)
            method.isAccessible = true
            val result = method.invoke(parser, uri, continuation)
            if (result !== COROUTINE_SUSPENDED) continuation.resume(result as String)
        } catch(e: Exception) { continuation.resumeWithException(e) }
    }
    private fun expected(case: Case) = expectedNames.mapIndexed { i, name ->
        linkedMapOf<String, Any?>("creditor" to name, "suffix" to listOf("1234", "5678", "9012")[i],
            "balance" to if (case.layout == "missing-balance" && i == 1) null else listOf(1280, 680, 0)[i],
            "status" to listOf("Open", "Collection", "Closed")[i], "paymentStatus" to listOf("Current", "Past due", "Paid")[i],
            "openedDate" to "01/2020", "dateReported" to "09/2026", "creditLimit" to 5000, "accountType" to "Revolving", "responsibility" to "Individual")
    }
    private fun actual(account: CreditAccount) = linkedMapOf<String, Any?>("creditor" to account.creditor.uppercase(), "suffix" to account.accountSuffix,
        "balance" to account.balance, "status" to account.status, "paymentStatus" to account.paymentStatus,
        "openedDate" to account.openedDate, "dateReported" to account.dateReported, "creditLimit" to account.creditLimit,
        "accountType" to account.accountType, "responsibility" to account.responsibility)
    private fun header(case: Case) = mutableListOf(case.bureau.displayName.uppercase() + " CREDIT REPORT", "SYNTHETIC TEST DOCUMENT - NOT A CONSUMER REPORT",
        "Invented records for offline extraction validation only.", "No actual personal identifiers or borrower information are present.",
        "The balances and account identifiers below are generated test values.").apply { if (case.layout != "no-score") add("Your Score: 640") }
    private fun accountLines(case: Case, index: Int): List<String> {
        val e = expected(case)[index]
        val heading = when(case.layout) {
            "creditor" -> "Creditor: ${e["creditor"]}"
            "company" -> "Company: ${e["creditor"]}"
            "subscriber" -> "Subscriber: ${e["creditor"]}"
            "title-case" -> e["creditor"].toString().lowercase().split(" ").joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            else -> e["creditor"].toString()
        }
        val fields = mutableListOf((if(case.layout == "aliases") "Account #" else "Account Number") to "XXXX${e["suffix"]}",
            (if(case.layout == "account-status") "Account Status" else "Status") to e["status"].toString(),
            "Payment Status" to e["paymentStatus"].toString(), "Date Opened" to "01/2020", "Date Reported" to "09/2026",
            "Credit Limit" to "$5,000", "Account Type" to "Revolving", "Responsibility" to "Individual")
        if (e["balance"] != null) fields.add(1, (if(case.layout == "aliases") "Current Balance" else "Balance") to "$${e["balance"]}")
        val formatted = when(case.layout) {
            "split-values" -> fields.flatMap { listOf(it.first + ":", it.second) }
            "whitespace" -> fields.map { it.first + "\u00a0 :\t  " + it.second }
            "inline-fields" -> fields.chunked(2).map { pair -> pair.joinToString(" | ") { it.first + ": " + it.second } }
            else -> fields.map { it.first + ": " + it.second }
        }
        return listOf(heading) + formatted + listOf("")
    }
    private fun pdf(case: Case): File {
        val file = File(directory, case.id + ".pdf")
        if(case.reject == "invalid") { file.writeText("This is not a PDF"); return file }
        val pages = when(case.reject) { "too-many-text-pages" -> 201; "too-many-scan-pages" -> 31; else -> if(case.multipage) 3 else 1 }
        val document = PdfDocument()
        repeat(pages) { p ->
            val height = if(case.reject == "tall-scan") 3000 else 1900
            val page = document.startPage(PdfDocument.PageInfo.Builder(1000, height, p + 1).create())
            val lines = when(case.reject) { "blank" -> emptyList(); "too-many-text-pages" -> header(case); else -> header(case) + if(case.multipage) accountLines(case, p) else (0..2).flatMap { accountLines(case, it) } }
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 21f; typeface = android.graphics.Typeface.MONOSPACE }
            if(case.scanned || case.reject in listOf("too-many-scan-pages", "tall-scan")) {
                val bitmap = Bitmap.createBitmap(1000, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap); canvas.drawColor(Color.WHITE)
                lines.forEachIndexed { i, text -> canvas.drawText(text, 24f, 40f + i * 24f, paint) }
                page.canvas.drawBitmap(bitmap, 0f, 0f, null); document.finishPage(page); bitmap.recycle()
            } else {
                page.canvas.drawColor(Color.WHITE)
                lines.forEachIndexed { i, text -> page.canvas.drawText(text, 24f, 40f + i * 24f, paint) }
                document.finishPage(page)
            }
        }
        file.outputStream().use(document::writeTo); document.close()
        if(case.encrypted || case.reject == "password-required") {
            val temporary = File(directory, case.id + "-encrypted.pdf")
            PDDocument.load(file).use { doc ->
                val policy = StandardProtectionPolicy("fictional-owner-password", if(case.reject == "password-required") "fictional-user-password" else "", AccessPermission())
                policy.setEncryptionKeyLength(256); policy.setPreferAES(true); doc.protect(policy); doc.save(temporary)
            }
            temporary.copyTo(file, overwrite = true); temporary.delete()
        }
        return file
    }
    @Test fun measuredSyntheticPdfBatch() = runBlocking {
        val cases = mutableListOf<Case>()
        listOf(Bureau.EQUIFAX, Bureau.EXPERIAN, Bureau.TRANSUNION).forEach { bureau ->
            layouts.forEach { cases.add(Case("${bureau.name}-embedded-$it", bureau, it)) }
            scans.forEach { cases.add(Case("${bureau.name}-scan-$it", bureau, it, scanned = true)) }
            cases.add(Case("${bureau.name}-encrypted", bureau, encrypted = true))
            cases.add(Case("${bureau.name}-multipage", bureau, multipage = true))
        }
        listOf("invalid", "blank", "password-required", "too-many-text-pages", "too-many-scan-pages", "tall-scan").forEach { cases.add(Case("guard-$it", Bureau.EQUIFAX, reject = it)) }
        val results = JSONArray()
        val started = System.currentTimeMillis()
        for(case in cases) {
            val start = System.currentTimeMillis()
            val row = JSONObject().put("id", case.id).put("bureau", case.bureau.name).put("layout", case.layout)
                .put("scanned", case.scanned).put("encrypted", case.encrypted).put("multipage", case.multipage).put("expectedRejection", case.reject ?: JSONObject.NULL)
            // A fixture/provider failure is a harness failure, never a safe parser rejection.
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.test-fixtures", pdf(case))
            try {
                val parsed = parser.parse(uri, "synthetic.pdf") // bureau must come from content, not filename
                val want = expected(case)
                val got = parsed.accounts.map(::actual)
                val fieldTotal = want.sumOf { it.size }
                val matched = want.mapIndexed { i, target -> target.count { (key, value) -> got.getOrNull(i)?.get(key) == value && got.getOrNull(i)?.containsKey(key) == true } }.sum()
                val scoreOk = parsed.report.score == if(case.layout == "no-score") null else 640
                val bureauOk = parsed.report.bureau == case.bureau
                val exact = case.reject == null && got == want && scoreOk && bureauOk
                row.put("accepted", true).put("exactPass", exact).put("bureauCorrect", bureauOk).put("scoreCorrect", scoreOk)
                    .put("accountCount", got.size).put("fieldMatches", matched).put("fieldTotal", fieldTotal)
                    .put("extractionMethod", parsed.report.extractionMethod).put("expectedAccounts", JSONArray(want.map { JSONObject(it) })).put("actualAccounts", JSONArray(got.map { JSONObject(it) }))
                if (!exact && case.scanned) row.put("syntheticOcrDiagnostic", runCatching { diagnosticOcr(uri) }.getOrDefault("Diagnostic extraction unavailable"))
            } catch(e: Exception) {
                row.put("accepted", false).put("exactPass", case.reject != null).put("error", e.javaClass.simpleName + ": " + (e.message ?: "").take(250))
            }
            row.put("elapsedMs", System.currentTimeMillis() - start); results.put(row)
        }
        val all = (0 until results.length()).map { results.getJSONObject(it) }
        val supported = all.filter { it.isNull("expectedRejection") }
        val guards = all.filterNot { it.isNull("expectedRejection") }
        val phase = InstrumentationRegistry.getArguments().getString("parserPhase") ?: "current"
        val summary = JSONObject().put("phase", phase).put("fixtureKind", "synthetic-only; not bureau-provided reports")
            .put("androidApi", android.os.Build.VERSION.SDK_INT).put("device", android.os.Build.MODEL)
            .put("total", all.size).put("supportedCases", supported.size).put("supportedAccepted", supported.count { it.getBoolean("accepted") })
            .put("supportedExactPass", supported.count { it.getBoolean("exactPass") }).put("guards", guards.size).put("guardsSafelyRejected", guards.count { !it.getBoolean("accepted") })
            .put("fieldMatches", supported.sumOf { it.optInt("fieldMatches") }).put("fieldTotal", supported.size * 30)
            .put("elapsedMs", System.currentTimeMillis() - started).put("cases", results)
        File(context.filesDir, "parser-validation-$phase.json").writeText(summary.toString(2))
        // Gradle uninstalls the app after instrumentation. Preserve small numbered log
        // chunks so the runner can reconstruct and validate the complete synthetic report.
        summary.toString().chunked(1500).forEachIndexed { index, chunk ->
            android.util.Log.i("ParserBatchChunk", "%05d:%s".format(index, chunk))
        }
        directory.deleteRecursively()
        if(InstrumentationRegistry.getArguments().getString("parserEnforce") != "false") assertEquals("Inspect parser-validation-$phase.json; do not weaken independent fixture expectations", all.size, all.count { it.getBoolean("exactPass") })
    }
}
