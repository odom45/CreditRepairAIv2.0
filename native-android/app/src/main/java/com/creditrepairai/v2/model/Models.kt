package com.creditrepairai.v2.model

import java.util.UUID

enum class Bureau(val displayName: String) {
    TRANSUNION("TransUnion"),
    EXPERIAN("Experian"),
    EQUIFAX("Equifax"),
    UNKNOWN("Unknown");

    companion object {
        fun from(value: String): Bureau = entries.firstOrNull {
            it.name.equals(value, true) || it.displayName.equals(value, true)
        } ?: UNKNOWN
    }
}

enum class FindingType {
    BALANCE_MISMATCH,
    STATUS_MISMATCH,
    MISSING_BUREAU,
    POSSIBLE_DUPLICATE,
    LATE_PAYMENT_MISMATCH,
    REVIEW_NEEDED,
}

enum class Severity { HIGH, MEDIUM, LOW }

enum class DisputeStatus { DRAFT, READY, SENT, INVESTIGATING, RESOLVED }

data class CreditAccount(
    val id: String = UUID.randomUUID().toString(),
    val creditor: String,
    val accountSuffix: String = "",
    val bureau: Bureau,
    val status: String = "Unknown",
    val balance: Int? = null,
    val paymentStatus: String = "",
    val openedDate: String = "",
    val accountType: String = "",
    val responsibility: String = "",
    val dateReported: String = "",
    val originalCreditor: String = "",
    val pastDue: Int? = null,
    val creditLimit: Int? = null,
    val highBalance: Int? = null,
    val remarks: String = "",
)

data class CreditReport(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val bureau: Bureau,
    val score: Int? = null,
    val accountCount: Int,
    val importedAt: Long = System.currentTimeMillis(),
    val extractionMethod: String = "PDF text",
)

data class Finding(
    val id: String = UUID.randomUUID().toString(),
    val type: FindingType,
    val title: String,
    val detail: String,
    val severity: Severity,
    val creditor: String,
    val bureaus: List<Bureau>,
    val accountIds: List<String>,
    val legalBasis: List<String>,
)

data class Dispute(
    val id: String = UUID.randomUUID().toString(),
    val findingId: String,
    val creditor: String,
    val bureaus: List<Bureau>,
    val reason: String,
    val letter: String,
    val status: DisputeStatus = DisputeStatus.DRAFT,
    val createdAt: Long = System.currentTimeMillis(),
    val reviewBy: Long = System.currentTimeMillis() + 35L * 24 * 60 * 60 * 1000,
    val sentAt: Long? = null,
    val craResponseDueAt: Long? = null,
)

data class ScoreSnapshot(
    val bureau: Bureau,
    val score: Int,
    val recordedAt: Long = System.currentTimeMillis(),
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val fromUser: Boolean,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class AppState(
    val reports: List<CreditReport> = emptyList(),
    val accounts: List<CreditAccount> = emptyList(),
    val findings: List<Finding> = emptyList(),
    val disputes: List<Dispute> = emptyList(),
    val scores: List<ScoreSnapshot> = emptyList(),
    val frozenBureaus: Set<String> = emptySet(),
    val chat: List<ChatMessage> = listOf(
        ChatMessage(
            fromUser = false,
            text = "I’m your secure CreditRepairAI guide. Import reports, then ask me to explain an item, weigh evidence, prepare a dispute plan, or check CFPB readiness. I use only a redacted case snapshot and retrieved, cited legal sources when the authenticated AI service is connected.",
        ),
    ),
)

data class ParsedReport(
    val report: CreditReport,
    val accounts: List<CreditAccount>,
    val extractedCharacters: Int,
)

data class ScoreImpactStage(
    val window: String,
    val title: String,
    val expectation: String,
    val positiveInfluence: String,
)
