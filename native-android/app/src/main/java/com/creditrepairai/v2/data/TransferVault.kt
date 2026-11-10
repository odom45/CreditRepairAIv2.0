package com.creditrepairai.v2.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

/** Explicit encrypted transfer, interoperable with Web Crypto and Apple CryptoKit. */
object TransferVault {
    private fun key(password: CharArray, salt: ByteArray): SecretKeySpec {
        require(password.size >= 12 && salt.size == 16) { "Use a transfer passphrase with at least 12 characters." }
        val spec = PBEKeySpec(password, salt, 310_000, 256)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            try { SecretKeySpec(bytes, "AES") } finally { bytes.fill(0) }
        } finally { spec.clearPassword() }
    }
    fun encrypt(raw: String, password: CharArray, owner: String): String {
        require(raw.toByteArray(Charsets.UTF_8).size <= 20_000_000)
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD("creditrepairai-v1:$owner".toByteArray(Charsets.UTF_8))
        return JSONObject().put("format", "creditrepairai-vault-v1")
            .put("salt", Base64.getEncoder().encodeToString(salt))
            .put("iv", Base64.getEncoder().encodeToString(iv))
            .put("ciphertext", Base64.getEncoder().encodeToString(cipher.doFinal(raw.toByteArray(Charsets.UTF_8)))).toString()
    }
    fun decrypt(raw: String, password: CharArray, owner: String): String {
        require(raw.length <= 28_000_000) { "Vault exceeds the transfer limit." }
        val json = JSONObject(raw)
        require(json.getString("format") == "creditrepairai-vault-v1") { "Unsupported vault format." }
        val salt = Base64.getDecoder().decode(json.getString("salt"))
        val iv = Base64.getDecoder().decode(json.getString("iv"))
        require(salt.size == 16 && iv.size == 12) { "Invalid vault." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(password, salt), GCMParameterSpec(128, iv))
        cipher.updateAAD("creditrepairai-v1:$owner".toByteArray(Charsets.UTF_8))
        val bytes = cipher.doFinal(Base64.getDecoder().decode(json.getString("ciphertext")))
        require(bytes.size <= 20_000_000) { "Case exceeds the transfer limit." }
        return bytes.toString(Charsets.UTF_8)
    }
}
