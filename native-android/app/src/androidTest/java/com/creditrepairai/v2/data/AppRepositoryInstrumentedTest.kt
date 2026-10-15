package com.creditrepairai.v2.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.creditrepairai.v2.guidance.FilingAnswerDraft
import com.creditrepairai.v2.guidance.CreditGoal
import com.creditrepairai.v2.guidance.FilingGuide
import com.creditrepairai.v2.guidance.FilingPortal
import com.creditrepairai.v2.guidance.FilingRecord
import com.creditrepairai.v2.model.Dispute
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.AppState
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run only on a disposable debug installation containing fictional test records. */
@RunWith(AndroidJUnit4::class)
class AppRepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repository = AppRepository(context)
    @After fun cleanTestRecords() { repository.clear(preserveOwner = false) }
    @Test fun clearingThenSavingPreservesNewCaseAcrossRestart() {
        repository.selectOwner("fictional-owner-A")
        repository.save(AppState(goal = CreditGoal("Fictional old goal")))
        repository.clear()
        repository.save(AppState(goal = CreditGoal("Fictional new goal")))
        val restarted = AppRepository(context)
        restarted.selectOwner("fictional-owner-A")
        assertEquals("Fictional new goal", restarted.load().goal.description)
    }
    @Test fun otherOwnerCannotReadCaseAndCiphertextHasNoGoalText() {
        repository.selectOwner("fictional-owner-A")
        repository.save(AppState(goal = CreditGoal("Fictional private marker")))
        val prefs = context.getSharedPreferences("credit_repair_private", android.content.Context.MODE_PRIVATE)
        assertFalse(prefs.all.values.joinToString().contains("Fictional private marker"))
        repository.selectOwner("fictional-owner-B")
        assertEquals("", repository.load().goal.description)
    }
    @Test fun aiDraftFieldsAreEncryptedAndRestoredWithoutMarkingReviewed() {
        repository.selectOwner("fictional-owner-A")
        val draft = FilingAnswerDraft("fictional-context", "Fictional private draft marker", "No response", "Please review", "Dated statement")
        val record = FilingRecord(disputeId = "fictional-dispute", portal = FilingPortal.EQUIFAX, answerDraft = draft)
        repository.save(AppState(filings = listOf(record)))
        val restored = AppRepository(context).load().filings.single()
        assertEquals(draft, restored.answerDraft)
        assertFalse(restored.reviewed)
        val prefs = context.getSharedPreferences("credit_repair_private", android.content.Context.MODE_PRIVATE)
        assertFalse(prefs.all.values.joinToString().contains("Fictional private draft marker"))
    }
    @Test fun savedFilingExplanationIsEncryptedAndRestored() {
        repository.selectOwner("fictional-owner-A")
        val dispute = Dispute(findingId = "f", creditor = "Fictional", bureaus = listOf(Bureau.EQUIFAX), reason = "Verify facts", letter = "Fictional draft")
        val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.EQUIFAX, step = 3)
        val explained = FilingGuide.request(dispute, record).apply(dispute, record, "Fictional saved explanation")!!
        repository.save(AppState(disputes = listOf(dispute), filings = listOf(explained)))
        val restarted = AppRepository(context).load()
        assertEquals("Fictional saved explanation", FilingGuide.currentExplanation(restarted.disputes.single(), restarted.filings.single()))
        val prefs = context.getSharedPreferences("credit_repair_private", android.content.Context.MODE_PRIVATE)
        assertFalse(prefs.all.values.joinToString().contains("Fictional saved explanation"))
    }
}
