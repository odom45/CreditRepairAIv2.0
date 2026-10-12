package com.creditrepairai.v2.billing

data class Entitlement(val active: Boolean = false, val expiresAt: Long = 0, val state: String = "Not subscribed") {
    fun hasAccess(now: Long) = active && expiresAt > now
}
enum class PurchaseDisposition { IGNORE, PENDING, VERIFY }
object BillingPolicy {
    fun disposition(productMatches: Boolean, purchased: Boolean, pending: Boolean): PurchaseDisposition = when {
        !productMatches -> PurchaseDisposition.IGNORE
        pending -> PurchaseDisposition.PENDING
        purchased -> PurchaseDisposition.VERIFY
        else -> PurchaseDisposition.IGNORE
    }
    fun accountId(subject: String) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(subject.toByteArray()).joinToString("") { "%02x".format(it) }
}
