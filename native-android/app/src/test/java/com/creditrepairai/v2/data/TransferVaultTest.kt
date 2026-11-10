package com.creditrepairai.v2.data

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class TransferVaultTest {
    @Test fun webCryptoInteroperability() {
        val raw = requireNotNull(javaClass.classLoader?.getResourceAsStream("transfer/web-vault.json")).bufferedReader().use { it.readText() }
        val plaintext = TransferVault.decrypt(raw, "synthetic interoperability passphrase".toCharArray(), "interop-synthetic-owner")
        assertEquals("Synthetic shared case", JSONObject(plaintext).getJSONObject("goal").getString("description"))
        assertThrows(Exception::class.java) { TransferVault.decrypt(raw, "wrong password here".toCharArray(), "interop-synthetic-owner") }
        assertThrows(Exception::class.java) { TransferVault.decrypt(raw, "synthetic interoperability passphrase".toCharArray(), "other-owner") }
    }
    @Test fun roundTripRandomNonceAndTamperRejection() {
        val secret = "synthetic test passphrase".toCharArray()
        val a = TransferVault.encrypt("{\"version\":1}", secret, "owner")
        val b = TransferVault.encrypt("{\"version\":1}", secret, "owner")
        assertNotEquals(JSONObject(a).getString("iv"), JSONObject(b).getString("iv"))
        assertEquals("{\"version\":1}", TransferVault.decrypt(a, secret, "owner"))
        val altered = JSONObject(a).apply {
            val c = getString("ciphertext")
            put("ciphertext", (if (c[0] == 'A') "B" else "A") + c.substring(1))
        }.toString()
        assertThrows(Exception::class.java) { TransferVault.decrypt(altered, secret, "owner") }
    }
}
