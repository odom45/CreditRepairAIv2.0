package com.creditrepairai.v2

import android.content.Context
import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.creditrepairai.v2.ai.CreditAgentGateway
import com.creditrepairai.v2.ai.SensitiveDataRedactor
import com.creditrepairai.v2.analysis.CreditAnalyzer
import com.creditrepairai.v2.analysis.CreditReportParser
import com.creditrepairai.v2.data.AppRepository
import com.creditrepairai.v2.auth.FirebaseSession
import com.creditrepairai.v2.auth.NativeSession
import com.creditrepairai.v2.auth.SessionFence
import com.creditrepairai.v2.auth.AuthFeedback
import com.creditrepairai.v2.legal.DisputeLetterGenerator
import com.creditrepairai.v2.model.AppState
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.ChatMessage
import com.creditrepairai.v2.model.CreditAccount
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.DisputeStatus
import com.creditrepairai.v2.model.Finding
import com.creditrepairai.v2.model.ScoreSnapshot
import com.creditrepairai.v2.model.UsJurisdictions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.creditrepairai.v2.guidance.*
import com.creditrepairai.v2.billing.PlayBillingManager
import com.creditrepairai.v2.ai.SecureService
import com.creditrepairai.v2.legal.PolicyAcceptance

sealed interface PendingAiRequest {
    data class Chat(val prompt: String, val saveAsPlan: Boolean, val caseScopeId: String? = null) : PendingAiRequest
    data class Filing(val record: FilingRecord, val draftAnswers: Boolean = false) : PendingAiRequest
}

data class FilingHelpStatus(
    val recordId: String = "", val step: Int = -1, val contextKey: String = "",
    val thinking: Boolean = false, val error: String? = null,
)

data class AppUiState(
    val data: AppState = AppState(),
    val isProcessing: Boolean = false,
    val progressText: String = "",
    val notice: String? = null,
    val isAssistantThinking: Boolean = false,
    val isAgentEndpointConfigured: Boolean = false,
    val signedInEmail: String? = null,
    val emailVerified: Boolean = false,
    val isAuthenticating: Boolean = false,
    val filingHelp: FilingHelpStatus = FilingHelpStatus(),
    val showAiDisclosure: Boolean = false,
    val pendingAiRequest: PendingAiRequest? = null,
)

