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
            override fun observeSession(onChange: (String?) -> Unit): () -> Unit = {}
            override suspend fun bearerToken(expectedSubject: String): String? = null
            override suspend fun signInWithEmail(email: String, password: String) = error("Offline fixture only")
            override suspend fun createAccount(email: String, password: String) = error("Offline fixture only")
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
}
