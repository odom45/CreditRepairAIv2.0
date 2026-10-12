package com.creditrepairai.v2.guidance

import com.creditrepairai.v2.model.*
import java.util.UUID

const val EDUCATIONAL_LABEL = "Educational information only — not legal or financial advice."

enum class AcquisitionStep { PREPARE, REQUEST, SAVE_PDF, IMPORT, VERIFY }
data class AcquisitionState(val step: AcquisitionStep = AcquisitionStep.PREPARE, val confirmed: Set<Bureau> = emptySet()) {
    fun next() = copy(step = AcquisitionStep.entries[(step.ordinal + 1).coerceAtMost(AcquisitionStep.entries.lastIndex)])
    fun back() = copy(step = AcquisitionStep.entries[(step.ordinal - 1).coerceAtLeast(0)])
    fun capture(bureau: Bureau) = if (bureau == Bureau.UNKNOWN) this else copy(confirmed = confirmed + bureau)
    val missing: List<Bureau> get() = Bureau.entries.filter { it != Bureau.UNKNOWN && it !in confirmed }
}

enum class FilingPortal(val label: String, val url: String) {
    EXPERIAN("Experian", "https://www.experian.com/disputes/main.html"),
    EQUIFAX("Equifax", "https://www.equifax.com/personal/credit-report-services/credit-dispute/"),
    TRANSUNION("TransUnion", "https://www.transunion.com/credit-disputes/dispute-your-credit"),
    CFPB("CFPB", "https://www.consumerfinance.gov/complaint/")
}
data class FilingRecord(
    val id: String = UUID.randomUUID().toString(), val disputeId: String, val portal: FilingPortal,
    val step: Int = 0, val reviewed: Boolean = false, val confirmation: String = "",
    val problem: String = "", val companyResponse: String = "", val resolution: String = "",
    val evidence: String = "", val submittedAt: Long? = null, val responseReceivedAt: Long? = null,
    val reminder30: Long? = null, val reminder45: Long? = null,
)
data class DraftChunk(val label: String, val text: String)
object DisputeChunker {
    /** User supplies the current field limit: portal limits change and are not assumed. Never discard text. */
    fun split(label: String, text: String, limit: Int): List<DraftChunk> {
        require(limit in 100..10000)
        val chunks = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.length > limit) {
            val end = remaining.lastIndexOf(' ', limit).takeIf { it >= limit / 2 } ?: limit
            chunks += remaining.substring(0, end)
            remaining = remaining.substring(end).trimStart()
        }
        if (remaining.isNotEmpty()) chunks += remaining
        return chunks.mapIndexed { i, value -> DraftChunk("$label ${i + 1}/${chunks.size}", value) }
    }
    fun fields(dispute: Dispute, filing: FilingRecord, limit: Int): List<DraftChunk> = buildList {
        addAll(split("Item / furnisher", dispute.creditor, limit))
        addAll(split("What happened", filing.problem.ifBlank { dispute.reason }, limit))
        if (filing.portal == FilingPortal.CFPB) addAll(split("Company response", filing.companyResponse, limit))
        addAll(split("Requested review", filing.resolution.ifBlank { "Please review the supporting records and correct information you determine to be inaccurate or incomplete. Send written investigation results." }, limit))
        addAll(split("Evidence", filing.evidence.ifBlank { "Attach copies of records supporting each disputed fact, the report page, and prior correspondence. Keep originals." }, limit))
    }
}
data class CreditGoal(val description: String = "", val targetMonths: Int = 12, val aiPlan: String = "")
data class PlanAction(val title: String, val reason: String, val findingId: String? = null)
object GoalPlanner {
    fun prioritize(state: AppState): List<PlanAction> = buildList {
        val missing = Bureau.entries.filter { it != Bureau.UNKNOWN && state.reports.none { report -> report.bureau == it } }
        if (missing.isNotEmpty()) add(PlanAction("Obtain ${missing.joinToString { it.displayName }} reports", "A complete comparison needs each original report; reports may not contain scores."))
        state.findings.sortedBy { it.severity.ordinal }.take(10).forEach {
            add(PlanAction("Verify: ${it.title}", "${it.creditor}: compare extracted details with dated original records. A finding is not proof of an error.", it.id))
        }
        state.disputes.filter { it.sentAt != null && it.status != DisputeStatus.RESOLVED }.forEach {
            add(PlanAction("Check correspondence for ${it.creditor}", "Record the actual company response. Reminder dates are check-in dates, not promised results."))
        }
        add(PlanAction("Record a dated score and review payment records", "Score history is manual. Do not treat a change as caused by a dispute or as an approval prediction."))
    }
    fun prompt(goal: CreditGoal): String = "Build an educational prioritized, sequenced action plan for this goal: ${goal.description}. The user's target is ${goal.targetMonths} months, not a forecast or promise. Use actual findings and saved dispute history; identify evidence gaps; do not predict approvals, score changes, deletion, or completion dates."
}
object FilingGuide {
    val steps = listOf(
        "Review facts" to "Compare the draft with original reports and supporting records. A flag can be caused by different reporting dates or parsing errors.",
        "Open the official portal" to "Only you type your credentials. This app cannot read the browser page or interact with its form.",
        "Choose the reported item" to "Select the company and account that match your report. A field can differ from our checklist; follow the official instructions.",
        "Paste the matching explanation" to "Copy the matching reviewed paragraph below. If the field is shorter, change the chunk limit. Never replace facts with a generic allegation.",
        "Attach evidence" to "Use the portal's file picker yourself. Include only needed records. The app does not inspect or upload portal attachments.",
        "Review and submit yourself" to "Read every answer. Only you press the official Submit button. The app never submits, clicks, or fills a portal form.",
        "Record the receipt" to "After you actually submit, save the confirmation number here and schedule check-ins. These dates are not guarantees of response or resolution.",
    )
    fun cfpbReady(dispute: Dispute, records: List<FilingRecord>, now: Long): Boolean =
        dispute.sentAt != null && (records.any { it.disputeId == dispute.id && it.responseReceivedAt != null } ||
            dispute.craResponseDueAt?.let { it <= now } == true)
    fun submitted(record: FilingRecord, confirmation: String, now: Long): FilingRecord {
        require(record.reviewed && confirmation.isNotBlank())
        return record.copy(confirmation = confirmation.trim().take(120), submittedAt = now, step = 6,
            reminder30 = now + 30L * 86400000, reminder45 = now + 45L * 86400000)
    }
}
