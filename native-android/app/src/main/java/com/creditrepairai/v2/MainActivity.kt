package com.creditrepairai.v2

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.creditrepairai.v2.ui.CreditRepairApp
import com.creditrepairai.v2.ui.theme.CreditRepairTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels { MainViewModel.factory(applicationContext) }
    private var pendingPdf: Uri? = null
    private var pdfIntentConsumed = false
    private val externalResults = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            CreditRepairTheme {
                CreditRepairApp(viewModel)
            }
        }
        @Suppress("DEPRECATION")
        if (savedInstanceState != null) {
            pendingPdf = savedInstanceState.getParcelable("pendingPdf")
            pdfIntentConsumed = savedInstanceState.getBoolean("pdfIntentConsumed")
        }
        if (!pdfIntentConsumed) acceptPdf(intent)
        lifecycleScope.launch { viewModel.uiState.collect { importPendingPdf() } }
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                if (viewModel.isSessionLocked) return
                viewModel.checkEmailVerification(quiet = true)
                viewModel.billing.refresh()
                viewModel.rescheduleCheckins()
                importPendingPdf()
            }
        })
    }
    override fun onStop() {
        super.onStop()
        // Android document/permission pickers and explicit Google authentication are continuations,
        // not a closed app. Their results must reach the original reviewed workflow.
        if (!isChangingConfigurations && externalResults.isEmpty() && !viewModel.isGoogleSignInInProgress) viewModel.lockSession()
    }
    override fun onDestroy() {
        if (isFinishing) viewModel.lockSession()
        super.onDestroy()
    }
    @Suppress("DEPRECATION")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        externalResults.add(requestCode)
        try { super.startActivityForResult(intent, requestCode, options) }
        catch (error: Exception) { externalResults.remove(requestCode); throw error }
    }
    @Deprecated("Android activity result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try { super.onActivityResult(requestCode, resultCode, data) }
        finally { externalResults.remove(requestCode) }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); acceptPdf(intent) }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable("pendingPdf", pendingPdf)
        outState.putBoolean("pdfIntentConsumed", pdfIntentConsumed)
        super.onSaveInstanceState(outState)
    }
    private fun acceptPdf(incoming: Intent) {
        if (incoming.type != "application/pdf") return
        @Suppress("DEPRECATION")
        val uri = if (incoming.action == Intent.ACTION_SEND) incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) else incoming.data
        if (uri?.scheme != "content") return
        pdfIntentConsumed = true
        pendingPdf = uri
        importPendingPdf()
    }
    private fun importPendingPdf() {
        val state = viewModel.uiState.value
        if (state.signedInEmail == null || !state.emailVerified || state.data.policyAcceptance?.isCurrent != true || !state.data.onboardingComplete) return
        val uri = pendingPdf ?: return
        pendingPdf = null
        val name = runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull() ?: "Credit-report.pdf"
        viewModel.importPdf(uri, name)
    }
}
