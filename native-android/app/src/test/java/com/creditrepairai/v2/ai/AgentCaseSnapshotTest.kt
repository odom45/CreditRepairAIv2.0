package com.creditrepairai.v2.ai

import com.creditrepairai.v2.model.*
import org.junit.Assert.*
import org.junit.Test

class AgentCaseSnapshotTest {
    private val dispute = Dispute(id = "selected", findingId = "flag", creditor = "Selected company",
        bureaus = listOf(Bureau.EQUIFAX), reason = "Verify balance", letter = "Fictional draft")
    private fun account(index: Int, creditor: String = "Other company") = CreditAccount(id = "a$index", creditor = creditor, bureau = Bureau.EQUIFAX)

    @Test fun completeBoundaryCaseRetainsEveryRecord() {
        val accounts = (1..200).map { account(it) }
        val state = AppState(accounts = accounts, disputes = (1..100).map { dispute.copy(id = "d$it") })
        assertEquals(accounts, state.forAgentCase(null).accounts)
        assertEquals(100, state.forAgentCase(null).disputes.size)
    }
    @Test fun oversizedCaseIsRejectedInsteadOfSilentlyDroppingTheLastAccount() {
        val state = AppState(accounts = (1..201).map { account(it) })
        val error = runCatching { state.forAgentCase(null) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("No partial case was sent"))
        assertEquals(201, state.accounts.size)
    }
    @Test fun focusedDisputeWorksWhenTheWholeCaseExceedsTheLimit() {
        val selected = account(202, dispute.creditor)
        val state = AppState(accounts = (1..201).map { account(it) } + selected, disputes = listOf(dispute))
        val scoped = state.forAgentCase(dispute.id)
        assertEquals(listOf(selected), scoped.accounts)
        assertEquals(listOf(dispute), scoped.disputes)
        assertEquals(202, state.accounts.size)
    }
    @Test(expected = IllegalStateException::class) fun tooManyDisputesCannotBeSilentlyOmitted() {
        AppState(disputes = (1..101).map { dispute.copy(id = "d$it") }).forAgentCase(null)
    }
    @Test(expected = IllegalStateException::class) fun deletedSelectionCannotFallBackToTheWholeCase() {
        AppState(disputes = listOf(dispute)).forAgentCase("deleted")
    }
}
