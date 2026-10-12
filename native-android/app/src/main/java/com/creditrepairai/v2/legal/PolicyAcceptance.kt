package com.creditrepairai.v2.legal

data class PolicyAcceptance(
    val version: String, val digest: String, val acceptedAt: Long, val aiSharing: Boolean = false, val choiceAt: Long = acceptedAt,
) {
    val isCurrent: Boolean get() = version == PolicyDocuments.VERSION && digest == PolicyDocuments.DIGEST && acceptedAt > 0 && choiceAt >= acceptedAt
    val allowsAi: Boolean get() = isCurrent && aiSharing
    fun chooseAi(enabled: Boolean, now: Long) = copy(aiSharing = enabled, choiceAt = maxOf(now, choiceAt + 1))
    companion object {
        fun accept(agreed: Boolean, adult: Boolean, now: Long): PolicyAcceptance {
            require(agreed && adult && now > 0) { "Affirmative agreement and adult confirmation are required." }
            return PolicyAcceptance(PolicyDocuments.VERSION, PolicyDocuments.DIGEST, now)
        }
    }
}
