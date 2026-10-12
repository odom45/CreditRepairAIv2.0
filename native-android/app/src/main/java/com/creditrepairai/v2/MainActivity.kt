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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            CreditRepairTheme {
                CreditRepairApp(viewModel)
            }
        }
        acceptPdf(intent)
        lifecycleScope.launch { viewModel.uiState.collect { if (it.signedInEmail != null) importPendingPdf() } }
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                viewModel.billing.refresh()
                viewModel.rescheduleCheckins()
                importPendingPdf()
            }
        })
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); acceptPdf(intent) }
    private fun acceptPdf(incoming: Intent) {
        if (incoming.type != "application/pdf") return
        @Suppress("DEPRECATION")
        val uri = if (incoming.action == Intent.ACTION_SEND) incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) else incoming.data
        if (uri?.scheme != "content") return
        pendingPdf = uri
        importPendingPdf()
    }
    private fun importPendingPdf() {
        if (viewModel.uiState.value.signedInEmail == null) return
        val uri = pendingPdf ?: return
        val name = runCatching { contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { if (it.moveToFirst()) it.getString(0) else null } }.getOrNull() ?: "Credit-report.pdf"
        viewModel.importPdf(uri, name)
        pendingPdf = null
    }
}
