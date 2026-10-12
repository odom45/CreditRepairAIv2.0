package com.creditrepairai.v2.ui

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
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
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creditrepairai.v2.*
import com.creditrepairai.v2.guidance.*
import com.creditrepairai.v2.model.*
import com.creditrepairai.v2.ui.theme.*

@Composable
fun EducationLabel() { Text(EDUCATIONAL_LABEL, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Amber400) }

@Composable
private fun LaunchColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable
fun OnboardingScreen(onComplete: () -> Unit, onPolicy: () -> Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val pages = listOf(
        "Your workflow" to "Request reports → import PDFs → verify extracted facts → prepare supported drafts → submit yourself → track correspondence. Reports may not include a score.",
        "You review and submit" to "Findings are prompts for review, not proof of an error. Check every fact and citation. The app never submits disputes or complaints, fills forms, or handles bureau/government credentials.",
        "Privacy by design" to "PDF parsing and OCR run on this device. Saved case data is encrypted. AI uses a redacted snapshot, not the original PDF. Browser pages remain controlled by your browser and the official site.",
        "Software access, not results" to "Monthly access, when enabled, pays for guided tools, AI assistance, and education. Results are not guaranteed. There are no deletion fees or outcome-based prices.",
        "Your responsibilities" to "Keep originals, protect PDFs saved by your browser, and verify all drafts. Reminder dates are check-ins, not outcome predictions. Read the privacy policy and terms before continuing.",
    )
    LaunchColumn {
        Text("Welcome to CreditRepairAI", style = MaterialTheme.typography.headlineMedium)
        Text("Step ${step + 1} of ${pages.size}")
        Text(pages[step].first, style = MaterialTheme.typography.titleLarge)
        Text(pages[step].second)
        EducationLabel()
        OutlinedButton(onClick = onPolicy, modifier = Modifier.fillMaxWidth()) { Text("Read privacy policy and terms") }
        if (step > 0) OutlinedButton(onClick = { step-- }, modifier = Modifier.fillMaxWidth()) { Text("Previous") }
        Button(onClick = { if (step == pages.lastIndex) onComplete() else step++ }, modifier = Modifier.fillMaxWidth()) {
            Text(if (step == pages.lastIndex) "I understand — continue" else "Continue")
        }
    }
}
@Composable
fun AcquisitionScreen(data: AppState, update: (AcquisitionState) -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    var error by remember { mutableStateOf("") }
    val state = data.acquisition
    val instructions = listOf(
        "Prepare" to "Use the official AnnualCreditReport.com site. Only you enter your identity and credentials there. This app cannot read, fill, store, or transmit them.",
        "Request" to "Choose the reports you need and follow each bureau's own identity checks. Leave and return at any time; this checklist is saved.",
        "Save a PDF" to "Download the report. If it is a web page, use the browser menu → Print → Save as PDF. If Print is unavailable here, open the page in your full browser and use its menu. Do not share a login page with this app.",
        "Import" to "Open the downloaded PDF or share it → CreditRepairAI. Only the PDF you explicitly select is parsed on-device. Alternatively tap Import downloaded PDF below. Custom Tabs do not let an app silently intercept browser downloads.",
        "Verify" to "Confirm bureau, account fields, and reporting dates against the original PDF. Missing or unknown bureaus need another report or manual verification. Flags do not prove errors.",
    )
    LaunchColumn {
        Text("Get your reports", style = MaterialTheme.typography.headlineMedium)
        Text("Step ${state.step.ordinal + 1} of ${instructions.size}: ${instructions[state.step.ordinal].first}", fontWeight = FontWeight.Bold)
        Text(instructions[state.step.ordinal].second)
        Button(onClick = { error = if (PortalBrowser.open(context, "https://www.annualcreditreport.com/")) "" else "No compatible browser found. Install or enable a trusted browser and try again." }, modifier = Modifier.fillMaxWidth()) { Text("Open AnnualCreditReport.com") }
        Button(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text("Import downloaded PDF") }
        if (error.isNotEmpty()) Text(error, color = Amber400)
        Text("Captured bureaus: ${state.confirmed.joinToString { it.displayName }.ifBlank { "None yet" }}")
        Text("Missing: ${state.missing.joinToString { it.displayName }.ifBlank { "All three captured — verify their contents" }}")
        if (data.reports.any { it.bureau == Bureau.UNKNOWN }) Text("An imported report has an unknown bureau. Verify the original and try a bureau-labelled filename; do not rely on its flags yet.", color = Amber400)
        OutlinedButton(onClick = { update(state.back()) }, enabled = state.step != AcquisitionStep.PREPARE, modifier = Modifier.fillMaxWidth()) { Text("Previous step") }
        Button(onClick = { update(state.next()) }, enabled = state.step != AcquisitionStep.VERIFY, modifier = Modifier.fillMaxWidth()) { Text("Next step") }
    }
}
@Composable
fun FilingScreen(ui: AppUiState, viewModel: MainViewModel, initialPortal: FilingPortal = FilingPortal.EXPERIAN) {
    val data = ui.data
    val context = LocalContext.current
    var selectedDispute by rememberSaveable { mutableStateOf(data.disputes.firstOrNull()?.id.orEmpty()) }
    var portal by rememberSaveable { mutableStateOf(initialPortal) }
    var limitText by rememberSaveable { mutableStateOf("1000") }
    var receipt by rememberSaveable(selectedDispute, portal) { mutableStateOf("") }
    var browserError by remember { mutableStateOf("") }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val dispute = data.disputes.firstOrNull { it.id == selectedDispute } ?: data.disputes.firstOrNull()
    val newRecord = remember(dispute?.id, portal) { FilingRecord(disputeId = dispute?.id.orEmpty(), portal = portal) }
    val record = data.filings.firstOrNull { it.disputeId == dispute?.id && it.portal == portal } ?: newRecord
    val ready = dispute != null && (portal != FilingPortal.CFPB || FilingGuide.cfpbReady(dispute, data.filings, System.currentTimeMillis()))
    LaunchColumn {
        Text("Guided filing", style = MaterialTheme.typography.headlineMedium)
        EducationLabel()
        Text("This checklist follows your saved progress, not the portal page. Partial browser support varies: keep the guide below the browser or return to the app. We cannot see fields or detect submission.")
        if (dispute == null) Text("Create a fact-specific dispute from Analysis first. Then return here.")
        data.disputes.forEach { item -> FilterChip(selected = dispute?.id == item.id, onClick = { selectedDispute = item.id }, label = { Text(item.creditor) }) }
        FilingPortal.entries.forEach { item -> FilterChip(selected = portal == item, onClick = { portal = item }, label = { Text(item.label) }) }
        if (dispute != null) {
            Text("Step ${record.step + 1} of 7: ${FilingGuide.steps[record.step].first}", style = MaterialTheme.typography.titleLarge)
            Text(FilingGuide.steps[record.step].second)
            val help = ui.filingHelp.takeIf { it.recordId == record.id && it.step == record.step && it.contextKey == FilingGuide.contextKey(dispute, record) }
            TextButton(onClick = { viewModel.explainFiling(record) }, enabled = !ui.filingHelp.thinking) { Text("Ask AI to explain this step") }
            if (help?.thinking == true) Text("Preparing an explanation for this saved step…")
            help?.error?.let { Text(it, color = Amber400) }
            FilingGuide.currentExplanation(dispute, record)?.let { answer ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("AI explanation for this step", fontWeight = FontWeight.Bold)
                        EducationLabel()
                        Text(answer)
                        Text("Compare this guidance with the actual portal and your records. Only you review, paste and submit.")
                    }
                }
            }
            if (!ready) Text("First record the reporting company's response, or wait until the saved 45-day check-in date. CFPB guidance is not a guarantee of eligibility; verify current portal requirements.", color = Amber400)
            Button(onClick = { browserError = if (PortalBrowser.open(context, portal.url)) "" else "A compatible browser is unavailable. Try a trusted browser." }, enabled = ready && record.reviewed, modifier = Modifier.fillMaxWidth()) { Text("Open official ${portal.label} portal") }
            if (browserError.isNotEmpty()) Text(browserError, color = Amber400)
            FilterChip(selected = record.reviewed, onClick = { viewModel.saveFiling(record.copy(reviewed = !record.reviewed)) }, label = { Text("I reviewed the facts against my records") })
            OutlinedTextField(value = record.problem, onValueChange = { viewModel.saveFiling(record.copy(problem = it.take(4000), reviewed = false)) }, label = { Text("What happened — factual explanation") }, placeholder = { Text(dispute.reason) }, minLines = 3, modifier = Modifier.fillMaxWidth())
            if (portal == FilingPortal.CFPB) OutlinedTextField(value = record.companyResponse, onValueChange = { viewModel.saveFiling(record.copy(companyResponse = it.take(3000), reviewed = false)) }, label = { Text("Actual company response") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = record.resolution, onValueChange = { viewModel.saveFiling(record.copy(resolution = it.take(2000), reviewed = false)) }, label = { Text("Requested review / correction") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = record.evidence, onValueChange = { viewModel.saveFiling(record.copy(evidence = it.take(2000), reviewed = false)) }, label = { Text("Supporting evidence checklist") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = limitText, onValueChange = { limitText = it.filter(Char::isDigit).take(5) }, label = { Text("Current portal field character limit (100–10000)") }, modifier = Modifier.fillMaxWidth())
            Text("Chunks are generic field suggestions. Match the actual portal prompt; do not silently omit a chunk or paste multiple chunks into a field that cannot hold them.")
            DisputeChunker.fields(dispute, record, (limitText.toIntOrNull() ?: 1000).coerceIn(100, 10000)).forEach { chunk ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(chunk.label, fontWeight = FontWeight.Bold); Text(chunk.text)
                        OutlinedButton(onClick = {
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(chunk.label, chunk.text))
                        }, enabled = record.reviewed) { Text("Copy ${chunk.label}") }
                    }
                }
            }
            OutlinedButton(onClick = { viewModel.saveFiling(record.copy(step = (record.step - 1).coerceAtLeast(0))) }, enabled = record.step > 0, modifier = Modifier.fillMaxWidth()) { Text("Previous step") }
            Button(onClick = { viewModel.saveFiling(record.copy(step = (record.step + 1).coerceAtMost(6))) }, enabled = record.step < 6, modifier = Modifier.fillMaxWidth()) { Text("Next step") }
            if (record.submittedAt == null) {
                OutlinedTextField(value = receipt, onValueChange = { receipt = it.take(120) }, label = { Text("Actual confirmation number after you submit") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.recordSubmission(record, receipt); if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }, enabled = ready && record.reviewed && record.step == 6 && receipt.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("I submitted it myself — save receipt") }
            } else {
                Text("Saved receipt: ${record.confirmation}")
                Text("30/45-day reminders are check-ins, not promised response dates. Delivery may be delayed by Android or notification settings.")
                if (record.responseReceivedAt == null) OutlinedButton(onClick = { viewModel.recordResponse(record) }, modifier = Modifier.fillMaxWidth()) { Text("I received a company response") }
                else Text("Response received recorded. Review the response before changing dispute status.")
            }
        }
    }
}
@Composable
fun PlanningScreen(data: AppState, viewModel: MainViewModel, onAssistant: () -> Unit) {
    var goal by rememberSaveable { mutableStateOf(data.goal.description) }
    var months by rememberSaveable { mutableStateOf(data.goal.targetMonths.toString()) }
    LaunchColumn {
        Text("Goals & next actions", style = MaterialTheme.typography.headlineMedium)
        EducationLabel()
        OutlinedTextField(value = goal, onValueChange = { goal = it.take(1000) }, label = { Text("My goal (no identifiers)") }, placeholder = { Text("Prepare to apply for a home loan") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = months, onValueChange = { months = it.filter(Char::isDigit).take(3) }, label = { Text("My target months — not a forecast") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = { viewModel.saveGoal(CreditGoal(goal, months.toIntOrNull() ?: 12, data.goal.aiPlan)); viewModel.buildGoalPlan(); onAssistant() }, enabled = goal.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save goal and ask AI for a sequenced plan") }
        Text("Priority checklist (on-device, not an AI answer)", fontWeight = FontWeight.Bold)
        GoalPlanner.prioritize(data).forEachIndexed { i, action -> Text("${i + 1}. ${action.title}\n${action.reason}") }
        if (data.goal.aiPlan.isNotBlank()) { Text("Saved AI educational plan", fontWeight = FontWeight.Bold); Text(data.goal.aiPlan) }
        Text("Filed portal receipts: ${data.filings.count { it.submittedAt != null }} • Responses recorded: ${data.filings.count { it.responseReceivedAt != null }}")
    }
}
@Composable
fun SubscriptionScreen(viewModel: MainViewModel) {
    val state by viewModel.billing.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchColumn {
        Text("Monthly software access", style = MaterialTheme.typography.headlineMedium)
        Text("Guided tools, AI assistance, and education — not paid deletion, representation, approval, or a score change. Results are not guaranteed.")
        EducationLabel()
        Text("Plan state: ${state.entitlement.state}")
        if (state.entitlement.expiresAt > 0) Text("Play access expiry: ${java.text.DateFormat.getDateInstance().format(java.util.Date(state.entitlement.expiresAt))}")
        Text(state.message)
        if (!BuildConfig.PAID_ACCESS_ENABLED) Text("Purchases are disabled pending owner configuration and compliance review. No charge can be started in this build.", color = Amber400)
        Button(onClick = { (context as? Activity)?.let(viewModel.billing::purchase) }, enabled = BuildConfig.PAID_ACCESS_ENABLED && state.available && !state.busy && !state.entitlement.hasAccess(System.currentTimeMillis()), modifier = Modifier.fillMaxWidth()) {
            Text(if (state.price.isBlank()) "Monthly plan unavailable" else "Subscribe for ${state.price} / month")
        }
        Text("Renews monthly until canceled. Cancel anytime through Google Play; cancellation normally stops the next renewal, not the current access period. Google Play shows the final price and terms before you accept.")
        OutlinedButton(onClick = viewModel.billing::refresh, modifier = Modifier.fillMaxWidth()) { Text("Restore purchases / refresh status") }
        OutlinedButton(onClick = { PortalBrowser.open(context, "https://play.google.com/store/account/subscriptions?sku=${BuildConfig.PLAY_PRODUCT_ID}&package=com.creditrepairai.v2") }, modifier = Modifier.fillMaxWidth()) { Text("Manage or cancel in Google Play") }
    }
}
@Composable
fun PolicyScreen() {
    val context = LocalContext.current
    LaunchColumn {
        Text("Privacy policy & terms", style = MaterialTheme.typography.headlineMedium)
        Text("Version: 2026-10-12 • Operator: 3six0 Studios LLC • U.S. educational software. Owner must complete legal review before commercial launch.")
        Text("Privacy policy", style = MaterialTheme.typography.titleLarge)
        Text("Report PDFs are selected by you and parsed on-device. Extracted case data, drafts, receipts, goals and chat are stored using Android Keystore AES-GCM. App screens block screenshots and backup is disabled. Original PDFs downloaded by your browser remain outside the app; delete them yourself when no longer needed.")
        Text("Firebase handles app sign-in. The app never receives bureau or government login credentials; those stay with the browser and official site. When you ask AI, a short-lived Firebase token and a redacted structured case snapshot are sent over HTTPS to Convex and OpenAI. Automated redaction can miss unusual identifiers; avoid including identifiers in goals/chat and review what you share. We do not upload original report PDFs.")
        Text("Google Play handles purchases. The backend stores the account subject, purchase token, product/state/expiry and limited audit records to verify access, prevent token reuse, and manage subscriptions/refunds. No advertising or analytics SDK is included in the native app. Do not paste sensitive information into support requests or public issues.")
        Text("Delete local data in Settings to destroy the local encryption key and cancel reminders. Account deletion first removes Convex billing records, then deletes Firebase login; recent sign-in may be required. A minimal hashed account-deletion marker remains to prevent delayed server work from restoring records; it contains no reports or purchase tokens. This does not delete the Google Play receipt, cancel a subscription, delete browser downloads, or guarantee removal from vendor retention/backups. Cancel renewal in Google Play separately. Contact support for privacy requests and vendor-retention questions. Owner must publish the external deletion-request and privacy URLs before Play launch.")
        Text("Terms of service", style = MaterialTheme.typography.titleLarge)
        EducationLabel()
        Text("Use these tools only for your own records or records you have permission to use. You verify every fact, citation, attachment and statement, and submit directly to the official site yourself. Findings are review prompts, not proof of an error. AI output may be incorrect; verify cited authority and seek qualified advice when needed. Never allege fraud without supporting facts or dispute information you know to be accurate.")
        Text("Software access does not include credit-repair representation, a government filing service, lending decisions, a guarantee of deletion or score change, or a promised time to results. Monthly subscriptions, when enabled, renew until canceled through Google Play. Purchases are subject to the price and terms shown by Play. Refunds are subject to applicable law and Play policy; statutory rights are not waived by these terms.")
        Text("Security features reduce risk; they are not a bank-level-security certification. Keep your device protected. External portals have their own terms and privacy policies. Features, parsing accuracy, connectivity, browser support and reminders can vary. Do not rely on reminders as legal deadlines. The software is intended for U.S. adults; do not upload another person's information without permission.")
        Text("Support & privacy contact", style = MaterialTheme.typography.titleLarge)
        if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}"))) } }, modifier = Modifier.fillMaxWidth()) { Text("Email ${BuildConfig.SUPPORT_EMAIL}") }
        else {
            Text("Owner support mailbox has not been configured. Commercial release is blocked until a monitored contact is supplied.", color = Amber400)
            OutlinedButton(onClick = { PortalBrowser.open(context, "https://github.com/odom45/CreditRepairAIv2.0/issues") }, modifier = Modifier.fillMaxWidth()) { Text("Report a software issue (public — no case data)") }
        }
    }
}
