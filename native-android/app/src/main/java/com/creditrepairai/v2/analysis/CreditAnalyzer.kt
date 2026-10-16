package com.creditrepairai.v2.analysis

import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.CreditAccount
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.Finding
import com.creditrepairai.v2.model.FindingType
import com.creditrepairai.v2.model.ScoreImpactStage
import com.creditrepairai.v2.model.Severity
import java.util.Locale

object CreditAnalyzer {
    fun analyze(accounts: List<CreditAccount>, reports: List<CreditReport>): List<Finding> {
        val importedBureaus = reports.map { it.bureau }.filter { it != Bureau.UNKNOWN }.toSet()
        val findings = mutableListOf<Finding>()
        val grouped = accounts.groupBy { accountKey(it) }

        grouped.forEach { (_, versions) ->
            val creditor = versions.first().creditor
            val versionBureaus = versions.map { it.bureau }.filter { it != Bureau.UNKNOWN }.toSet()
            val balances = versions.mapNotNull { it.balance }.distinct()
            val statuses = versions.map { normalizeStatus(it.status) }.filter { it != "unknown" }.distinct()

            if (balances.size > 1 && (balances.maxOrNull()!! - balances.minOrNull()!!) >= 25) {
                findings += Finding(
                    type = FindingType.BALANCE_MISMATCH,
                    title = "Balance differs across bureaus",
                    detail = versions.joinToString(" • ") { "${it.bureau.displayName}: ${it.balance?.let { value -> "$$value" } ?: "not shown"}" },
                    severity = Severity.HIGH,
                    creditor = creditor,
                    bureaus = versionBureaus.toList(),
                    accountIds = versions.map { it.id },
                    legalBasis = listOf("15 U.S.C. § 1681e(b)", "15 U.S.C. § 1681i"),
                )
            }

            if (statuses.size > 1) {
                findings += Finding(
                    type = FindingType.STATUS_MISMATCH,
                    title = "Account status does not match",
                    detail = versions.joinToString(" • ") { "${it.bureau.displayName}: ${it.status}" },
                    severity = Severity.HIGH,
                    creditor = creditor,
                    bureaus = versionBureaus.toList(),
                    accountIds = versions.map { it.id },
                    legalBasis = listOf("15 U.S.C. § 1681e(b)", "15 U.S.C. § 1681i(a)"),
                )
            }

            if (importedBureaus.size >= 2 && versionBureaus.isNotEmpty() && versionBureaus != importedBureaus) {
                val missing = importedBureaus - versionBureaus
                findings += Finding(
                    type = FindingType.MISSING_BUREAU,
                    title = "Reporting differs by bureau",
                    detail = "Present on ${versionBureaus.joinToString { it.displayName }}; not found on ${missing.joinToString { it.displayName }}. This is a review flag, not proof of an error.",
                    severity = Severity.MEDIUM,
                    creditor = creditor,
                    bureaus = (versionBureaus + missing).toList(),
                    accountIds = versions.map { it.id },
                    legalBasis = listOf("No general duty to report to every bureau; verify whether any reported field is inaccurate"),
                )
            }

            versions.groupBy { it.bureau }.filterValues { it.size > 1 }.forEach { (bureau, duplicates) ->
                findings += Finding(
                    type = FindingType.POSSIBLE_DUPLICATE,
                    title = "Possible duplicate tradeline",
                    detail = "${duplicates.size} similar entries were found on ${bureau.displayName}. Confirm account numbers and ownership before disputing.",
                    severity = Severity.HIGH,
                    creditor = creditor,
                    bureaus = listOf(bureau),
                    accountIds = duplicates.map { it.id },
                    legalBasis = listOf("15 U.S.C. § 1681e(b)", "15 U.S.C. § 1681i"),
                )
            }

            val negative = versions.firstOrNull { isNegative(it.status) || isNegative(it.paymentStatus) }
            if (versions.size == 1 && negative != null) {
                findings += Finding(
                    type = FindingType.REVIEW_NEEDED,
                    title = "Negative item needs verification",
                    detail = "${negative.bureau.displayName} reports '${negative.status}'. Compare this with your statements before choosing a dispute reason.",
                    severity = Severity.MEDIUM,
                    creditor = creditor,
                    bureaus = listOf(negative.bureau),
                    accountIds = listOf(negative.id),
                    legalBasis = listOf("15 U.S.C. § 1681i"),
                )
            }
        }

        return findings.distinctBy { Triple(it.type, it.accountIds.sorted(), it.bureaus.sortedBy(Bureau::name)) }
            .sortedWith(compareBy<Finding> { it.severity.ordinal }.thenBy { it.creditor })
    }

    fun impactPlan(findings: List<Finding>, disputes: List<com.creditrepairai.v2.model.Dispute>): List<ScoreImpactStage> = listOf(
        ScoreImpactStage(
            window = "Prepare",
            title = "Verify the original records",
            expectation = if (findings.isEmpty()) "Import all three reports and verify the extracted data." else "Review ${findings.size} flagged item(s), compare reporting dates, and gather supporting records. A flag is not proof of an error.",
            positiveInfluence = "Educational checklist; results are not guaranteed",
        ),
        ScoreImpactStage(
            window = "Review and follow up",
            title = "Track actual correspondence",
            expectation = if (disputes.isEmpty()) "Prepare a draft only when you can identify a supported factual issue." else "Track ${disputes.size} dispute(s), keep receipts, and record the company's actual response. Check-ins are not response guarantees or legal deadlines.",
            positiveInfluence = "No predicted deletion, score change, or response time",
        ),
        ScoreImpactStage(
            window = "Maintain records",
            title = "Review goals and manual score history",
            expectation = "Maintain payment records, compare dated reports, and record scores without assuming causation or predicting a lender decision.",
            positiveInfluence = "Personal goals are preferences, not outcome forecasts",
        ),
    )

    private fun accountKey(account: CreditAccount): String {
        val creditor = account.creditor.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "")
        val suffix = account.accountSuffix.filter(Char::isLetterOrDigit).uppercase(Locale.US)
        if (suffix.isNotBlank()) return "$creditor:suffix:$suffix"
        val fallback = listOf(account.openedDate, account.accountType)
            .joinToString(":")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9:]"), "")
        // An account type alone is not an identity. Retain the full opened-date
        // fallback instead of comparing only its final four characters.
        return if (account.openedDate.isNotBlank()) "$creditor:opened:$fallback"
        else "$creditor:unmatched:${account.id}"
    }

    private fun normalizeStatus(status: String): String = status.lowercase(Locale.US)
        .replace("charge off", "charge-off").replace("paid in full", "paid").trim()

    private fun isNegative(value: String): Boolean {
        val normalized = value.lowercase(Locale.US)
        return listOf("charge", "collection", "late", "delinquent", "past due", "repossession").any(normalized::contains)
    }
}
