package com.creditrepairai.v2.data

import android.content.Context
import com.creditrepairai.v2.model.AppState
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.ChatMessage
import com.creditrepairai.v2.model.CreditAccount
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.Dispute
import com.creditrepairai.v2.model.DisputeStatus
import com.creditrepairai.v2.model.Finding
import com.creditrepairai.v2.model.FindingType
import com.creditrepairai.v2.model.ScoreSnapshot
import com.creditrepairai.v2.model.Severity
import org.json.JSONArray
import org.json.JSONObject

class AppRepository(context: Context) {
    private val preferences = context.getSharedPreferences("credit_repair_private", Context.MODE_PRIVATE)

    fun load(): AppState {
        val raw = preferences.getString(KEY_STATE, null) ?: return AppState()
        return runCatching { decode(JSONObject(raw)) }.getOrElse { AppState() }
    }

    fun save(state: AppState) {
        preferences.edit().putString(KEY_STATE, encode(state).toString()).apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private fun encode(state: AppState) = JSONObject().apply {
        put("reports", JSONArray().apply { state.reports.forEach { put(it.toJson()) } })
        put("accounts", JSONArray().apply { state.accounts.forEach { put(it.toJson()) } })
        put("findings", JSONArray().apply { state.findings.forEach { put(it.toJson()) } })
        put("disputes", JSONArray().apply { state.disputes.forEach { put(it.toJson()) } })
        put("scores", JSONArray().apply { state.scores.forEach { put(it.toJson()) } })
        put("frozenBureaus", state.frozenBureaus.toList().stringJsonArray())
        put("chat", JSONArray().apply { state.chat.takeLast(40).forEach { put(it.toJson()) } })
    }

    private fun decode(json: JSONObject) = AppState(
        reports = json.array("reports").objects().map { it.toReport() },
        accounts = json.array("accounts").objects().map { it.toAccount() },
        findings = json.array("findings").objects().map { it.toFinding() },
        disputes = json.array("disputes").objects().map { it.toDispute() },
        scores = json.array("scores").objects().map { it.toScore() },
        frozenBureaus = json.array("frozenBureaus").strings().toSet(),
        chat = json.array("chat").objects().map { it.toMessage() }.ifEmpty { AppState().chat },
    )

    private fun JSONObject.array(key: String): JSONArray = optJSONArray(key) ?: JSONArray()
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
    private fun List<Bureau>.bureauJsonArray() = JSONArray().apply { forEach { put(it.name) } }
    private fun List<String>.stringJsonArray() = JSONArray().apply { forEach { put(it) } }
    private fun JSONArray.strings(): List<String> = (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    private fun CreditReport.toJson() = JSONObject()
        .put("id", id).put("fileName", fileName).put("bureau", bureau.name)
        .put("score", score ?: JSONObject.NULL).put("accountCount", accountCount)
        .put("importedAt", importedAt).put("extractionMethod", extractionMethod)

    private fun CreditAccount.toJson() = JSONObject()
        .put("id", id).put("creditor", creditor).put("accountSuffix", accountSuffix)
        .put("bureau", bureau.name).put("status", status).put("balance", balance ?: JSONObject.NULL)
        .put("paymentStatus", paymentStatus).put("openedDate", openedDate)

    private fun Finding.toJson() = JSONObject()
        .put("id", id).put("type", type.name).put("title", title).put("detail", detail)
        .put("severity", severity.name).put("creditor", creditor)
        .put("bureaus", bureaus.bureauJsonArray()).put("accountIds", accountIds.stringJsonArray())
        .put("legalBasis", legalBasis.stringJsonArray())

    private fun Dispute.toJson() = JSONObject()
        .put("id", id).put("findingId", findingId).put("creditor", creditor)
        .put("bureaus", bureaus.bureauJsonArray()).put("reason", reason).put("letter", letter)
        .put("status", status.name).put("createdAt", createdAt).put("reviewBy", reviewBy)

    private fun ScoreSnapshot.toJson() = JSONObject()
        .put("bureau", bureau.name).put("score", score).put("recordedAt", recordedAt)

    private fun ChatMessage.toJson() = JSONObject()
        .put("id", id).put("fromUser", fromUser).put("text", text).put("createdAt", createdAt)

    private fun JSONObject.toReport() = CreditReport(
        id = optString("id"), fileName = optString("fileName"), bureau = Bureau.from(optString("bureau")),
        score = optIntOrNull("score"), accountCount = optInt("accountCount"),
        importedAt = optLong("importedAt"), extractionMethod = optString("extractionMethod", "PDF text"),
    )

    private fun JSONObject.toAccount() = CreditAccount(
        id = optString("id"), creditor = optString("creditor"), accountSuffix = optString("accountSuffix"),
        bureau = Bureau.from(optString("bureau")), status = optString("status", "Unknown"),
        balance = optIntOrNull("balance"), paymentStatus = optString("paymentStatus"), openedDate = optString("openedDate"),
    )

    private fun JSONObject.toFinding() = Finding(
        id = optString("id"), type = enumValueOrDefault(optString("type"), FindingType.REVIEW_NEEDED),
        title = optString("title"), detail = optString("detail"),
        severity = enumValueOrDefault(optString("severity"), Severity.LOW), creditor = optString("creditor"),
        bureaus = array("bureaus").strings().map(Bureau::from), accountIds = array("accountIds").strings(),
        legalBasis = array("legalBasis").strings(),
    )

    private fun JSONObject.toDispute() = Dispute(
        id = optString("id"), findingId = optString("findingId"), creditor = optString("creditor"),
        bureaus = array("bureaus").strings().map(Bureau::from), reason = optString("reason"), letter = optString("letter"),
        status = enumValueOrDefault(optString("status"), DisputeStatus.DRAFT),
        createdAt = optLong("createdAt"), reviewBy = optLong("reviewBy"),
    )

    private fun JSONObject.toScore() = ScoreSnapshot(
        bureau = Bureau.from(optString("bureau")), score = optInt("score"), recordedAt = optLong("recordedAt"),
    )

    private fun JSONObject.toMessage() = ChatMessage(
        id = optString("id"), fromUser = optBoolean("fromUser"), text = optString("text"), createdAt = optLong("createdAt"),
    )

    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else optInt(key)

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    companion object { private const val KEY_STATE = "app_state_v1" }
}
