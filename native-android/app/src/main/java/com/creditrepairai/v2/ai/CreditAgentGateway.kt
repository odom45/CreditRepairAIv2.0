package com.creditrepairai.v2.ai

import com.creditrepairai.v2.model.AppState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

fun interface SessionTokenProvider {
    suspend fun bearerToken(): String?
}

class CreditAgentGateway(
    private val endpoint: String,
    private val tokenProvider: SessionTokenProvider,
) {
    val isConfigured: Boolean get() = endpoint.startsWith("https://")

    suspend fun answer(question: String, state: AppState): String = withContext(Dispatchers.IO) {
        check(isConfigured) { "The secure AI service has not been configured for this build." }
        val token = tokenProvider.bearerToken()?.takeIf(String::isNotBlank)
            ?: error("Secure AI sign-in is not connected. No report data was sent.")
        val safeQuestion = SensitiveDataRedactor.redact(question)
        check(!SensitiveDataRedactor.containsHighRiskIdentifier(safeQuestion)) {
            "Remove sensitive identifiers such as an SSN, birth date, email, or phone number and try again."
        }

        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 45_000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val body = JSONObject()
                .put("clientRequestId", UUID.randomUUID().toString())
                .put("question", safeQuestion)
                .put("caseSnapshot", state.toSafeAgentJson())
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val responseText = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText().take(64_000) }.orEmpty()
            if (status !in 200..299) {
                val publicMessage = runCatching { JSONObject(responseText).optString("error") }.getOrNull()
                    ?.takeIf(String::isNotBlank)
                error(publicMessage ?: "The secure AI service returned HTTP $status.")
            }
            JSONObject(responseText).optString("answer").takeIf(String::isNotBlank)
                ?: error("The secure AI service returned an empty answer.")
        } finally {
            connection.disconnect()
        }
    }

    private fun AppState.toSafeAgentJson(): JSONObject = JSONObject().apply {
        put("jurisdictionCode", jurisdictionCode)
        put("reports", JSONArray().apply {
            reports.forEach { report ->
                put(JSONObject()
                    .put("bureau", report.bureau.name.lowercase())
                    .put("score", report.score ?: JSONObject.NULL)
                    .put("accountCount", report.accountCount)
                    .put("importedAt", report.importedAt))
            }
        })
        put("accounts", JSONArray().apply {
            accounts.take(200).forEach { account ->
                put(JSONObject()
                    .put("creditor", SensitiveDataRedactor.redact(account.creditor))
                    .put("accountSuffix", account.accountSuffix.takeLast(4))
                    .put("bureau", account.bureau.name.lowercase())
                    .put("status", SensitiveDataRedactor.redact(account.status))
                    .put("balance", account.balance ?: JSONObject.NULL)
                    .put("paymentStatus", SensitiveDataRedactor.redact(account.paymentStatus))
                    .put("openedDate", account.openedDate)
                    .put("accountType", account.accountType)
                    .put("responsibility", account.responsibility)
                    .put("dateReported", account.dateReported)
                    .put("originalCreditor", SensitiveDataRedactor.redact(account.originalCreditor))
                    .put("pastDue", account.pastDue ?: JSONObject.NULL)
                    .put("creditLimit", account.creditLimit ?: JSONObject.NULL)
                    .put("highBalance", account.highBalance ?: JSONObject.NULL)
                    .put("remarks", SensitiveDataRedactor.redact(account.remarks)))
            }
        })
        put("findings", JSONArray().apply {
            findings.take(100).forEach { finding ->
                put(JSONObject()
                    .put("id", finding.id)
                    .put("type", finding.type.name)
                    .put("title", finding.title)
                    .put("detail", SensitiveDataRedactor.redact(finding.detail))
                    .put("severity", finding.severity.name)
                    .put("creditor", SensitiveDataRedactor.redact(finding.creditor))
                    .put("bureaus", JSONArray(finding.bureaus.map { it.name.lowercase() }))
                    .put("legalBasis", JSONArray(finding.legalBasis)))
            }
        })
        put("disputes", JSONArray().apply {
            disputes.take(100).forEach { dispute ->
                put(JSONObject()
                    .put("id", dispute.id)
                    .put("findingId", dispute.findingId)
                    .put("creditor", SensitiveDataRedactor.redact(dispute.creditor))
                    .put("bureaus", JSONArray(dispute.bureaus.map { it.name.lowercase() }))
                    .put("reason", SensitiveDataRedactor.redact(dispute.reason))
                    .put("status", dispute.status.name)
                    .put("createdAt", dispute.createdAt)
                    .put("sentAt", dispute.sentAt ?: JSONObject.NULL)
                    .put("craResponseDueAt", dispute.craResponseDueAt ?: JSONObject.NULL))
            }
        })
    }
}
