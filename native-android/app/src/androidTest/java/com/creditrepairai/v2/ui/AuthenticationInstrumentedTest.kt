package com.creditrepairai.v2.ui

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.creditrepairai.v2.MainViewModel
import com.creditrepairai.v2.ai.CreditAgentGateway
import com.creditrepairai.v2.analysis.CreditReportParser
import com.creditrepairai.v2.auth.AuthFailure
import com.creditrepairai.v2.auth.NativeSession
import com.creditrepairai.v2.data.AppRepository
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import com.creditrepairai.v2.guidance.CreditGoal
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** No real identity, outbound mail, or provider account is used in these tests. */
class AuthenticationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val store = ViewModelStore()
    private class Session(var signedIn: Boolean = false) : NativeSession {
        override val email get() = if (signedIn) "fictional@example.invalid" else null
        override val uid get() = if (signedIn) "fictional-auth-owner" else null
        override var isEmailVerified = false
        var resetRequests = 0
        var resendRequests = 0
        var refreshRequests = 0
        var refreshGate: CompletableDeferred<Unit>? = null
        override fun observeSession(onChange: (String?) -> Unit): () -> Unit = {}
        override suspend fun bearerToken(expectedSubject: String): String? = null
        override suspend fun signInWithEmail(email: String, password: String) { throw AuthFailure("ERROR_OPERATION_NOT_ALLOWED") }
        override suspend fun createAccount(email: String, password: String) { error("Offline fixture only") }
        override suspend fun sendPasswordReset(email: String) { resetRequests++ }
        override suspend fun resendVerification() { resendRequests++ }
        override suspend fun refreshUser() { refreshRequests++; refreshGate?.await() }
        override suspend fun signInWithGoogle(activity: Activity) { throw AuthFailure("GOOGLE_CANCELED") }
        override suspend fun deleteAccount() { error("Offline fixture only") }
        override suspend fun signOut(context: Context) { signedIn = false }
    }
    private fun model(session: Session): MainViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(AppRepository(context), CreditReportParser(context), CreditAgentGateway("", session), session, context) as T
        }
        return ViewModelProvider(store, factory)[MainViewModel::class.java]
    }
    @After fun cleanup() { compose.runOnIdle { store.clear(); AppRepository(context).clear(preserveOwner = false) } }

    @Test fun emailLoginDisplaysAnActionableProviderError() {
        val session = Session()
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session) }
        compose.runOnIdle { vm.signInWithEmail("fictional@example.invalid", "fictional-password", false) }
        compose.waitUntil { vm.uiState.value.notice != null }
        assertTrue(vm.uiState.value.notice!!.contains("AUTH_PROVIDER_DISABLED"))
        assertFalse(vm.uiState.value.isAuthenticating)
        assertNull(vm.uiState.value.signedInEmail)
    }
    @Test fun passwordRecoveryRequiresTheExplicitButtonAndNoPassword() {
        val session = Session()
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session) }
        compose.setContent { CreditRepairTheme { val ui by vm.uiState.collectAsState(); AuthenticationScreen(ui, vm) {} } }
        compose.runOnIdle { assertEquals(0, session.resetRequests) }
        compose.onNodeWithText("Forgot password? Send reset email").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Email", substring = false).performScrollTo().performTextInput("fictional@example.invalid")
        compose.onNodeWithText("Forgot password? Send reset email").performScrollTo().assertIsEnabled().performClick()
        compose.waitUntil { session.resetRequests == 1 && !vm.uiState.value.isAuthenticating }
        assertTrue(vm.uiState.value.notice!!.startsWith("If this email has a password account"))
    }
    @Test fun verificationRefreshUpdatesStateWithoutSendingAnotherEmail() {
        val session = Session(signedIn = true)
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session); session.isEmailVerified = true; vm.checkEmailVerification() }
        compose.waitUntil { vm.uiState.value.emailVerified }
        assertEquals(1, session.refreshRequests)
        assertEquals(0, session.resendRequests)
        assertFalse(vm.uiState.value.isAuthenticating)
    }
    @Test fun verificationControlsDoNotSendEmailOnDisplay() {
        var checks = 0; var resends = 0
        compose.setContent { CreditRepairTheme { AccountVerificationCard(false, false, { checks++ }, { resends++ }) } }
        compose.runOnIdle { assertEquals(0, checks); assertEquals(0, resends) }
        compose.onNodeWithText("I've verified my email — check again").performClick()
        compose.runOnIdle { assertEquals(1, checks); assertEquals(0, resends) }
        compose.onNodeWithText("Resend verification email").performClick()
        compose.runOnIdle { assertEquals(1, resends) }
    }
    @Test fun clearingTheCaseWhileVerificationWaitsAllowsRetryAndRejectsTheOldReply() {
        val session = Session(signedIn = true)
        val gate = CompletableDeferred<Unit>()
        session.refreshGate = gate
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session); vm.checkEmailVerification() }
        compose.waitUntil { vm.uiState.value.isAuthenticating }
        compose.runOnIdle {
            vm.clearData()
            assertFalse(vm.uiState.value.isAuthenticating)
            session.isEmailVerified = true
            gate.complete(Unit)
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(vm.uiState.value.emailVerified)
            session.refreshGate = null
            vm.checkEmailVerification()
        }
        compose.waitUntil { vm.uiState.value.emailVerified }
        assertEquals(2, session.refreshRequests)
    }
    @Test fun unavailableAgentKeepsThePreviouslySavedPlan() {
        val session = Session(signedIn = true).apply { isEmailVerified = true }
        lateinit var vm: MainViewModel
        compose.runOnIdle {
            vm = model(session); vm.acceptPolicies(true, true); vm.completeOnboarding(); vm.setAiSharing(true)
            vm.saveGoal(CreditGoal("Fictional goal", 12, "Previously reviewed plan"))
            vm.buildGoalPlan()
        }
        compose.waitUntil { !vm.uiState.value.isAssistantThinking && vm.uiState.value.data.chat.last().text.startsWith("I could not use") }
        compose.runOnIdle {
            assertEquals("Previously reviewed plan", vm.uiState.value.data.goal.aiPlan)
            assertEquals("Previously reviewed plan", AppRepository(context).load().goal.aiPlan)
        }
    }
}