class MainViewModel(
    private val repository: AppRepository,
    private val parser: CreditReportParser,
    private val agentGateway: CreditAgentGateway,
    private val session: NativeSession,
    context: Context,
) : ViewModel() {
    private val context: android.app.Application = context.applicationContext as android.app.Application
    private val caseFence = SessionFence(session.uid)
    private var aiJob: Job? = null
    private var filingHelpJob: Job? = null
    private var activeImportJob: Job? = null
    init { repository.selectOwner(session.uid) }
    val service = SecureService(BuildConfig.AI_GATEWAY_URL, session) { _uiState.value.data.policyAcceptance }
    val signedInSubject: String? get() = session.uid
    val billing = PlayBillingManager(context, service) { session.uid }
    private val importLock = Mutex()
    private val _uiState = MutableStateFlow(
        AppUiState(
            data = repository.load(),
            isAgentEndpointConfigured = agentGateway.isConfigured,
            signedInEmail = session.email,
            emailVerified = session.isEmailVerified,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private val stopObservingSession = session.observeSession { owner ->
        if (caseFence.ticket().owner != owner) {
            invalidateCase(owner)
            repository.selectOwner(owner)
            billing.reset()
            _uiState.value = AppUiState(data = repository.load(), signedInEmail = session.email, emailVerified = session.isEmailVerified, isAgentEndpointConfigured = agentGateway.isConfigured)
        }
    }

    private fun invalidateCase(owner: String? = session.uid) {
        caseFence.invalidate(owner)
        aiJob?.cancel()
        filingHelpJob?.cancel()
        _uiState.update { it.copy(filingHelp = FilingHelpStatus(), showAiDisclosure = false, pendingAiRequest = null) }
        activeImportJob?.cancel()
        FollowUpWorker.cancelAll(context)
    }

    fun signInWithEmail(email: String, password: String, createAccount: Boolean) {
        if (email.isBlank() || password.isEmpty() || (createAccount && password.length < 6) || _uiState.value.isAuthenticating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, notice = null) }
            runCatching {
                if (createAccount) session.createAccount(email, password) else session.signInWithEmail(email, password)
            }.onSuccess {
                repository.selectOwner(session.uid)
                _uiState.update {
                    it.copy(
                        data = repository.load(),
                        signedInEmail = session.email,
                        emailVerified = session.isEmailVerified,
                        isAuthenticating = false,
                        notice = if (createAccount) "Account created. Check your email to verify it." else "Signed in securely.",
                    )
                }
                billing.refresh()
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update { state ->
                    state.copy(isAuthenticating = false, signedInEmail = session.email, emailVerified = session.isEmailVerified, notice = AuthFeedback.failureMessage(error))
                }
            }
        }
    }

    fun signInWithGoogle(activity: Activity) {
        if (_uiState.value.isAuthenticating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, notice = null) }
            runCatching { session.signInWithGoogle(activity) }
                .onSuccess {
                    repository.selectOwner(session.uid)
                    _uiState.update { it.copy(data = repository.load(), signedInEmail = session.email, emailVerified = session.isEmailVerified, isAuthenticating = false, notice = "Signed in with Google.") }
                    billing.refresh()
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _uiState.update { it.copy(isAuthenticating = false, notice = AuthFeedback.failureMessage(error)) }
                }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank() || _uiState.value.isAuthenticating) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, notice = null) }
            try {
                session.sendPasswordReset(email)
                _uiState.update { it.copy(notice = "If this email has a password account, a reset email has been requested. Check your inbox and spam folder, then return to sign in.") }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(notice = AuthFeedback.failureMessage(error)) }
            } finally { _uiState.update { it.copy(isAuthenticating = false) } }
        }
    }

    fun checkEmailVerification(resend: Boolean = false) {
        if (session.uid == null || _uiState.value.isAuthenticating) return
        val ticket = caseFence.ticket()
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, notice = null) }
            try {
                session.refreshUser()
                if (!caseFence.accepts(ticket, session.uid)) return@launch
                if (resend && !session.isEmailVerified) session.resendVerification()
                if (!caseFence.accepts(ticket, session.uid)) return@launch
                _uiState.update { it.copy(emailVerified = session.isEmailVerified, notice = when {
                    session.isEmailVerified -> "Email verified. Your sign-in token has been refreshed."
                    resend -> "Verification email requested. Open its link, then tap I've verified my email."
                    else -> "Email is not verified yet. Open the verification link in your inbox, then check again."
                }) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (caseFence.accepts(ticket, session.uid)) _uiState.update { it.copy(notice = AuthFeedback.failureMessage(error)) }
            } finally {
                if (caseFence.accepts(ticket, session.uid)) _uiState.update { it.copy(isAuthenticating = false) }
            }
        }
    }

    fun signOut(context: Context) {
        invalidateCase(null)
        billing.reset()
        repository.clear(preserveOwner = false)
        _uiState.value = AppUiState(isAgentEndpointConfigured = agentGateway.isConfigured)
        viewModelScope.launch {
            session.signOut(context)
            if (session.uid != null) return@launch
            _uiState.value = AppUiState(
                data = AppState(),
                notice = "Signed out and removed local case data from this device.",
                isAgentEndpointConfigured = agentGateway.isConfigured,
            )
        }
    }

    fun importPdf(uri: Uri, displayName: String) {
        if (_uiState.value.data.policyAcceptance?.isCurrent != true || !_uiState.value.data.onboardingComplete) return
        val owner = session.uid ?: return
        val ticket = caseFence.ticket()
        if (uri.scheme != "content") return
        viewModelScope.launch {
            importLock.withLock {
            if (!caseFence.accepts(ticket, session.uid)) return@withLock
            activeImportJob = coroutineContext[Job]
            _uiState.update { it.copy(isProcessing = true, progressText = "Reading $displayName…", notice = null) }
            runCatching { parser.parse(uri, displayName) }
                .onSuccess { parsed ->
                    if (!caseFence.accepts(ticket, session.uid)) return@onSuccess
                    mutate("Imported ${parsed.report.bureau.displayName} report: ${parsed.accounts.size} account entries found using ${parsed.report.extractionMethod}.") { state ->
                        val priorReportIds = state.reports.filter { it.fileName == displayName }.map { it.id }.toSet()
                        val retainedReports = state.reports.filterNot { it.id in priorReportIds }
                        val retainedAccounts = state.accounts.filterNot { account ->
                            priorReportIds.any { oldId -> account.id.startsWith("$oldId:") }
                        }
                        val namespacedAccounts = parsed.accounts.map { it.copy(id = "${parsed.report.id}:${it.id}") }
                        val reports = retainedReports + parsed.report
                        val accounts = retainedAccounts + namespacedAccounts
                        state.copy(
                            reports = reports,
                            accounts = accounts,
                            findings = CreditAnalyzer.analyze(accounts, reports),
                            scores = parsed.report.score?.let { score ->
                                state.scores + ScoreSnapshot(parsed.report.bureau, score)
                            } ?: state.scores,
                            acquisition = state.acquisition.capture(parsed.report.bureau),
                        )
                    }
                    _uiState.update { it.copy(isProcessing = false, progressText = "") }
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    if (!caseFence.accepts(ticket, session.uid)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progressText = "",
                            notice = "Could not import $displayName: ${error.message ?: "unreadable PDF"}",
                        )
                    }
                }
            activeImportJob = null
            }
        }
    }

    fun loadDemo() {
        invalidateCase()
        _uiState.update { it.copy(isProcessing = false, isAssistantThinking = false) }
        val reports = listOf(
            CreditReport(fileName = "Demo-TransUnion.pdf", bureau = Bureau.TRANSUNION, score = 618, accountCount = 3, extractionMethod = "Safe demo"),
            CreditReport(fileName = "Demo-Experian.pdf", bureau = Bureau.EXPERIAN, score = 603, accountCount = 3, extractionMethod = "Safe demo"),
            CreditReport(fileName = "Demo-Equifax.pdf", bureau = Bureau.EQUIFAX, score = 611, accountCount = 2, extractionMethod = "Safe demo"),
        )
        val accounts = listOf(
            CreditAccount(creditor = "Northstar Bank", accountSuffix = "2201", bureau = Bureau.TRANSUNION, status = "Open", balance = 1280),
            CreditAccount(creditor = "Northstar Bank", accountSuffix = "2201", bureau = Bureau.EXPERIAN, status = "Open", balance = 680),
            CreditAccount(creditor = "Northstar Bank", accountSuffix = "2201", bureau = Bureau.EQUIFAX, status = "Open", balance = 680),
            CreditAccount(creditor = "Cedar Collections", accountSuffix = "1198", bureau = Bureau.TRANSUNION, status = "Collection", balance = 466),
            CreditAccount(creditor = "Cedar Collections", accountSuffix = "1198", bureau = Bureau.EXPERIAN, status = "Charge-off", balance = 466),
            CreditAccount(creditor = "Sound Credit Union", accountSuffix = "8834", bureau = Bureau.TRANSUNION, status = "Paid", balance = 0),
            CreditAccount(creditor = "Sound Credit Union", accountSuffix = "8834", bureau = Bureau.EXPERIAN, status = "Paid", balance = 0),
            CreditAccount(creditor = "Sound Credit Union", accountSuffix = "8834", bureau = Bureau.EQUIFAX, status = "Paid", balance = 0),
        )
        val findings = CreditAnalyzer.analyze(accounts, reports)
        mutate("Safe demo loaded. No real personal data is included.") {
            AppState(
                onboardingComplete = _uiState.value.data.onboardingComplete,
                policyAcceptance = _uiState.value.data.policyAcceptance,
                jurisdictionCode = _uiState.value.data.jurisdictionCode,
                reports = reports,
                accounts = accounts,
                findings = findings,
                scores = reports.mapNotNull { report -> report.score?.let { ScoreSnapshot(report.bureau, it) } },
            )
        }
    }

    fun createDispute(finding: Finding) {
        mutate("Draft dispute created. Review every fact and replace all placeholders before sending.") { state ->
            if (state.disputes.any { it.findingId == finding.id }) state
            else state.copy(disputes = state.disputes + DisputeLetterGenerator.fromFinding(finding))
        }
    }

    fun updateDispute(id: String, status: DisputeStatus) {
        mutate("Dispute status updated to ${status.name.lowercase()}.") { state ->
            val now = System.currentTimeMillis()
            state.copy(disputes = state.disputes.map { dispute ->
                if (dispute.id != id) dispute
                else if (status == DisputeStatus.SENT && dispute.sentAt == null) {
                    dispute.copy(
                        status = status,
                        sentAt = now,
                        // Conservative readiness window: CFPB says to first dispute
                        // with the reporting company and not file while that dispute
                        // remains pending; its current intake notice also references 45 days.
                        craResponseDueAt = now + 45L * 24 * 60 * 60 * 1000,
                    )
                } else dispute.copy(status = status)
            })
        }
    }

    fun deleteDispute(id: String) {
        FollowUpWorker.cancelAll(context)
        mutate("Dispute removed from this device.") { state ->
            state.copy(disputes = state.disputes.filterNot { it.id == id }, filings = state.filings.filterNot { it.disputeId == id })
        }
        rescheduleCheckins()
    }

    fun recordScore(bureau: Bureau, score: Int) {
        if (score !in 300..850 || bureau == Bureau.UNKNOWN) {
            _uiState.update { it.copy(notice = "Enter a score from 300 to 850 and choose a bureau.") }
            return
        }
        mutate("${bureau.displayName} score recorded.") { state ->
            state.copy(scores = state.scores + ScoreSnapshot(bureau, score))
        }
    }

    fun toggleFreeze(name: String) {
        mutate("Freeze checklist updated. Confirm the actual status with the reporting company.") { state ->
            state.copy(
                frozenBureaus = if (name in state.frozenBureaus) state.frozenBureaus - name else state.frozenBureaus + name,
            )
        }
    }

    fun updateJurisdiction(value: String) {
        val code = UsJurisdictions.normalize(value)
        if (code == null) {
            _uiState.update { it.copy(notice = "Enter a valid two-letter U.S. state or DC code.") }
            return
        }
        mutate(
            "State context set to ${UsJurisdictions.names.getValue(code)}. State-specific law remains disabled until its official provisions are verified.",
        ) { state ->
            state.copy(jurisdictionCode = code)
        }
    }

    fun askAssistant(prompt: String, saveAsPlan: Boolean = false, caseScopeId: String? = null): Boolean {
        val owner = session.uid ?: return false
        val ticket = caseFence.ticket()
        if (!caseFence.accepts(ticket, owner)) return false
        val cleaned = SensitiveDataRedactor.redact(prompt.trim())
        if (cleaned.isBlank()) return false
        if (_uiState.value.isAssistantThinking) return false
        if (!allowAiOrDisclose(PendingAiRequest.Chat(cleaned, saveAsPlan, caseScopeId))) return false
        if (BuildConfig.PAID_ACCESS_ENABLED && !(if (caseScopeId != null && !saveAsPlan) billing.ui.value.entitlement.hasCaseAccess(owner, caseScopeId, System.currentTimeMillis()) else billing.ui.value.entitlement.hasAccess(System.currentTimeMillis()))) {
            _uiState.update { it.copy(notice = "Choose a purchased dispute for case AI, or a monthly/yearly subscription for all-item assistance. Manage access in Settings.") }
            return false
        }

        val stateWithQuestion = _uiState.value.data.copy(
            chat = (_uiState.value.data.chat + ChatMessage(fromUser = true, text = cleaned)).takeLast(40),
        )
        if (runCatching { repository.save(stateWithQuestion) }.isFailure) {
            _uiState.update { it.copy(notice = "Encrypted storage could not save the question. No case data was sent.") }; return false
        }
        _uiState.update { it.copy(data = stateWithQuestion, isAssistantThinking = true, notice = null) }

        aiJob = viewModelScope.launch {
            val answer = runCatching {
                service.syncPolicy(owner, requireNotNull(stateWithQuestion.policyAcceptance))
                if (!caseFence.accepts(ticket, session.uid) || _uiState.value.data.policyAcceptance?.allowsAi != true) throw CancellationException()
                agentGateway.answer(cleaned, stateWithQuestion, owner, caseScopeId)
            }
                .getOrElse { error ->
                    if (error is CancellationException) throw error
                    "I could not use the secure legal agent: ${error.message ?: "service unavailable"}"
                }
            if (!caseFence.accepts(ticket, session.uid) || _uiState.value.data.policyAcceptance?.allowsAi != true) return@launch
            val current = _uiState.value.data
            val updated = current.copy(
                chat = (current.chat + ChatMessage(fromUser = false, text = answer)).takeLast(40),
                goal = if (saveAsPlan && current.goal.description == stateWithQuestion.goal.description && current.goal.targetMonths == stateWithQuestion.goal.targetMonths) current.goal.copy(aiPlan = answer) else current.goal,
            )
            if (runCatching { repository.save(updated) }.isFailure) {
                _uiState.update { it.copy(isAssistantThinking = false, notice = "Encrypted storage could not save the answer. Retry after checking device storage.") }; return@launch
            }
            _uiState.update { it.copy(data = updated, isAssistantThinking = false) }
        }
        return true
    }

    fun clearData() {
        invalidateCase()
        if (runCatching { repository.clear() }.isFailure) {
            _uiState.update { it.copy(isProcessing = false, isAssistantThinking = false, notice = "Local deletion could not finish. Retry before treating the data as deleted.") }; return
        }
        val empty = AppState(onboardingComplete = true, policyAcceptance = _uiState.value.data.policyAcceptance)
        if (runCatching { repository.save(empty) }.isFailure) {
            _uiState.update { it.copy(data = AppState(), isProcessing = false, isAssistantThinking = false, notice = "Case data was cleared, but agreement choices could not be saved. Reopen the app and review them again.") }; return
        }
        _uiState.update { it.copy(
            data = empty,
            isAssistantThinking = false,
            isProcessing = false,
            progressText = "",
            notice = "All local case data was deleted and its Android Keystore encryption key was destroyed.",
            isAgentEndpointConfigured = agentGateway.isConfigured,
        ) }
    }

    fun completeOnboarding() = mutate(null) { it.copy(onboardingComplete = true) }
    fun acceptPolicies(agreed: Boolean, adult: Boolean) {
        val acceptance = runCatching { PolicyAcceptance.accept(agreed, adult, System.currentTimeMillis()) }.getOrNull() ?: return
        mutate(null) { it.copy(policyAcceptance = it.policyAcceptance?.takeIf { old -> old.isCurrent } ?: acceptance) }
    }
    private fun allowAiOrDisclose(request: PendingAiRequest): Boolean {
        if (!session.isEmailVerified) {
            _uiState.update { it.copy(notice = "Verify your email before using AI. Open Settings to resend the link or refresh verification after opening it.") }; return false
        }
        if (_uiState.value.data.policyAcceptance?.isCurrent != true) {
            _uiState.update { it.copy(notice = "Read and accept the current terms before using AI.") }; return false
        }
        if (_uiState.value.data.policyAcceptance?.allowsAi == true) return true
        _uiState.update { it.copy(showAiDisclosure = true, pendingAiRequest = request) }
        return false
    }
    fun dismissAiDisclosure() { _uiState.update { it.copy(showAiDisclosure = false, pendingAiRequest = null) } }
    fun reviewAiSharing() { _uiState.update { it.copy(showAiDisclosure = true, pendingAiRequest = null) } }
    fun setAiSharing(enabled: Boolean) {
        val prior = _uiState.value.data.policyAcceptance?.takeIf { it.isCurrent } ?: return
        val pending = _uiState.value.pendingAiRequest
        aiJob?.cancel(); filingHelpJob?.cancel()
        val choice = prior.chooseAi(enabled, System.currentTimeMillis())
        val updated = _uiState.value.data.copy(policyAcceptance = choice)
        if (runCatching { repository.save(updated) }.isFailure) {
            _uiState.update { it.copy(notice = "Encrypted storage could not save your choice. Retry before using AI.") }; return
        }
        _uiState.update { it.copy(data = updated, showAiDisclosure = false, pendingAiRequest = null,
            isAssistantThinking = false, filingHelp = FilingHelpStatus(), notice = if (!enabled) "AI sharing is disabled on this device. Local tools remain available." else null) }
        if (enabled && pending != null) when (pending) {
            is PendingAiRequest.Chat -> askAssistant(pending.prompt, pending.saveAsPlan, pending.caseScopeId)
            is PendingAiRequest.Filing -> explainFiling(pending.record, pending.draftAnswers)
        } else session.uid?.let { owner ->
            val ticket = caseFence.ticket()
            viewModelScope.launch { runCatching { service.syncPolicy(owner, choice) }.onFailure {
                if (it is CancellationException) throw it
                if (caseFence.accepts(ticket, session.uid)) _uiState.update { it.copy(notice = "Your choice is saved on this device. Server preference sync is unavailable; retry when connected. Already-sent provider data cannot be recalled.") }
            } }
        }
    }
    fun updateAcquisition(state: AcquisitionState) = mutate(null) { it.copy(acquisition = state) }
    fun saveFiling(record: FilingRecord) = mutate(null) { it.copy(filings = it.filings.filterNot { old -> old.id == record.id } + record) }
    fun draftFilingAnswers(record: FilingRecord) = explainFiling(record, draftAnswers = true)
    fun applyFilingAnswers(record: FilingRecord) {
        val current = _uiState.value.data.filings.firstOrNull { it.id == record.id } ?: return
        val dispute = _uiState.value.data.disputes.firstOrNull { it.id == current.disputeId } ?: return
        current.answerDraft?.apply(dispute, current)?.let(::saveFiling)
    }
    fun explainFiling(record: FilingRecord, draftAnswers: Boolean = false) {
        if (filingHelpJob?.isActive == true) return
        val owner = session.uid ?: return
        val ticket = caseFence.ticket()
        if (!caseFence.accepts(ticket, owner)) return
        if (!allowAiOrDisclose(PendingAiRequest.Filing(record, draftAnswers))) return
        // Save a new checklist before requesting help; never send an unsaved case.
        if (_uiState.value.data.filings.none { it.id == record.id }) saveFiling(record)
        val snapshot = _uiState.value.data
        val saved = snapshot.filings.firstOrNull { it.id == record.id } ?: return
        val dispute = snapshot.disputes.firstOrNull { it.id == saved.disputeId } ?: return
        val request = FilingGuide.request(dispute, saved)
        val status = FilingHelpStatus(saved.id, saved.step, request.contextKey)
        if (BuildConfig.PAID_ACCESS_ENABLED && !billing.ui.value.entitlement.hasCaseAccess(owner, saved.disputeId, System.currentTimeMillis())) {
            _uiState.update { it.copy(filingHelp = status.copy(error = "A verified subscription or access for this dispute is required. Open Settings to choose a plan.")) }
            return
        }
        _uiState.update { it.copy(filingHelp = status.copy(thinking = true)) }
        filingHelpJob = viewModelScope.launch {
            val answer = try {
                service.syncPolicy(owner, requireNotNull(snapshot.policyAcceptance))
                if (!caseFence.accepts(ticket, session.uid) || _uiState.value.data.policyAcceptance?.allowsAi != true) throw CancellationException()
                agentGateway.answer(SensitiveDataRedactor.redact(if (draftAnswers) FilingGuide.answerPrompt(dispute, saved) else request.prompt), snapshot, owner, saved.disputeId)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (caseFence.accepts(ticket, session.uid)) _uiState.update {
                    it.copy(filingHelp = status.copy(error = "The secure AI explanation is unavailable. Check your connection and verified sign-in, then retry. The saved checklist remains available."))
                }
                return@launch
            }
            if (!caseFence.accepts(ticket, session.uid) || _uiState.value.data.policyAcceptance?.allowsAi != true) return@launch
            val current = _uiState.value.data
            val currentRecord = current.filings.firstOrNull { it.id == request.recordId }
            val currentDispute = current.disputes.firstOrNull { it.id == saved.disputeId }
            val explained = if (currentRecord != null && currentDispute != null &&
                FilingGuide.contextKey(currentDispute, currentRecord) == request.contextKey) {
                if (draftAnswers) {
                    val draft = runCatching { FilingAnswerParser.parse(answer, request.contextKey) }.getOrElse {
                        _uiState.update { it.copy(filingHelp = status.copy(error = "AI draft fields were incomplete. Retry; your saved facts were kept.")) }
                        return@launch
                    }
                    currentRecord.copy(answerDraft = draft)
                } else request.apply(currentDispute, currentRecord, answer)
            } else null
            if (explained == null) {
                _uiState.update { it.copy(filingHelp = FilingHelpStatus(), notice = "The filing details changed. Ask again to explain the current step.") }
                return@launch
            }
            val updated = current.copy(filings = current.filings.map { if (it.id == explained.id) explained else it })
            if (runCatching { repository.save(updated) }.isFailure) {
                _uiState.update { it.copy(filingHelp = status.copy(error = "Encrypted storage could not save the explanation. Check device storage and retry.")) }
                return@launch
            }
            _uiState.update { it.copy(data = updated, filingHelp = status) }
        }
    }
    fun recordSubmission(record: FilingRecord, confirmation: String) {
        val saved = runCatching { FilingGuide.submitted(record, confirmation, System.currentTimeMillis()) }.getOrElse {
            _uiState.update { it.copy(notice = "Review the facts and enter the actual portal receipt before recording a submission.") }; return
        }
        mutate("Receipt saved. Check-in reminders are not promised response dates.") { state ->
            state.copy(filings = state.filings.filterNot { it.id == saved.id } + saved,
                disputes = state.disputes.map { if (it.id == saved.disputeId && saved.portal != FilingPortal.CFPB) it.copy(status = DisputeStatus.SENT, sentAt = saved.submittedAt, craResponseDueAt = saved.reminder45) else it })
        }
        rescheduleCheckins()
    }
    fun recordResponse(record: FilingRecord) {
        saveFiling(record.copy(responseReceivedAt = System.currentTimeMillis()))
        FollowUpWorker.cancelAll(context)
        rescheduleCheckins()
    }
    fun rescheduleCheckins() { _uiState.value.data.filings.forEach { record ->
        if (record.responseReceivedAt == null) {
            record.reminder30?.takeIf { it > System.currentTimeMillis() }?.let { FollowUpWorker.schedule(context, record.id, 30, it) }
            record.reminder45?.takeIf { it > System.currentTimeMillis() }?.let { FollowUpWorker.schedule(context, record.id, 45, it) }
        }
    } }
    fun saveGoal(goal: CreditGoal) = mutate(null) { it.copy(goal = goal.copy(description = SensitiveDataRedactor.redact(goal.description).take(1000), targetMonths = goal.targetMonths.coerceIn(1, 120))) }
    fun buildGoalPlan() {
        val goal = _uiState.value.data.goal
        if (goal.description.isBlank()) return
        askAssistant(GoalPlanner.prompt(goal), saveAsPlan = true)
    }
    fun deleteAccount() {
        val owner = session.uid ?: return
        val ticket = caseFence.ticket()
        viewModelScope.launch {
            runCatching {
                service.post("/account/delete", owner)
                if (!caseFence.accepts(ticket, session.uid)) return@runCatching
                session.deleteAccount()
                signOut(context)
            }.onFailure { if (it is CancellationException) throw it; if (caseFence.accepts(ticket, session.uid)) _uiState.update { it.copy(notice = "Account deletion could not finish. Sign in again for recent authentication, then retry. Deleting your account does not cancel Google Play billing.") } }
        }
    }
    override fun onCleared() { stopObservingSession(); billing.close(); super.onCleared() }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun mutate(notice: String?, transform: (AppState) -> AppState) {
        val updated = transform(_uiState.value.data)
        if (runCatching { repository.save(updated) }.isFailure) {
            _uiState.update { it.copy(notice = "Encrypted storage could not save this change. Check device storage and retry.") }; return
        }
        _uiState.update { it.copy(
            data = updated,
            notice = notice,
            isAgentEndpointConfigured = agentGateway.isConfigured,
        ) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val session = FirebaseSession()
                return MainViewModel(
                    repository = AppRepository(context),
                    parser = CreditReportParser(context),
                    agentGateway = CreditAgentGateway(
                        endpoint = BuildConfig.AI_GATEWAY_URL,
                        tokenProvider = session,
                    ),
                    session = session,
                    context = context.applicationContext,
                ) as T
            }
        }
    }
}
