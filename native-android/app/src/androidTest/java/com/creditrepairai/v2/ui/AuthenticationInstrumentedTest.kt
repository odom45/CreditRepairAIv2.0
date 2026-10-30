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
        var allowSignIn = false
        var loginGate: CompletableDeferred<Unit>? = null
        var resetRequests = 0
        var resendRequests = 0
        var refreshRequests = 0
        var refreshGate: CompletableDeferred<Unit>? = null
        override fun observeSession(onChange: (String?) -> Unit): () -> Unit = {}
        override suspend fun bearerToken(expectedSubject: String): String? = null
        override suspend fun signInWithEmail(email: String, password: String) {
            if (!allowSignIn) throw AuthFailure("ERROR_OPERATION_NOT_ALLOWED")
            loginGate?.await()
            signedIn = true
        }
        override suspend fun createAccount(email: String, password: String) { error("Offline fixture only") }
        override suspend fun sendPasswordReset(email: String) { resetRequests++ }
        override suspend fun resendVerification() { resendRequests++ }
        override suspend fun refreshUser() { refreshRequests++; refreshGate?.await() }
        override suspend fun signInWithGoogle(activity: Activity) { throw AuthFailure("GOOGLE_CANCELED") }
        override suspend fun deleteAccount() { error("Offline fixture only") }
        override suspend fun signOut(context: Context) { signedIn = false }
    }
    private fun model(session: Session, freshSignIn: Boolean = false): MainViewModel {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(AppRepository(context), CreditReportParser(context), CreditAgentGateway("", session), session, context, requireFreshSignInOnStart = freshSignIn) as T
        }
        return ViewModelProvider(store, factory)[MainViewModel::class.java]
    }
    @After fun cleanup() { compose.runOnIdle { store.clear(); AppRepository(context).clear(preserveOwner = false) } }


    @Test fun cachedIdentityCannotOpenCaseUntilFreshLoginAndLockPreservesEncryptedData() {
        val session = Session(true).apply { isEmailVerified = true; allowSignIn = true }
        lateinit var vm: MainViewModel
        compose.runOnIdle {
            val repository = AppRepository(context)
            repository.selectOwner(session.uid)
            repository.save(com.creditrepairai.v2.model.AppState(onboardingComplete = true))
            vm = model(session, freshSignIn = true)
            assertTrue(vm.isSessionLocked)
            assertNull(vm.uiState.value.signedInEmail)
            assertNull(vm.signedInSubject)
            assertFalse(vm.uiState.value.data.onboardingComplete)
            assertFalse(vm.askAssistant("Review my case"))
            assertTrue(repository.load().onboardingComplete)
            vm.signInWithEmail("fictional@example.invalid", "fictional-password", false)
        }
        compose.waitUntil { vm.uiState.value.signedInEmail != null }
        compose.runOnIdle {
            assertFalse(vm.isSessionLocked)
            assertTrue(vm.uiState.value.data.onboardingComplete)
            vm.lockSession()
            assertTrue(vm.isSessionLocked)
            assertNull(vm.signedInSubject)
            assertFalse(vm.uiState.value.data.onboardingComplete)
            assertTrue(AppRepository(context).load().onboardingComplete)
        }
    }

    @Test fun lateLoginAndVerificationCannotUnlockABackgroundedCase() {
        val gate = CompletableDeferred<Unit>()
        val session = Session(true).apply { isEmailVerified = true; allowSignIn = true; loginGate = gate }
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session, freshSignIn = true); vm.signInWithEmail("fictional@example.invalid", "fictional-password", false) }
        compose.waitUntil { vm.uiState.value.isAuthenticating }
        compose.runOnIdle { vm.lockSession(); gate.complete(Unit) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(vm.isSessionLocked)
            assertNull(vm.uiState.value.signedInEmail)
            vm.checkEmailVerification(quiet = true)
            assertEquals(0, session.refreshRequests)
            assertNull(vm.signedInSubject)
        }
    }

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

    @Test fun restoredUnverifiedAccountShowsVerificationBeforeAnyReportTools() {
        val session = Session(signedIn = true)
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session); vm.acceptPolicies(true, true); vm.completeOnboarding() }
        compose.setContent { CreditRepairTheme { CreditRepairApp(vm) } }
        compose.onNodeWithText("Verify your signed-in account").assertIsDisplayed()
        compose.onNodeWithText("Signed in as fictional@example.invalid").assertIsDisplayed()
        compose.onNodeWithText("Reports", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Resend verification email").performScrollTo().performClick()
        compose.waitUntil { session.resendRequests == 1 && !vm.uiState.value.isAuthenticating }
        compose.runOnIdle { session.isEmailVerified = true }
        compose.onNodeWithText("I've verified my email — check again").performScrollTo().performClick()
        compose.waitUntil { vm.uiState.value.emailVerified }
        compose.onNodeWithText("Account", substring = false).assertIsDisplayed()
    }
    @Test fun accountAndSignOutAreVisibleAndRequireConfirmation() {
        val session = Session(signedIn = true).apply { isEmailVerified = true }
        lateinit var vm: MainViewModel
        compose.runOnIdle { vm = model(session); vm.acceptPolicies(true, true); vm.completeOnboarding() }
        compose.setContent { CreditRepairTheme { CreditRepairApp(vm) } }
        compose.onNodeWithText("Account", substring = false).performClick()
        compose.onNodeWithText("Signed in as fictional@example.invalid").assertIsDisplayed()
        compose.onNodeWithText("Sign out", substring = false).performClick()
        compose.onNodeWithText("Keep working").performClick()
        compose.runOnIdle { assertTrue(session.signedIn) }
        compose.onNodeWithText("Sign out", substring = false).performClick()
        compose.onNodeWithText("Sign out and remove local case").performClick()
        compose.waitUntil { !session.signedIn && vm.uiState.value.signedInEmail == null }
        compose.onNodeWithText("Welcome to CreditRepairAI").assertIsDisplayed()
    }
}
