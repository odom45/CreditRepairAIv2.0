package com.creditrepairai.v2.billing

data class Entitlement(val active: Boolean = false, val expiresAt: Long = 0, val state: String = "Not subscribed", val caseHashes: Set<String> = emptySet()) {
    fun hasAccess(now: Long) = active && expiresAt > now
    fun hasCaseAccess(owner: String, disputeId: String, now: Long) = hasAccess(now) || BillingPolicy.caseId(owner, disputeId) in caseHashes
}
enum class AccessPlan(val productId: String, val label: String, val period: String) {
    MONTHLY("creditrepairai_monthly", "Monthly", "P1M"),
    YEARLY("creditrepairai_yearly", "Yearly", "P1Y"),
    CASE("creditrepairai_dispute", "One dispute", ""),
}
enum class PurchaseDisposition { IGNORE, PENDING, VERIFY }
object BillingPolicy {
    fun disposition(productMatches: Boolean, purchased: Boolean, pending: Boolean): PurchaseDisposition = when {
        !productMatches -> PurchaseDisposition.IGNORE
        pending -> PurchaseDisposition.PENDING
        purchased -> PurchaseDisposition.VERIFY
        else -> PurchaseDisposition.IGNORE
    }
    fun caseId(subject: String, disputeId: String) = accountId("$subject:$disputeId")
    fun accountId(subject: String) = java.security.MessageDigest.getInstance("SHA-256")
        .digest(subject.toByteArray()).joinToString("") { "%02x".format(it) }
}
