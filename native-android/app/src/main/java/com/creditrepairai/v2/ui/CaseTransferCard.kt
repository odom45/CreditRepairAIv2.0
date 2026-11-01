package com.creditrepairai.v2.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.creditrepairai.v2.MainViewModel
import com.creditrepairai.v2.data.TransferVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException

@Composable
fun CaseTransferCard(viewModel: MainViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val owner = viewModel.signedInSubject
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val encrypted = pendingExport
        pendingExport = null
        if (uri != null && encrypted != null) scope.launch {
            try { withContext(Dispatchers.IO) { requireNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(encrypted.toByteArray(Charsets.UTF_8)) } }; notice = "Encrypted transfer file saved. Keep its passphrase safe." }
            catch (error: Exception) { if (error is CancellationException) throw error; notice = "Transfer file could not be saved." }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val expected = owner
        if (uri != null && expected != null) scope.launch {
            busy = true
            val secret = password.toCharArray(); password = ""
            try {
                val raw = withContext(Dispatchers.IO) {
                    val bytes = requireNotNull(context.contentResolver.openInputStream(uri)).use { stream ->
                        val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
                        while (true) { val n = stream.read(buffer); if (n < 0) break; require(n > 0 && output.size().toLong() + n <= 28_000_000) { "Vault exceeds safe limits." }; output.write(buffer, 0, n) }; output.toByteArray()
                    }
                    TransferVault.decrypt(bytes.toString(Charsets.UTF_8), secret, expected)
                }
                viewModel.restorePortableCase(raw, expected)
                notice = "Encrypted case restored. Verify the imported facts."
            } catch (error: Exception) { if (error is CancellationException) throw error; notice = "Import failed. Check the account, passphrase and file; no case was replaced." }
            finally { secret.fill('\u0000'); busy = false }
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Encrypted cross-platform case transfer", style = MaterialTheme.typography.titleMedium)
            Text("Use the same Firebase account and transfer passphrase on web, iPhone, iPad or Mac. Export before signing out if you need to keep a portable copy. Import replaces this device's case. This is a manual encrypted transfer; no cloud case sync is implied.")
            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Transfer passphrase (12+ characters)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Button(enabled = !busy && owner != null && password.length >= 12, onClick = {
                val expected = owner ?: return@Button
                scope.launch {
                    busy = true
                    val secret = password.toCharArray(); password = ""
                    try {
                        val raw = viewModel.exportPortableCase()
                        val encrypted = withContext(Dispatchers.Default) { TransferVault.encrypt(raw, secret, expected) }
                        check(viewModel.signedInSubject == expected)
                        pendingExport = encrypted; export.launch("CreditRepairAI-encrypted-vault.json")
                    } catch (error: Exception) { if (error is CancellationException) throw error; notice = "Case could not be exported. Sign in and retry." }
                    finally { secret.fill('\u0000'); busy = false }
                }
            }) { Text("Export encrypted case") }
            OutlinedButton(enabled = !busy && owner != null && password.length >= 12, onClick = { import.launch(arrayOf("application/json")) }) { Text("Import and replace local case") }
            if (notice.isNotBlank()) Text(notice)
        }
    }
}
