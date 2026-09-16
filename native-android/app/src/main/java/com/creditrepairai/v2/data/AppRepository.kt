package com.creditrepairai.v2.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AppRepository(context: Context) {
    private val preferences = context.getSharedPreferences("credit_repair_private", Context.MODE_PRIVATE)

    fun load(): AppState {
        val encrypted = preferences.getString(KEY_ENCRYPTED_STATE, null)
        if (encrypted != null) {
            return runCatching { decode(JSONObject(decrypt(encrypted))) }.getOrElse {
                // A corrupt or invalidated key must never cause sensitive state to be
                // written back in plaintext. Start with an empty local session.
                clear()
                AppState()
            }
        }

        // One-time migration from early preview builds that used a private but
        // unencrypted SharedPreferences value.
        val legacy = preferences.getString(KEY_LEGACY_STATE, null) ?: return AppState()
        return runCatching { decode(JSONObject(legacy)) }.getOrElse { AppState() }.also { state ->
            save(state)
            preferences.edit().remove(KEY_LEGACY_STATE).apply()
        }
    }

    fun save(state: AppState) {
        val encrypted = encrypt(encode(state).toString())
        preferences.edit()
            .putString(KEY_ENCRYPTED_STATE, encrypted)
            .remove(KEY_LEGACY_STATE)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().commit()
        keyStore().apply {
            if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
        }
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
        .put("accountType", accountType).put("responsibility", responsibility)
        .put("dateReported", dateReported).put("originalCreditor", originalCreditor)
        .put("pastDue", pastDue ?: JSONObject.NULL).put("creditLimit", creditLimit ?: JSONObject.NULL)
        .put("highBalance", highBalance ?: JSONObject.NULL).put("remarks", remarks)

    private fun Finding.toJson() = JSONObject()
        .put("id", id).put("type", type.name).put("title", title).put("detail", detail)
        .put("severity", severity.name).put("creditor", creditor)
        .put("bureaus", bureaus.bureauJsonArray()).put("accountIds", accountIds.stringJsonArray())
        .put("legalBasis", legalBasis.stringJsonArray())

    private fun Dispute.toJson() = JSONObject()
        .put("id", id).put("findingId", findingId).put("creditor", creditor)
        .put("bureaus", bureaus.bureauJsonArray()).put("reason", reason).put("letter", letter)
        .put("status", status.name).put("createdAt", createdAt).put("reviewBy", reviewBy)
        .put("sentAt", sentAt ?: JSONObject.NULL).put("craResponseDueAt", craResponseDueAt ?: JSONObject.NULL)

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
        accountType = optString("accountType"), responsibility = optString("responsibility"),
        dateReported = optString("dateReported"), originalCreditor = optString("originalCreditor"),
        pastDue = optIntOrNull("pastDue"), creditLimit = optIntOrNull("creditLimit"),
        highBalance = optIntOrNull("highBalance"), remarks = optString("remarks"),
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
        sentAt = optLongOrNull("sentAt"), craResponseDueAt = optLongOrNull("craResponseDueAt"),
    )

    private fun JSONObject.toScore() = ScoreSnapshot(
        bureau = Bureau.from(optString("bureau")), score = optInt("score"), recordedAt = optLong("recordedAt"),
    )

    private fun JSONObject.toMessage() = ChatMessage(
        id = optString("id"), fromUser = optBoolean("fromUser"), text = optString("text"), createdAt = optLong("createdAt"),
    )

    private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key) || !has(key)) null else optInt(key)
    private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else optLong(key)

    private fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
        val payload = JSONObject()
            .put("v", 1)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP))
        return payload.toString()
    }

    private fun decrypt(payload: String): String {
        val json = JSONObject(payload)
        require(json.optInt("v") == 1) { "Unsupported encrypted state version" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            encryptionKey(),
            GCMParameterSpec(128, Base64.decode(json.getString("iv"), Base64.NO_WRAP)),
        )
        return cipher.doFinal(Base64.decode(json.getString("ciphertext"), Base64.NO_WRAP)).toString(Charsets.UTF_8)
    }

    private fun encryptionKey(): SecretKey {
        val store = keyStore()
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generateKey()
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "credit_repair_local_state_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ENCRYPTED_STATE = "app_state_encrypted_v1"
        private const val KEY_LEGACY_STATE = "app_state_v1"
    }
}
