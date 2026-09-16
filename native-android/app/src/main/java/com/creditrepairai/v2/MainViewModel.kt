package com.creditrepairai.v2

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.creditrepairai.v2.ai.CreditAgentGateway
import com.creditrepairai.v2.ai.SensitiveDataRedactor
import com.creditrepairai.v2.analysis.CreditAnalyzer
import com.creditrepairai.v2.analysis.CreditReportParser
import com.creditrepairai.v2.data.AppRepository
import com.creditrepairai.v2.legal.DisputeLetterGenerator
import com.creditrepairai.v2.model.AppState
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.ChatMessage
import com.creditrepairai.v2.model.CreditAccount
import com.creditrepairai.v2.model.CreditReport
import com.creditrepairai.v2.model.DisputeStatus
import com.creditrepairai.v2.model.Finding
import com.creditrepairai.v2.model.ScoreSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val data: AppState = AppState(),
    val isProcessing: Boolean = false,
    val progressText: String = "",
    val notice: String? = null,
    val isAssistantThinking: Boolean = false,
    val isAgentEndpointConfigured: Boolean = false,
)

class MainViewModel(
    private val repository: AppRepository,
    private val parser: CreditReportParser,
    private val agentGateway: CreditAgentGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(
            data = repository.load(),
            isAgentEndpointConfigured = agentGateway.isConfigured,
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    fun importPdf(uri: Uri, displayName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, progressText = "Reading $displayName…", notice = null) }
            runCatching { parser.parse(uri, displayName) }
                .onSuccess { parsed ->
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
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            progressText = "",
                            notice = "Could not import $displayName: ${error.message ?: "unreadable PDF"}",
                        )
                    }
                }
        }
    }

    fun loadDemo() {
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
        mutate("Dispute removed from this device.") { state ->
            state.copy(disputes = state.disputes.filterNot { it.id == id })
        }
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

    fun askAssistant(prompt: String) {
        val cleaned = SensitiveDataRedactor.redact(prompt.trim())
        if (cleaned.isBlank()) return
        if (_uiState.value.isAssistantThinking) return

        val stateWithQuestion = _uiState.value.data.copy(
            chat = (_uiState.value.data.chat + ChatMessage(fromUser = true, text = cleaned)).takeLast(40),
        )
        repository.save(stateWithQuestion)
        _uiState.update { it.copy(data = stateWithQuestion, isAssistantThinking = true, notice = null) }

        viewModelScope.launch {
            val answer = runCatching { agentGateway.answer(cleaned, stateWithQuestion) }
                .getOrElse { error ->
                    "I could not use the secure legal agent: ${error.message ?: "service unavailable"}"
                }
            val current = _uiState.value.data
            val updated = current.copy(
                chat = (current.chat + ChatMessage(fromUser = false, text = answer)).takeLast(40),
            )
            repository.save(updated)
            _uiState.update { it.copy(data = updated, isAssistantThinking = false) }
        }
    }

    fun clearData() {
        repository.clear()
        _uiState.value = AppUiState(
            data = AppState(),
            notice = "All local case data was deleted and its Android Keystore encryption key was destroyed.",
            isAgentEndpointConfigured = agentGateway.isConfigured,
        )
    }

    fun consumeNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun mutate(notice: String?, transform: (AppState) -> AppState) {
        val updated = transform(_uiState.value.data)
        repository.save(updated)
        _uiState.value = AppUiState(
            data = updated,
            notice = notice,
            isAgentEndpointConfigured = agentGateway.isConfigured,
        )
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                repository = AppRepository(context),
                parser = CreditReportParser(context),
                agentGateway = CreditAgentGateway(
                    endpoint = BuildConfig.AI_GATEWAY_URL,
                    // Production must inject the short-lived OIDC access token
                    // obtained after native sign-in. A static APK token is forbidden.
                    tokenProvider = { null },
                ),
            ) as T
        }
    }
}
