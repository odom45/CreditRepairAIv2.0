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

        return findings.distinctBy { Triple(it.type, accountKey(it.creditor, ""), it.bureaus.sortedBy(Bureau::name)) }
            .sortedWith(compareBy<Finding> { it.severity.ordinal }.thenBy { it.creditor })
    }

    fun impactPlan(findings: List<Finding>, disputes: List<com.creditrepairai.v2.model.Dispute>): List<ScoreImpactStage> = listOf(
        ScoreImpactStage(
            window = "Now–7 days",
            title = "Correct the file and lower utilization",
            expectation = if (findings.isEmpty()) "Import all three reports and verify the extracted data." else "Review ${findings.size} flagged item(s), add evidence, and send only accurate disputes. Card-paydown updates may report on the next statement cycle.",
            positiveInfluence = "Possible soon; no guaranteed score change",
        ),
        ScoreImpactStage(
            window = "8–45 days",
            title = "Investigation and bureau updates",
            expectation = if (disputes.isEmpty()) "Generate a dispute only for information you can identify as inaccurate or incomplete." else "Track ${disputes.size} dispute(s). Many FCRA investigations are generally completed within 30 days, with exceptions.",
            positiveInfluence = "Depends on the item, scoring model, and outcome",
        ),
        ScoreImpactStage(
            window = "45–120 days",
            title = "Build durable positive history",
            expectation = "Keep every payment on time, maintain low revolving utilization, and avoid unnecessary hard inquiries while updated data propagates.",
            positiveInfluence = "Typically gradual and individualized",
        ),
    )

    private fun accountKey(account: CreditAccount): String {
        val fallback = listOf(account.openedDate, account.accountType)
            .joinToString(":")
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9:]"), "")
        return accountKey(account.creditor, account.accountSuffix.ifBlank { fallback })
    }
    private fun accountKey(creditor: String, suffix: String) =
        creditor.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "") + ":" + suffix.takeLast(4)

    private fun normalizeStatus(status: String): String = status.lowercase(Locale.US)
        .replace("charge off", "charge-off").replace("paid in full", "paid").trim()

    private fun isNegative(value: String): Boolean {
        val normalized = value.lowercase(Locale.US)
        return listOf("charge", "collection", "late", "delinquent", "past due", "repossession").any(normalized::contains)
    }
}
