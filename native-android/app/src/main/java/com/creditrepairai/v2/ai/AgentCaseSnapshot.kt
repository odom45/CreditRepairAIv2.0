package com.creditrepairai.v2.ai

import com.creditrepairai.v2.model.AppState

/** Enforce the server contract before serialization; never send a silently partial case. */
internal fun AppState.forAgentCase(caseId: String?): AppState {
    val snapshot = if (caseId == null) this else {
        val dispute = disputes.singleOrNull { it.id == caseId }
            ?: error("The selected dispute is no longer available.")
        copy(disputes = listOf(dispute), findings = findings.filter { it.id == dispute.findingId },
            accounts = accounts.filter { it.creditor == dispute.creditor && it.bureau in dispute.bureaus },
            reports = reports.filter { it.bureau in dispute.bureaus }, filings = filings.filter { it.disputeId == caseId })
    }
    check(snapshot.accounts.size <= 200 && snapshot.findings.size <= 100 && snapshot.disputes.size <= 100) {
        "This case exceeds the AI request limits. No partial case was sent. Open Guided filing and choose one dispute for focused AI help; all imported records remain saved."
    }
    return snapshot
}
