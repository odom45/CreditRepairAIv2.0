package com.creditrepairai.v2.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.creditrepairai.v2.MainViewModel
import com.creditrepairai.v2.ai.CreditAgentGateway
import com.creditrepairai.v2.analysis.CreditReportParser
import com.creditrepairai.v2.auth.NativeSession
import com.creditrepairai.v2.data.AppRepository
import com.creditrepairai.v2.guidance.FilingGuide
import com.creditrepairai.v2.guidance.FilingRecord
import com.creditrepairai.v2.guidance.FilingAnswerDraft
import com.creditrepairai.v2.guidance.FilingPortal
import com.creditrepairai.v2.model.DisputeStatus
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Actual ViewModel/repository/UI orchestration with an offline, uncredentialed identity. */
class FilingFlowInstrumentedTest {
    @get:Rule(order = 0) val notifications = GrantPermissionRule.grant(android.Manifest.permission.POST_NOTIFICATIONS)
    @get:Rule(order = 1) val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ViewModelStore()
    private var vm: MainViewModel? = null
    private fun model(): MainViewModel {
        val session = object : NativeSession {
            override val email = "fictional@example.invalid"
            override val uid = "fictional-device-flow-owner"
            override val isEmailVerified = true
            override fun observeSession(onChange: (String?) -> Unit): () -> Unit = {}
            override suspend fun bearerToken(expectedSubject: String): String? = null
            override suspend fun signInWithEmail(email: String, password: String) = error("Offline fixture only")
            override suspend fun createAccount(email: String, password: String) = error("Offline fixture only")
            override suspend fun sendPasswordReset(email: String) = error("Offline fixture only")
            override suspend fun resendVerification() = error("Offline fixture only")
            override suspend fun refreshUser() = Unit
            override suspend fun signInWithGoogle(activity: Activity) = error("Offline fixture only")
            override suspend fun deleteAccount() = error("Offline fixture only")
            override suspend fun signOut(context: Context) = Unit
        }
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(AppRepository(context), CreditReportParser(context), CreditAgentGateway("", session), session, context) as T
        }
        return ViewModelProvider(store, factory)[MainViewModel::class.java].also { vm = it }
    }
    @After fun cleanup() { compose.runOnIdle { vm?.clearData(); store.clear(); AppRepository(context).clear(preserveOwner = false) } }
    @Test fun filingRequiresReviewAndAnExplicitReceiptThenTracksResponse() {
        lateinit var viewModel: MainViewModel
        compose.runOnIdle {
            viewModel = model(); viewModel.acceptPolicies(true, true); viewModel.completeOnboarding(); viewModel.loadDemo()
            viewModel.createDispute(viewModel.uiState.value.data.findings.first())
        }
        compose.setContent { CreditRepairTheme { val ui by viewModel.uiState.collectAsState(); FilingScreen(ui, viewModel, FilingPortal.EQUIFAX) } }
        compose.onNodeWithText("Copy Item / furnisher 1/1").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("I reviewed the facts against my records").performScrollTo().performClick()
        compose.onNodeWithText("Copy Item / furnisher 1/1").performScrollTo().assertIsEnabled()
        repeat(6) { compose.onNodeWithText("Next step").performScrollTo().performClick() }
        compose.onNodeWithText("I submitted it myself — save receipt").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Actual confirmation number after you submit").performScrollTo().performTextInput("Fictional-device-receipt")
        compose.onNodeWithText("I submitted it myself — save receipt").performScrollTo().performClick()
        compose.runOnIdle {
            val record = viewModel.uiState.value.data.filings.single()
            assertEquals("Fictional-device-receipt", record.confirmation)
            assertNotNull(record.reminder30); assertNotNull(record.reminder45)
            assertEquals(DisputeStatus.SENT, viewModel.uiState.value.data.disputes.single().status)
        }
        compose.onNodeWithText("I received a company response").performScrollTo().performClick()
        compose.runOnIdle { assertNotNull(viewModel.uiState.value.data.filings.single().responseReceivedAt) }
    }
    @Test fun applyingAiDraftRequiresReplacingPlaceholdersAndReviewingBeforeCopy() {
        lateinit var viewModel: MainViewModel
        compose.runOnIdle {
            viewModel = model(); viewModel.acceptPolicies(true, true); viewModel.completeOnboarding(); viewModel.loadDemo()
            viewModel.createDispute(viewModel.uiState.value.data.findings.first { com.creditrepairai.v2.model.Bureau.EQUIFAX in it.bureaus })
            val dispute = viewModel.uiState.value.data.disputes.single()
            val record = FilingRecord(disputeId = dispute.id, portal = FilingPortal.EQUIFAX)
            val draft = FilingAnswerDraft(FilingGuide.contextKey(dispute, record), "Fictional drafted explanation", "[NOT APPLICABLE]", "Please investigate the reported balance", "[ADD SUPPORTING RECORDS]")
            viewModel.saveFiling(record.copy(answerDraft = draft))
        }
        compose.setContent { CreditRepairTheme { val ui by viewModel.uiState.collectAsState(); FilingScreen(ui, viewModel, FilingPortal.EQUIFAX) } }
        compose.onNodeWithText("Use these drafts in my editable fields").performScrollTo().performClick()
        compose.runOnIdle {
            val record = viewModel.uiState.value.data.filings.single()
            assertEquals("Fictional drafted explanation", record.problem)
            assertFalse(record.reviewed); assertNull(record.answerDraft)
        }
        compose.onNodeWithText("I reviewed the facts against my records").performScrollTo().performClick()
        compose.runOnIdle { assertFalse(viewModel.uiState.value.data.filings.single().reviewed) }
        compose.onNodeWithText("Supporting evidence checklist").performScrollTo().performTextReplacement("Fictional dated statement")
        compose.onNodeWithText("I reviewed the facts against my records").performScrollTo().performClick()
        compose.onNodeWithText("Copy Item / furnisher 1/1").performScrollTo().assertIsEnabled()
    }
    @Test fun staleEditorCannotEraseAReceiptOrResponseAndRepeatedSubmissionKeepsDates() {
        compose.runOnIdle {
            val model = model(); model.loadDemo(); model.createDispute(model.uiState.value.data.findings.first())
            val dispute = model.uiState.value.data.disputes.single()
            val draft = FilingRecord(disputeId = dispute.id, portal = FilingPortal.EQUIFAX, step = 6, reviewed = true)
            model.saveFiling(draft)
            model.recordSubmission(draft, "Fictional-original-receipt")
            val submitted = model.uiState.value.data.filings.single()
            assertNotNull(submitted.submittedAt)
            model.recordSubmission(submitted, "Fictional-repeat-receipt")
            assertEquals(submitted, model.uiState.value.data.filings.single())
            model.recordResponse(submitted)
            val responded = model.uiState.value.data.filings.single()
            assertNotNull(responded.responseReceivedAt)
            model.saveFiling(draft.copy(problem = "Edited after sending", reviewed = false))
            val edited = model.uiState.value.data.filings.single()
            assertEquals("Fictional-original-receipt", edited.confirmation)
            assertEquals(responded.submittedAt, edited.submittedAt)
            assertEquals(responded.responseReceivedAt, edited.responseReceivedAt)
            assertEquals(responded.reminder45, edited.reminder45)
            assertEquals("Edited after sending", AppRepository(context).load().filings.single().problem)
        }
    }
    @Test fun editedOrDeletedFilingCannotUseAnOldReviewToRecordSubmission() {
        compose.runOnIdle {
            val model = model(); model.loadDemo(); model.createDispute(model.uiState.value.data.findings.first())
            val dispute = model.uiState.value.data.disputes.single()
            val reviewed = FilingRecord(disputeId = dispute.id, portal = FilingPortal.EQUIFAX, step = 6, reviewed = true)
            model.saveFiling(reviewed)
            model.saveFiling(reviewed.copy(problem = "New facts", reviewed = false))
            model.recordSubmission(reviewed, "Fictional-stale-receipt")
            assertNull(model.uiState.value.data.filings.single().submittedAt)
            model.recordResponse(reviewed)
            assertNull(model.uiState.value.data.filings.single().responseReceivedAt)
            model.deleteDispute(dispute.id)
            model.saveFiling(reviewed)
            model.recordSubmission(reviewed, "Fictional-deleted-receipt")
            assertTrue(model.uiState.value.data.filings.isEmpty())
        }
    }
    @Test fun mainWorkflowRequiresAgreementOnboardingAndKeepsSafeDemoNavigable() {
        lateinit var viewModel: MainViewModel
        compose.runOnIdle { viewModel = model() }
        compose.setContent { CreditRepairTheme { CreditRepairApp(viewModel) } }
        compose.onNodeWithText("I agree — continue").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("I agree to the displayed Terms of Use and acknowledge the Privacy Policy and Educational Disclaimer").performScrollTo().performClick()
        compose.onNodeWithText("I am at least 18 and will use records I own or have lawful permission to use").performScrollTo().performClick()
        compose.onNodeWithText("I agree — continue").performScrollTo().performClick()
        repeat(4) { compose.onNodeWithText("Continue").performScrollTo().performClick() }
        compose.onNodeWithText("I understand — continue").performScrollTo().performClick()
        compose.onNode(hasText("Reports") and hasClickAction()).performClick()
        compose.onNodeWithText("Select PDF reports").performScrollTo().assertIsEnabled()
        compose.onNode(hasText("Home") and hasClickAction()).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Load safe demo"))
        compose.onNodeWithText("Load safe demo").performClick()
        compose.runOnIdle {
            assertEquals(3, viewModel.uiState.value.data.reports.size)
            assertTrue(viewModel.uiState.value.data.findings.isNotEmpty())
            assertTrue(viewModel.uiState.value.data.policyAcceptance!!.isCurrent)
            assertFalse(viewModel.uiState.value.data.policyAcceptance!!.allowsAi)
        }
        compose.onNode(hasText("Reports") and hasClickAction()).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Demo-TransUnion.pdf"))
        compose.onNodeWithText("Demo-TransUnion.pdf").assertIsDisplayed()
        compose.onNode(hasText("Analysis") and hasClickAction()).performClick()
        compose.onNodeWithText("Three-bureau analysis").assertIsDisplayed()
        compose.onNodeWithText("No reports imported").assertDoesNotExist()
    }

    @Test fun savedDisputeOffersTheSelectedBureauWalkthroughDirectly() {
        lateinit var journeyModel: MainViewModel
        lateinit var bureau: com.creditrepairai.v2.model.Bureau
        compose.runOnIdle {
            journeyModel = model(); journeyModel.acceptPolicies(true, true); journeyModel.completeOnboarding(); journeyModel.loadDemo()
            journeyModel.createDispute(journeyModel.uiState.value.data.findings.first())
            bureau = journeyModel.uiState.value.data.disputes.first().bureaus.first { it != com.creditrepairai.v2.model.Bureau.UNKNOWN }
        }
        compose.setContent { CreditRepairTheme { CreditRepairApp(journeyModel) } }
        compose.onNodeWithText("Disputes", substring = false).performClick()
        compose.onNodeWithText("${bureau.displayName} walkthrough").performScrollTo().performClick()
        compose.onRoot().printToLog("JourneyTree")
        compose.onNodeWithText("${bureau.displayName} dispute walkthrough").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Step 1 of 7: Review facts").assertIsDisplayed()
    }
    @Test fun physicalLetterExportRequiresSenderEvidenceAndReview() {
        lateinit var journeyModel: MainViewModel
        lateinit var bureau: com.creditrepairai.v2.model.Bureau
        compose.runOnIdle {
            journeyModel = model(); journeyModel.acceptPolicies(true, true); journeyModel.completeOnboarding(); journeyModel.loadDemo()
            journeyModel.createDispute(journeyModel.uiState.value.data.findings.first())
            bureau = journeyModel.uiState.value.data.disputes.first().bureaus.first { it != com.creditrepairai.v2.model.Bureau.UNKNOWN }
        }
        compose.setContent { CreditRepairTheme { CreditRepairApp(journeyModel) } }
        compose.onNodeWithText("Disputes", substring = false).performClick()
        compose.onNodeWithText("${bureau.displayName} print / mail letter").performScrollTo().performClick()
        compose.onRoot().printToLog("JourneyTree")
        compose.onNodeWithText("Save ${bureau.displayName} letter PDF").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Your full name — local only").performScrollTo().performTextInput("Fictional Reader")
        compose.onNodeWithText("Your return mailing address — local only").performScrollTo().performTextInput("123 Test Street, Test City WA 00000")
        compose.onNodeWithText("Copies I will enclose").performScrollTo().performTextInput("Copy of the dated fictional statement")
        compose.onNodeWithText("I verified every statement and the mailing requirements").performScrollTo().performClick()
        compose.onNodeWithText("Save ${bureau.displayName} letter PDF").performScrollTo().assertIsEnabled()
    }
}
