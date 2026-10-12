package com.creditrepairai.v2.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.creditrepairai.v2.legal.PolicyAcceptance

class SecureService(endpoint: String, private val session: SessionTokenProvider, private val policy: () -> PolicyAcceptance? = { null }) {
    private val origin = CreditAgentGateway.resolveEndpoint(endpoint).removeSuffix("/credit-agent")
    suspend fun post(path: String, expectedSubject: String, body: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
        require(origin.startsWith("https://") && path in setOf("/billing/status", "/billing/verify", "/account/delete", "/policy/accept")) { "Secure service configuration is missing." }
        if (path == "/billing/verify") syncPolicy(expectedSubject, policy()?.takeIf { it.isCurrent } ?: error("Read and accept the current terms first."))
        val token = session.bearerToken(expectedSubject) ?: error("Sign in again to continue.")
        val connection = (URL("$origin$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; doOutput = true; connectTimeout = 15000; readTimeout = 45000
            setRequestProperty("Content-Type", "application/json"); setRequestProperty("Authorization", "Bearer $token")
            instanceFollowRedirects = false
        }
        try {
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val success = connection.responseCode in 200..299
            val response = (if (success) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readBoundedResponse() }.orEmpty()
            if (!success) error("Secure service could not complete this action (HTTP ${connection.responseCode}).")
            JSONObject(response)
        } finally { connection.disconnect() }
    }
    suspend fun syncPolicy(expectedSubject: String, acceptance: PolicyAcceptance) {
        require(acceptance.isCurrent)
        val result = post("/policy/accept", expectedSubject, JSONObject().put("version", acceptance.version)
            .put("digest", acceptance.digest).put("termsAccepted", true).put("aiSharing", acceptance.aiSharing).put("choiceAt", acceptance.choiceAt))
        check(result.optBoolean("aiSharing") == acceptance.aiSharing) { "A newer AI-sharing choice exists. Review the current choice in Settings." }
    }
}
