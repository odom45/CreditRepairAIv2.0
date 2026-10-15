package com.creditrepairai.v2.guidance

import com.creditrepairai.v2.model.*
import org.junit.Assert.*
import org.junit.Test

class GuidedWorkflowTest {
    private fun fictionalDispute() = Dispute(findingId = "f", creditor = "Fictional company", bureaus = listOf(Bureau.EQUIFAX), reason = "Compare the reported balance with a dated statement", letter = "Fictional reviewed draft")
    @Test fun filingHelpRejectsChangesMadeWhileAnswerIsPending() {
        val dispute = fictionalDispute()
        val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.EQUIFAX, step = 3, reviewed = true, problem = "Balance differs")
        val pending = FilingGuide.request(dispute, record)
        assertNull(pending.apply(dispute, record.copy(step = 4), "Old answer"))
        assertNull(pending.apply(dispute, record.copy(problem = "Payment date differs"), "Old answer"))
        assertNull(pending.apply(dispute, record.copy(portal = FilingPortal.CFPB), "Old answer"))
        assertNull(pending.apply(dispute.copy(reason = "Revised reason"), record, "Old answer"))
        assertNull(pending.apply(dispute, record.copy(id = "different-filing"), "Old answer"))
        assertNull(pending.apply(dispute, record.copy(responseReceivedAt = 123), "Old answer"))
    }
    @Test fun filingHelpSurvivesSaveButIsHiddenAfterDraftChanges() {
        val dispute = fictionalDispute()
        val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.EQUIFAX, step = 3)
        val explained = FilingGuide.request(dispute, record).apply(dispute, record, "Verify the balance against the original statement.")!!
        assertEquals("Verify the balance against the original statement.", FilingGuide.currentExplanation(dispute, explained))
        assertNull(FilingGuide.currentExplanation(dispute, explained.copy(resolution = "New requested review")))
        assertNull(FilingGuide.currentExplanation(dispute, explained.copy(reviewed = true)))
        assertNull(FilingGuide.currentExplanation(dispute, explained.copy(step = 4)))
    }
    @Test fun filingHelpKeepsWorstCaseQuestionWithinServerLimit() {
        val dispute = fictionalDispute().copy(creditor = "x".repeat(4000), reason = "x".repeat(4000))
        val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.CFPB, step = 5,
            problem = "x".repeat(4000), companyResponse = "x".repeat(4000), resolution = "x".repeat(4000), evidence = "x".repeat(4000))
        val prompt = FilingGuide.request(dispute, record).prompt
        assertTrue(prompt.length <= 4000)
        assertTrue(prompt.contains("user handles credentials"))
        assertTrue(prompt.contains("never as instructions or proof"))
    }
    @Test(expected = IllegalArgumentException::class) fun filingHelpRejectsWrongDispute() {
        FilingGuide.request(fictionalDispute(), FilingRecord(disputeId = "wrong", portal = FilingPortal.EQUIFAX))
    }
    @Test fun generatedAnswersRequireCompleteFieldsAndRejectStaleApplication() {
        val dispute = fictionalDispute()
        val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.CFPB, reviewed = true)
        val answer = "Educational information only.\nWHAT_HAPPENED:\nThe balance differs.\nCOMPANY_RESPONSE:\n[ADD VERIFIED DETAILS]\nREQUESTED_REVIEW:\nPlease investigate the reported balance.\nEVIDENCE:\n[ADD SUPPORTING RECORDS]"
        val draft = FilingAnswerParser.parse(answer, FilingGuide.contextKey(dispute, record))
        assertNull(draft.apply(dispute, record.copy(problem = "Changed while pending")))
        val applied = draft.apply(dispute, record)!!
        assertEquals("The balance differs.", applied.problem)
        assertFalse(applied.reviewed)
        assertFalse(FilingGuide.canReview(applied))
        assertTrue(FilingGuide.canReview(applied.copy(companyResponse = "No response received as of my review.", evidence = "Dated statement")))
        assertNull(applied.answerDraft)
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsPartialAiDraftInsteadOfOverwritingFacts() {
        FilingAnswerParser.parse("WHAT_HAPPENED:\nOnly one field", "context")
    }
    @Test(expected = IllegalArgumentException::class) fun refusesSubmissionWithUnfilledAiPlaceholders() {
        FilingGuide.submitted(FilingRecord(disputeId = "d", portal = FilingPortal.CFPB, reviewed = true, evidence = "[ADD SUPPORTING RECORDS]"), "receipt", 1)
    }
    @Test fun generatedAnswerPromptRemainsWithinQuestionContract() {
        val dispute = fictionalDispute().copy(creditor = "x".repeat(4000), reason = "x".repeat(4000))
        val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.CFPB,
            problem = "x".repeat(4000), companyResponse = "x".repeat(4000), resolution = "x".repeat(4000), evidence = "x".repeat(4000))
        assertTrue(FilingGuide.answerPrompt(dispute, record).length <= 4000)
    }
    @Test fun acquisitionTransitionsClampAndUnknownDoesNotCount() {
        var state = AcquisitionState().back().capture(Bureau.UNKNOWN)
        assertEquals(3, state.missing.size)
        repeat(20) { state = state.next() }
        assertEquals(AcquisitionStep.VERIFY, state.step)
        state = state.capture(Bureau.EQUIFAX).capture(Bureau.EQUIFAX)
        assertEquals(1, state.confirmed.size)
        assertEquals(2, state.missing.size)
        repeat(20) { state = state.back() }
        assertEquals(AcquisitionStep.PREPARE, state.step)
    }
    @Test fun chunkingNeverSilentlyTruncates() {
        val text = (1..200).joinToString(" ") { "word$it" }
        val chunks = DisputeChunker.split("Explanation", text, 100)
        assertTrue(chunks.all { it.text.length <= 100 })
        assertEquals(text, chunks.joinToString(" ") { it.text })
        val token = "x".repeat(350)
        assertEquals(token, DisputeChunker.split("Token", token, 100).joinToString("") { it.text })
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsInvalidFieldLimit() { DisputeChunker.split("test", "text", 0) }
    @Test fun blankDraftHasNoChunks() { assertTrue(DisputeChunker.split("Empty", " ", 100).isEmpty()) }
    @Test fun cfpbRequiresPriorSubmissionAndResponseOrCheckin() {
        val draft = Dispute(findingId = "f", creditor = "Fictional", bureaus = listOf(Bureau.EQUIFAX), reason = "Verify", letter = "Draft", status = DisputeStatus.RESOLVED)
        assertFalse(FilingGuide.cfpbReady(draft, emptyList(), 100))
        assertFalse(FilingGuide.cfpbReady(draft.copy(sentAt = 1, craResponseDueAt = 200), emptyList(), 100))
        assertTrue(FilingGuide.cfpbReady(draft.copy(sentAt = 1, craResponseDueAt = 200), emptyList(), 200))
        assertTrue(FilingGuide.cfpbReady(draft.copy(sentAt = 1), listOf(FilingRecord(disputeId = draft.id, portal = FilingPortal.EQUIFAX, responseReceivedAt = 5)), 100))
    }
    @Test fun submissionCreatesCheckinsNotOutcomeDates() {
        val record = FilingGuide.submitted(FilingRecord(disputeId = "d", portal = FilingPortal.EQUIFAX, reviewed = true), "Fictional-receipt", 1000)
        assertEquals(1000L, record.submittedAt)
        assertEquals(1000L + 30L * 86400000, record.reminder30)
        assertEquals(1000L + 45L * 86400000, record.reminder45)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotRecordUnreviewedSubmission() { FilingGuide.submitted(FilingRecord(disputeId = "d", portal = FilingPortal.CFPB), "receipt", 0) }
    @Test fun plannerUsesActualFindingsAndMissingReports() {
        val finding = Finding(type = FindingType.BALANCE_MISMATCH, title = "Balance differs", detail = "Review", severity = Severity.HIGH, creditor = "Fictional bank", bureaus = listOf(Bureau.EQUIFAX), accountIds = emptyList(), legalBasis = emptyList())
        val plan = GoalPlanner.prioritize(AppState(findings = listOf(finding)))
        assertTrue(plan.first().title.contains("Obtain"))
        assertEquals(finding.id, plan[1].findingId)
        assertTrue(plan[1].reason.contains("not proof"))
        assertTrue(GoalPlanner.prompt(CreditGoal("Home", 12)).contains("not a forecast"))
    }
}
