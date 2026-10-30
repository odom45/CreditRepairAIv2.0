package com.creditrepairai.v2.ui

import android.app.Activity
import android.Manifest
import android.os.Build
import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.creditrepairai.v2.AppUiState
import com.creditrepairai.v2.MainViewModel
import com.creditrepairai.v2.guidance.PortalBrowser
import com.creditrepairai.v2.letters.*
import com.creditrepairai.v2.model.Bureau
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date

/** Sender details stay in memory; only explicit user export/printing creates a plaintext copy. */
@Composable
fun PaperLetterScreen(ui: AppUiState, viewModel: MainViewModel, initialDisputeId: String?, initialBureau: Bureau = Bureau.UNKNOWN) {
    val context = LocalContext.current
    val dispute = ui.data.disputes.firstOrNull { it.id == initialDisputeId } ?: ui.data.disputes.firstOrNull()
    if (dispute == null) { Text("Create a dispute from Analysis first."); return }
    val bureaus = dispute.bureaus.filter { it != Bureau.UNKNOWN }.distinct()
    var bureau by rememberSaveable(dispute.id, initialBureau) { mutableStateOf(initialBureau.takeIf { it in bureaus } ?: bureaus.firstOrNull() ?: Bureau.UNKNOWN) }
    var name by remember(dispute.id) { mutableStateOf("") }
    var address by remember(dispute.id) { mutableStateOf("") }
    var facts by remember(dispute.id, bureau) { mutableStateOf(dispute.reason) }
    var evidence by remember(dispute.id, bureau) { mutableStateOf("") }
    var resolution by remember(dispute.id, bureau) { mutableStateOf("Please investigate the disputed information and correct inaccurate or incomplete reporting supported by my records. Please send written results.") }
    var date by remember { mutableStateOf(DateFormat.getDateInstance(DateFormat.LONG).format(Date())) }
    var reviewed by remember(dispute.id, bureau, name, address, facts, evidence, resolution, date) { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var savedLetter by remember(dispute.id, bureau) { mutableStateOf<String?>(null) }
    var pendingText by remember { mutableStateOf<String?>(null) }
    var pendingOwner by remember { mutableStateOf<String?>(null) }
    var pendingDisputeId by remember { mutableStateOf<String?>(null) }
    var tracking by remember(dispute.id, bureau) { mutableStateOf("") }
    var mailingConfirmed by remember(dispute.id, bureau) { mutableStateOf(false) }
    val letter = if (bureau != Bureau.UNKNOWN && name.isNotBlank() && address.isNotBlank() && facts.isNotBlank() && evidence.isNotBlank() && resolution.isNotBlank() && date.isNotBlank())
        PaperDisputeLetter.compose(dispute, bureau, name, address, date, facts, evidence, resolution) else null
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val text = pendingText; pendingText = null
        val owner = pendingOwner; pendingOwner = null
        val exportingDisputeId = pendingDisputeId; pendingDisputeId = null
        if (uri != null && text != null && owner == viewModel.signedInSubject && viewModel.uiState.value.data.disputes.any { it.id == exportingDisputeId }) {
            message = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { LetterPdfRenderer.write(it, text) } ?: error("Could not open the selected destination")
                savedLetter = text
                "PDF saved. Print and mail it yourself, or use the optional paid mailing service below. Saving does not send a dispute."
            }.getOrElse { "Could not save the PDF. Retry with another destination." }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Print or send a dispute letter", style = MaterialTheme.typography.headlineMedium)
        Text(dispute.creditor)
        Text("Prepare a separate letter for each bureau that reports this item. Review the original report and edit the facts below.")
        bureaus.forEach { item -> FilterChip(bureau == item, { bureau = item }, label = { Text(item.displayName) }) }
        if (bureau == Bureau.UNKNOWN) { Text("Verify which bureau reports this item before preparing a letter."); return@Column }
        val recipient = PaperDisputeLetter.recipient(bureau)
        Text("Mail to:\n${recipient.address}")
        Text("Address checked ${PaperDisputeLetter.ADDRESS_CHECKED}. Recheck the bureau's instructions before mailing, including identity/address documents and any required dispute form. Add required identifiers directly on the bureau form; do not send them to AI.")
        OutlinedButton(onClick = { PortalBrowser.open(context, recipient.source) }) { Text("Official ${bureau.displayName} mail instructions") }
        OutlinedTextField(name, { name = it.take(160) }, label = { Text("Your full name — local only") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it.take(600) }, label = { Text("Your return mailing address — local only") }, minLines = 3, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(date, { date = it.take(80) }, label = { Text("Letter date") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(facts, { facts = it.take(10000) }, label = { Text("Verified facts and requested account correction") }, minLines = 4, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(resolution, { resolution = it.take(2000) }, label = { Text("Requested investigation / correction") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(evidence, { evidence = it.take(2000) }, label = { Text("Copies I will enclose") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        letter?.let { Text("Letter preview"); Text(it) }
        Text("Name/address stay in this screen's memory and are not sent to AI. Exported/printed copies contain those details. Choose a private destination. Returning to this screen may require re-entering them.")
        FilterChip(reviewed, { reviewed = !reviewed }, label = { Text("I verified every statement and the mailing requirements") }, enabled = letter != null)
        Button(onClick = { pendingText = letter; pendingOwner = viewModel.signedInSubject; pendingDisputeId = dispute.id; export.launch("CreditRepairAI-${bureau.name}-dispute.pdf") }, enabled = reviewed && letter != null, modifier = Modifier.fillMaxWidth()) { Text("Save ${bureau.displayName} letter PDF") }
        OutlinedButton(onClick = {
            message = runCatching {
                (context.getSystemService(Context.PRINT_SERVICE) as PrintManager).print("${bureau.displayName} dispute", LetterPrintAdapter(requireNotNull(letter)), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.NA_LETTER).build())
                "Choose your printer or Save as PDF. Printing does not mail the letter."
            }.getOrElse { "Printing is unavailable. Save the PDF instead." }
        }, enabled = reviewed && letter != null && context is Activity, modifier = Modifier.fillMaxWidth()) { Text("Print ${bureau.displayName} letter") }
        PaidMailServiceCard(letter, reviewed, letter != null && savedLetter == letter) {
            message = if (PortalBrowser.open(context, PAID_MAIL_SERVICE_URL))
                "LetterStream opened. Upload your final packet, review the total and complete payment there. Nothing has been uploaded, purchased or mailed by CreditRepairAI."
            else "Could not open LetterStream. Check that a browser is installed and try again. Your saved PDF is still available."
        }
        Text("Mailing checklist: print and sign; add the bureau's required form/identity documents and supporting copies; keep originals and a complete copy; mail to the verified address; keep mailing and delivery proof. Standard or trackable mail is your choice.")
        val mailed = ui.data.mailings.firstOrNull { it.disputeId == dispute.id && it.bureau == bureau }
        if (mailed == null) {
            OutlinedTextField(tracking, { tracking = it.take(120) }, label = { Text("Mail tracking number (optional, encrypted on device)") }, modifier = Modifier.fillMaxWidth())
            FilterChip(mailingConfirmed, { mailingConfirmed = !mailingConfirmed }, label = { Text("I confirmed this letter was actually mailed, by me or the provider") })
            Button(onClick = { viewModel.recordMailing(dispute.id, bureau, tracking) }, enabled = mailingConfirmed, modifier = Modifier.fillMaxWidth()) { Text("I mailed this ${bureau.displayName} letter") }
        } else {
            Text("Mailed: ${DateFormat.getDateInstance().format(Date(mailed.mailedAt))}\nTracking: ${mailed.trackingNumber.ifBlank { "Not entered" }}")
            if (mailed.deliveredAt == null) OutlinedButton(onClick = {
                viewModel.updateMailing(dispute.id, bureau)
                if (Build.VERSION.SDK_INT >= 33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("I confirmed delivery to ${bureau.displayName}") }
            else {
                val delivered = requireNotNull(mailed.deliveredAt)
                val format = DateFormat.getDateInstance()
                Text("Delivery recorded: ${format.format(Date(delivered))}. Check-ins: ${format.format(Date(delivered + 30L * 86400000))} and ${format.format(Date(delivered + 45L * 86400000))}. These are reminders, not promised result dates. Notification delivery depends on Android settings.")
            }
            if (mailed.responseReceivedAt == null) OutlinedButton(onClick = { viewModel.updateMailing(dispute.id, bureau, response = true) }) { Text("I received a ${bureau.displayName} response") }
            else Text("Response recorded. Compare it with your supporting records before deciding next steps.")
        }
        message?.let { Text(it) }
    }
}
private class LetterPrintAdapter(private val letter: String) : PrintDocumentAdapter() {
    override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?, cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: android.os.Bundle?) {
        if (cancellationSignal.isCanceled) callback.onLayoutCancelled()
        else {
            val pageCount = ByteArrayOutputStream().use { LetterPdfRenderer.write(it, letter) }
            callback.onLayoutFinished(PrintDocumentInfo.Builder("dispute-letter.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pageCount).build(), true)
        }
    }
    override fun onWrite(pages: Array<out android.print.PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
        if (cancellationSignal.isCanceled) { callback.onWriteCancelled(); return }
        try { FileOutputStream(destination.fileDescriptor).use { output -> LetterPdfRenderer.write(output, letter) { index -> pages.any { index in it.start..it.end } } }; callback.onWriteFinished(pages) }
        catch (_: Exception) { callback.onWriteFailed("Could not write the print document.") }
    }
}
