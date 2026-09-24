package com.creditrepairai.v2.ui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.creditrepairai.v2.AppUiState
import com.creditrepairai.v2.MainViewModel
import com.creditrepairai.v2.analysis.CreditAnalyzer
import com.creditrepairai.v2.legal.DisputeLetterGenerator
import com.creditrepairai.v2.model.AppState
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.Dispute
import com.creditrepairai.v2.model.DisputeStatus
import com.creditrepairai.v2.model.Finding
import com.creditrepairai.v2.model.ScoreSnapshot
import com.creditrepairai.v2.model.Severity
import com.creditrepairai.v2.ui.theme.Amber400
import com.creditrepairai.v2.ui.theme.Blue500
import com.creditrepairai.v2.ui.theme.Cyan400
import com.creditrepairai.v2.ui.theme.Emerald400
import com.creditrepairai.v2.ui.theme.Navy800
import com.creditrepairai.v2.ui.theme.Navy900
import com.creditrepairai.v2.ui.theme.Navy950
import com.creditrepairai.v2.ui.theme.Rose400
import com.creditrepairai.v2.ui.theme.Slate300
import com.creditrepairai.v2.ui.theme.Slate400
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private enum class Destination(val label: String, val icon: ImageVector) {
    DASHBOARD("Home", Icons.Default.Dashboard),
    REPORTS("Reports", Icons.Default.PictureAsPdf),
    ANALYSIS("Analysis", Icons.Default.Analytics),
    DISPUTES("Disputes", Icons.Default.Description),
    MORE("More", Icons.Default.MoreHoriz),
    CFPB("CFPB Assistant", Icons.Default.AccountBalance),
    ASSISTANT("AI Assistant", Icons.Default.AutoAwesome),
    PROGRESS("Progress", Icons.Default.Timeline),
    FREEZES("Freezes", Icons.Default.Security),
    SETTINGS("Settings", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditRepairApp(viewModel: MainViewModel) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    if (ui.signedInEmail == null) {
        AuthenticationScreen(ui, viewModel)
        return
    }
    var destination by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(ui.notice) {
        ui.notice?.let {
            snackbar.showSnackbar(it)
            viewModel.consumeNotice()
        }
    }

    Scaffold(
        containerColor = Navy950,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy950),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Blue500, shape = RoundedCornerShape(10.dp)) {
                            Text("C", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("CreditRepairAI", fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Text(destination.label, color = Slate400, fontSize = 11.sp)
                        }
                    }
                },
                navigationIcon = {
                    if (destination !in Destination.entries.take(5)) {
                        IconButton(onClick = { destination = Destination.MORE }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Navy900) {
                Destination.entries.take(5).forEach { item ->
                    val selected = destination == item || (item == Destination.MORE && destination !in Destination.entries.take(5))
                    NavigationBarItem(
                        selected = selected,
                        onClick = { destination = item },
                        icon = {
                            if (item == Destination.DISPUTES && ui.data.disputes.isNotEmpty()) {
                                BadgedBox(badge = { Badge { Text(ui.data.disputes.size.toString()) } }) {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            } else Icon(item.icon, contentDescription = item.label)
                        },
                        label = { Text(item.label, fontSize = 10.sp) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (destination) {
                Destination.DASHBOARD -> DashboardScreen(ui.data, viewModel, onNavigate = { destination = it })
                Destination.REPORTS -> ReportsScreen(ui, viewModel)
                Destination.ANALYSIS -> AnalysisScreen(ui.data, viewModel)
                Destination.DISPUTES -> DisputesScreen(ui.data, viewModel)
                Destination.MORE -> MoreScreen { destination = it }
                Destination.CFPB -> CfpbScreen(ui.data)
                Destination.ASSISTANT -> AssistantScreen(ui, viewModel)
                Destination.PROGRESS -> ProgressScreen(ui.data, viewModel)
                Destination.FREEZES -> FreezesScreen(ui.data, viewModel)
                Destination.SETTINGS -> SettingsScreen(ui, viewModel)
            }

            if (ui.isProcessing) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = Navy800)) {
                        Column(
                            Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(color = Cyan400)
                            Text(ui.progressText, fontWeight = FontWeight.Bold)
                            Text("Parsing stays on this device", color = Slate400, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthenticationScreen(ui: AppUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var createAccount by rememberSaveable { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Navy950) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.height(44.dp))
            Surface(color = Blue500, shape = RoundedCornerShape(18.dp)) {
                Text("C", modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontWeight = FontWeight.Black, fontSize = 24.sp)
            }
            Spacer(Modifier.height(18.dp))
            Text("Welcome to CreditRepairAI", fontWeight = FontWeight.Black, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Sign in before using reports or the legal AI assistant. Local data stays encrypted on this device.",
                color = Slate400,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.signInWithEmail(email, password, createAccount) },
                enabled = !ui.isAuthenticating && email.contains("@") && password.length >= 6,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (createAccount) "Create account" else "Sign in")
            }
            TextButton(onClick = { createAccount = !createAccount }, modifier = Modifier.fillMaxWidth()) {
                Text(if (createAccount) "Already have an account? Sign in" else "New here? Create an account")
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text("  or  ", color = Slate400, fontSize = 12.sp)
                HorizontalDivider(modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { activity?.let(viewModel::signInWithGoogle) },
                enabled = !ui.isAuthenticating && activity != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue with Google") }
            if (ui.isAuthenticating) {
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Signing in…", color = Slate400)
                }
            }
            ui.notice?.let {
                Spacer(Modifier.height(16.dp))
                Text(it, color = Amber400, fontSize = 13.sp)
            }
            Spacer(Modifier.height(28.dp))
            Text(
                "Educational credit organization and drafting assistance—not legal advice, score guarantees, or lender decisions.",
                color = Slate400,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ScreenList(content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Hero(title: String, subtitle: String, icon: ImageVector = Icons.Default.AutoAwesome) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Blue500.copy(.9f), Color(0xFF4338CA))), RoundedCornerShape(22.dp))
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = Color.White.copy(alpha = .85f), lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun DashboardScreen(data: AppState, viewModel: MainViewModel, onNavigate: (Destination) -> Unit) {
    val latestScores = data.scores.groupBy { it.bureau }.mapValues { (_, values) -> values.maxBy { it.recordedAt } }
    val progress = ((data.reports.map { it.bureau }.distinct().size.coerceAtMost(3) * 15) +
        (if (data.findings.isNotEmpty()) 20 else 0) +
        (data.disputes.count { it.status != DisputeStatus.DRAFT }.coerceAtMost(3) * 10)).coerceAtMost(100)

    ScreenList {
        item { Hero("Fix your credit with clarity", "Native, private report analysis with guided disputes and CFPB assistance.") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("ACTION PROGRESS", color = Slate400, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("$progress%", color = Cyan400, fontWeight = FontWeight.Black)
                    }
                    LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape))
                    Text("Progress measures completed steps, not a promised score increase.", color = Slate400, fontSize = 12.sp)
                }
            }
        }
        item { SectionTitle("Bureau scores") }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Bureau.TRANSUNION, Bureau.EXPERIAN, Bureau.EQUIFAX).forEach { bureau ->
                    val snapshot = latestScores[bureau]
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        label = bureau.displayName.take(2).uppercase(),
                        value = snapshot?.score?.toString() ?: "—",
                        caption = if (snapshot == null) "Import" else "Latest",
                    )
                }
            }
        }
        item { SectionTitle("Quick actions") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionRow(Icons.Default.FileOpen, "Import reports", "Select original bureau PDFs") { onNavigate(Destination.REPORTS) }
                ActionRow(Icons.Default.Analytics, "Review ${data.findings.size} finding(s)", "Compare balances, status, and bureau coverage") { onNavigate(Destination.ANALYSIS) }
                ActionRow(Icons.Default.AccountBalance, "CFPB assistant", "Prepare and review a complaint narrative") { onNavigate(Destination.CFPB) }
                ActionRow(Icons.Default.AutoAwesome, "Ask CreditRepairAI", "Get report-aware next-step guidance") { onNavigate(Destination.ASSISTANT) }
            }
        }
        if (data.reports.isEmpty()) {
            item {
                OutlinedCard(border = BorderStroke(1.dp, Cyan400.copy(.4f)), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Want to test every screen first?", fontWeight = FontWeight.Bold)
                        Text("Load fictional accounts and scores. Nothing leaves your phone.", color = Slate400)
                        Button(onClick = viewModel::loadDemo) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Load safe demo") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsScreen(ui: AppUiState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            viewModel.importPdf(uri, context.displayName(uri))
        }
    }
    ScreenList {
        item { Hero("Import credit reports", "Choose downloaded PDFs. Text extraction and OCR run on your device.", Icons.Default.PictureAsPdf) }
        item {
            Button(
                onClick = { launcher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !ui.isProcessing,
            ) {
                Icon(Icons.Default.FileOpen, null)
                Spacer(Modifier.width(8.dp))
                Text("Select PDF reports", fontWeight = FontWeight.Bold)
            }
        }
        item {
            NoticeCard(
                Icons.Default.Lock,
                "Private by default",
                "The native parser stores structured findings, not the full PDF text. Local case data is encrypted with an Android Keystore key and excluded from backup.",
                Emerald400,
            )
        }
        if (ui.data.reports.isEmpty()) item { EmptyState("No reports imported", "Import one or more PDFs, ideally one from each major bureau.") }
        items(ui.data.reports.sortedByDescending { it.importedAt }, key = { it.id }) { report ->
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Blue500.copy(.16f), shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.PictureAsPdf, null, tint = Cyan400, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(report.fileName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${report.bureau.displayName} • ${report.accountCount} entries • ${report.extractionMethod}", color = Slate400, fontSize = 12.sp)
                    }
                    report.score?.let { Text(it.toString(), color = Cyan400, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                }
            }
        }
        item {
            Text(
                "Parser results must be checked against the original report. Formats vary and OCR can misread text.",
                color = Slate400,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun AnalysisScreen(data: AppState, viewModel: MainViewModel) {
    ScreenList {
        item { Hero("Three-bureau analysis", "Review differences; a flag is a prompt to verify, not proof the data is wrong.", Icons.Default.Analytics) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard(Modifier.weight(1f), "Reports", data.reports.size.toString(), "Imported")
                MetricCard(Modifier.weight(1f), "Accounts", data.accounts.size.toString(), "Parsed")
                MetricCard(Modifier.weight(1f), "Flags", data.findings.size.toString(), "Review")
            }
        }
        if (data.findings.isEmpty()) item {
            EmptyState(
                if (data.reports.isEmpty()) "Import reports to begin" else "No supported mismatch detected",
                if (data.reports.isEmpty()) "The analyzer works best with all three bureau PDFs." else "Review extracted accounts manually; absence of a flag does not prove the report is error-free.",
            )
        }
        items(data.findings, key = { it.id }) { finding ->
            FindingCard(finding, data.disputes.any { it.findingId == finding.id }) { viewModel.createDispute(finding) }
        }
        if (data.accounts.isNotEmpty()) {
            item { SectionTitle("Parsed account entries") }
            items(data.accounts.take(100), key = { it.id }) { account ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(account.creditor, fontWeight = FontWeight.Bold)
                            Text("${account.bureau.displayName} • ••••${account.accountSuffix.ifBlank { "—" }} • ${account.status}", color = Slate400, fontSize = 12.sp)
                        }
                        Text(account.balance?.let { "$$it" } ?: "—", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { LegalAccuracyNote() }
    }
}

@Composable
private fun FindingCard(finding: Finding, created: Boolean, onCreate: () -> Unit) {
    val color = when (finding.severity) { Severity.HIGH -> Rose400; Severity.MEDIUM -> Amber400; Severity.LOW -> Cyan400 }
    Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(finding.creditor, color = Cyan400, fontWeight = FontWeight.Black)
                Surface(color = color.copy(.14f), shape = CircleShape) {
                    Text(finding.severity.name, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                }
            }
            Text(finding.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(finding.detail, color = Slate300, lineHeight = 20.sp)
            Text(finding.legalBasis.joinToString(" • "), color = Slate400, fontSize = 11.sp)
            Button(onClick = onCreate, enabled = !created, modifier = Modifier.fillMaxWidth()) {
                Icon(if (created) Icons.Default.Check else Icons.Default.Gavel, null)
                Spacer(Modifier.width(8.dp))
                Text(if (created) "Draft created" else "Generate reviewable dispute")
            }
        }
    }
}

@Composable
private fun DisputesScreen(data: AppState, viewModel: MainViewModel) {
    ScreenList {
        item { Hero("Dispute workspace", "Review, copy, share, and track fact-specific letters.", Icons.Default.Description) }
        if (data.disputes.isEmpty()) item { EmptyState("No disputes yet", "Create a draft from a verified finding in Analysis.") }
        items(data.disputes.sortedByDescending { it.createdAt }, key = { it.id }) { dispute ->
            DisputeCard(dispute, viewModel)
        }
        item { LegalAccuracyNote() }
    }
}

@Composable
private fun DisputeCard(dispute: Dispute, viewModel: MainViewModel) {
    var expanded by rememberSaveable(dispute.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dispute.creditor, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(dispute.bureaus.joinToString { it.displayName }, color = Slate400, fontSize = 12.sp)
                }
                StatusPill(dispute.status)
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
                }
            }
            AnimatedVisibility(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = Navy950, shape = RoundedCornerShape(12.dp)) {
                        Text(dispute.letter, modifier = Modifier.padding(14.dp), color = Slate300, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { context.copyText("Dispute letter", dispute.letter) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(5.dp)); Text("Copy")
                        }
                        OutlinedButton(onClick = { context.shareText("Credit dispute draft", dispute.letter) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Share, null); Spacer(Modifier.width(5.dp)); Text("Share")
                        }
                    }
                    Text("Update tracking status", color = Slate400, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(DisputeStatus.READY, DisputeStatus.SENT, DisputeStatus.INVESTIGATING, DisputeStatus.RESOLVED).forEach { status ->
                            FilterChip(
                                selected = dispute.status == status,
                                onClick = { viewModel.updateDispute(dispute.id, status) },
                                label = { Text(status.name.take(5).lowercase(), fontSize = 9.sp) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    TextButton(onClick = { confirmDelete = true }, colors = ButtonDefaults.textButtonColors(contentColor = Rose400)) {
                        Icon(Icons.Default.Delete, null); Text("Remove draft")
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove this dispute?") },
            text = { Text("This deletes the saved draft and tracking status from this device.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteDispute(dispute.id) }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MoreScreen(onNavigate: (Destination) -> Unit) {
    ScreenList {
        item { Hero("Every tool in one place", "Government complaint help, score planning, freezes, and private guidance.", Icons.Default.MoreHoriz) }
        item { ActionRow(Icons.Default.AccountBalance, "CFPB Assistant", "Build a complaint from a saved dispute") { onNavigate(Destination.CFPB) } }
        item { ActionRow(Icons.Default.AutoAwesome, "AI Assistant", "Ask questions using your analyzed report context") { onNavigate(Destination.ASSISTANT) } }
        item { ActionRow(Icons.Default.Timeline, "Score & action progress", "Separate immediate actions from longer-term effects") { onNavigate(Destination.PROGRESS) } }
        item { ActionRow(Icons.Default.Security, "Secondary bureau freezes", "Official links and confirmation checklist") { onNavigate(Destination.FREEZES) } }
        item { ActionRow(Icons.Default.Settings, "Privacy & settings", "Gateway status and local-data controls") { onNavigate(Destination.SETTINGS) } }
    }
}

@Composable
private fun CfpbScreen(data: AppState) {
    var selectedId by rememberSaveable { mutableStateOf(data.disputes.firstOrNull()?.id) }
    var activeStep by rememberSaveable { mutableIntStateOf(0) }
    val selected = data.disputes.firstOrNull { it.id == selectedId } ?: data.disputes.firstOrNull()
    val context = LocalContext.current
    val relatedFinding = data.findings.firstOrNull { it.id == selected?.findingId }
    var problem by rememberSaveable(selected?.id) { mutableStateOf(selected?.reason.orEmpty()) }
    var companyResponse by rememberSaveable(selected?.id) { mutableStateOf("") }
    var requestedResolution by rememberSaveable(selected?.id) {
        mutableStateOf("Correct or delete information that is inaccurate, incomplete, or cannot be verified, and provide an updated consumer report with a written explanation of the investigation result.")
    }
    var evidence by rememberSaveable(selected?.id) {
        mutableStateOf("Credit report pages showing the item; prior dispute and delivery proof; statements or records supporting the facts; response received, if any.")
    }
    var factsConfirmed by rememberSaveable(selected?.id) { mutableStateOf(false) }
    val now = System.currentTimeMillis()
    val cfpbReady = selected?.let { dispute ->
        dispute.status == DisputeStatus.RESOLVED ||
            (dispute.sentAt != null && dispute.craResponseDueAt?.let { it <= now } == true)
    } == true
    val steps = listOf(
        "Dispute with the reporting company first" to "Send a specific dispute to the bureau or furnisher and keep delivery proof. Do not file a CFPB credit-reporting complaint while that dispute is still pending.",
        "Wait for the response window" to "Continue after the company responds or after 45 days have passed. Record what was corrected, verified, or left unexplained.",
        "Confirm the exact problem" to "Edit the prefilled answers below so every statement matches your records. Do not include an SSN, full account number, or unnecessary medical details.",
        "Choose credit reporting" to "In the official CFPB form, select credit reporting and the company involved. Paste only the answer that corresponds to each prompt.",
        "Attach, submit, and track" to "Upload copies, keep originals, review the form, submit it yourself, and save the confirmation number for follow-up.",
    )
    ScreenList {
        item { Hero("CFPB complaint assistant", "A guided native workflow that prepares your draft without submitting anything for you.", Icons.Default.AccountBalance) }
        item {
            NoticeCard(Icons.Default.Warning, "You stay in control", "The app cannot promise an outcome and will never file a government complaint without your review and submission.", Amber400)
        }
        selected?.let { dispute ->
            item {
                NoticeCard(
                    if (cfpbReady) Icons.Default.Check else Icons.Default.Timeline,
                    if (cfpbReady) "CFPB preparation window reached" else "CFPB prerequisite not complete",
                    when {
                        dispute.status in listOf(DisputeStatus.DRAFT, DisputeStatus.READY) -> "First send the dispute to the reporting company and keep proof of delivery."
                        dispute.status == DisputeStatus.INVESTIGATING || dispute.status == DisputeStatus.SENT -> "The reporting dispute is pending. Wait for a response or until the recorded 45-day date before filing this complaint."
                        else -> "A response is recorded. Review it and describe precisely what remains unresolved."
                    },
                    if (cfpbReady) Emerald400 else Amber400,
                )
            }
        }
        if (data.disputes.isEmpty()) item { EmptyState("Create a dispute first", "The assistant uses a saved fact-specific dispute to prepare the CFPB narrative.") }
        if (data.disputes.isNotEmpty()) {
            item { SectionTitle("Choose a saved dispute") }
            items(data.disputes, key = { it.id }) { dispute ->
                FilterChip(
                    selected = selected?.id == dispute.id,
                    onClick = { selectedId = dispute.id },
                    label = { Text("${dispute.creditor} • ${dispute.bureaus.joinToString { it.displayName }}") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        item { SectionTitle("Step ${activeStep + 1} of ${steps.size}") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(steps[activeStep].first, fontWeight = FontWeight.Black, fontSize = 20.sp)
                    Text(steps[activeStep].second, color = Slate300, lineHeight = 21.sp)
                    LinearProgressIndicator(progress = { (activeStep + 1f) / steps.size }, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { activeStep = (activeStep - 1).coerceAtLeast(0) }, enabled = activeStep > 0) { Text("Back") }
                        Button(onClick = { activeStep = (activeStep + 1).coerceAtMost(steps.lastIndex) }, enabled = activeStep < steps.lastIndex) { Text("Next") }
                    }
                }
            }
        }
        selected?.let { dispute ->
            item { SectionTitle("Prefilled answers — edit and confirm") }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("What happened?", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = problem, onValueChange = { problem = it.take(4_000) }, modifier = Modifier.fillMaxWidth(), minLines = 4)
                        relatedFinding?.let {
                            Text("App evidence flag: ${it.title} — ${it.detail}", color = Slate400, fontSize = 11.sp)
                        }
                        Text("What did the company say or do?", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = companyResponse, onValueChange = { companyResponse = it.take(3_000) }, modifier = Modifier.fillMaxWidth(), minLines = 3, placeholder = { Text("Summarize the response or state that none was received.") })
                        Text("What resolution are you requesting?", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = requestedResolution, onValueChange = { requestedResolution = it.take(2_000) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        Text("Evidence checklist", fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = evidence, onValueChange = { evidence = it.take(2_000) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                        FilterChip(
                            selected = factsConfirmed,
                            onClick = { factsConfirmed = !factsConfirmed },
                            label = { Text(if (factsConfirmed) "Facts reviewed and confirmed" else "I must review every fact") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        val narrative = DisputeLetterGenerator.cfpbNarrative(
                            dispute = dispute,
                            problem = problem,
                            companyResponse = companyResponse,
                            requestedResolution = requestedResolution,
                            evidence = evidence,
                        )
                        OutlinedButton(
                            onClick = { context.copyText("CFPB complaint draft", narrative) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = factsConfirmed,
                        ) {
                            Icon(Icons.Default.ContentCopy, null); Spacer(Modifier.width(8.dp)); Text("Copy confirmed answers")
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick = { context.openUrl("https://www.consumerfinance.gov/complaint/") },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open official CFPB portal")
            }
        }
    }
}

@Composable
private fun AssistantScreen(ui: AppUiState, viewModel: MainViewModel) {
    val data = ui.data
    var input by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        NoticeCard(
            Icons.Default.Lock,
            if (ui.isAgentEndpointConfigured) "Secure agent endpoint configured" else "Secure agent setup incomplete",
            if (ui.isAgentEndpointConfigured) "Questions use a redacted case snapshot. A short-lived signed-in session is required; model and legal-library keys never live in the APK." else "No case data will leave this device until the authenticated AI gateway is configured.",
            if (ui.isAgentEndpointConfigured) Emerald400 else Amber400,
            modifier = Modifier.padding(16.dp),
        )
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(data.chat, key = { it.id }) { message ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                    Surface(
                        color = if (message.fromUser) Blue500 else Navy800,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth(.86f),
                    ) {
                        Text(message.text, modifier = Modifier.padding(13.dp), color = if (message.fromUser) Color.White else Slate300, lineHeight = 20.sp)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Navy900).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("What should I do first?") },
                modifier = Modifier.weight(1f),
                maxLines = 3,
                enabled = !ui.isAssistantThinking,
            )
            FloatingActionButton(
                onClick = { viewModel.askAssistant(input); input = "" },
                containerColor = Blue500,
            ) {
                if (ui.isAssistantThinking) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Icon(Icons.AutoMirrored.Filled.Send, "Send")
            }
        }
    }
}

private data class FreezeCompany(val name: String, val description: String, val url: String)

@Composable
private fun FreezesScreen(data: AppState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val companies = listOf(
        FreezeCompany("LexisNexis Risk Solutions", "Consumer disclosure and security-freeze controls.", "https://consumer.risk.lexisnexis.com/freeze"),
        FreezeCompany("SageStream", "Consumer-report request and security-freeze controls now handled through LexisNexis Risk Solutions.", "https://consumer.risk.lexisnexis.com/request"),
        FreezeCompany("Innovis", "Security freeze for the Innovis consumer-reporting file.", "https://www.innovis.com/securityFreeze/index"),
        FreezeCompany("ChexSystems", "Security freeze for checking-account consumer reports.", "https://www.chexsystems.com/security-freeze/place-freeze"),
        FreezeCompany("NCTUE", "Telecom, pay-TV, and utility consumer-report controls.", "https://nctue.com/consumers/"),
    )
    ScreenList {
        item { Hero("Secondary bureau freezes", "Use official sites, then record confirmations in your private checklist.", Icons.Default.Security) }
        item { NoticeCard(Icons.Default.Warning, "Know the tradeoff", "A freeze does not delete accurate data and can delay legitimate applications. Unfreeze when access is needed.", Amber400) }
        items(companies, key = { it.name }) { company ->
            val checked = company.name in data.frozenBureaus
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (checked) Icons.Default.Lock else Icons.Default.Security, null, tint = if (checked) Amber400 else Cyan400)
                        Spacer(Modifier.width(10.dp))
                        Text(company.name, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    }
                    Text(company.description, color = Slate400, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { context.openUrl(company.url) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.OpenInNew, null); Spacer(Modifier.width(5.dp)); Text("Official site")
                        }
                        Button(onClick = { viewModel.toggleFreeze(company.name) }, modifier = Modifier.weight(1f)) {
                            Icon(if (checked) Icons.Default.Check else Icons.Default.Add, null); Spacer(Modifier.width(5.dp)); Text(if (checked) "Confirmed" else "Mark done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(data: AppState, viewModel: MainViewModel) {
    var bureau by rememberSaveable { mutableStateOf(Bureau.TRANSUNION) }
    var scoreText by rememberSaveable { mutableStateOf("") }
    val stages = CreditAnalyzer.impactPlan(data.findings, data.disputes)
    ScreenList {
        item { Hero("Score & action progress", "See what can happen soon, what normally takes time, and what cannot be promised.", Icons.Default.Timeline) }
        item { SectionTitle("Record a score") }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(Bureau.TRANSUNION, Bureau.EXPERIAN, Bureau.EQUIFAX).forEach { item ->
                            FilterChip(selected = bureau == item, onClick = { bureau = item }, label = { Text(item.displayName.take(2).uppercase()) }, modifier = Modifier.weight(1f))
                        }
                    }
                    OutlinedTextField(value = scoreText, onValueChange = { scoreText = it.filter(Char::isDigit).take(3) }, label = { Text("Score (300–850)") }, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { scoreText.toIntOrNull()?.let { viewModel.recordScore(bureau, it); scoreText = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Save score") }
                }
            }
        }
        item { SectionTitle("Realistic effect windows") }
        items(stages) { stage ->
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stage.window, color = Cyan400, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    Text(stage.title, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text(stage.expectation, color = Slate300, lineHeight = 20.sp)
                    Text(stage.positiveInfluence, color = Amber400, fontSize = 12.sp)
                }
            }
        }
        if (data.scores.isNotEmpty()) {
            item { SectionTitle("Score history") }
            items(data.scores.sortedByDescending { it.recordedAt }.take(20)) { snapshot -> ScoreRow(snapshot) }
        }
    }
}

@Composable
private fun ScoreRow(snapshot: ScoreSnapshot) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(snapshot.bureau.displayName, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(DateFormat.getDateInstance(DateFormat.SHORT).format(Date(snapshot.recordedAt)), color = Slate400, fontSize = 12.sp)
            Spacer(Modifier.width(14.dp))
            Text(snapshot.score.toString(), color = Cyan400, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
private fun SettingsScreen(ui: AppUiState, viewModel: MainViewModel) {
    val data = ui.data
    var confirmClear by remember { mutableStateOf(false) }
    var jurisdictionCode by remember(data.jurisdictionCode) { mutableStateOf(data.jurisdictionCode) }
    ScreenList {
        item { Hero("Privacy & settings", "Control local credit data and review connection status.", Icons.Default.Settings) }
        item { NoticeCard(Icons.Default.Check, "Native Android build", "Kotlin + Jetpack Compose. No WebView, Capacitor, or wrapped website is used for the app interface.", Emerald400) }
        item { NoticeCard(Icons.Default.Security, "Protected local storage", "Case state is encrypted with AES-GCM using a non-exportable Android Keystore key. Screenshots are blocked and Android backup is disabled.", Emerald400) }
        item {
            NoticeCard(
                Icons.Default.AutoAwesome,
                if (ui.isAgentEndpointConfigured) "Legal agent endpoint: configured" else "Legal agent endpoint: not connected",
                if (ui.isAgentEndpointConfigured) "The APK has a gateway URL but still requires a short-lived authenticated user session before any redacted case context can be sent." else "On-device parsing and deterministic review flags work. Interactive legal-agent answers remain disabled so the app never falls back to pretending rules are AI.",
                if (ui.isAgentEndpointConfigured) Emerald400 else Amber400,
            )
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("State context", fontWeight = FontWeight.Black)
                    Text(
                        "Enter the user’s state or DC. The agent uses federal authority plus only state provisions that have been individually verified.",
                        color = Slate400,
                        fontSize = 12.sp,
                    )
                    OutlinedTextField(
                        value = jurisdictionCode,
                        onValueChange = { value ->
                            jurisdictionCode = value.filter(Char::isLetter).uppercase(Locale.US).take(2)
                        },
                        label = { Text("Two-letter state or DC code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { viewModel.updateJurisdiction(jurisdictionCode) },
                        enabled = jurisdictionCode.length == 2,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save state context") }
                    if (data.jurisdictionCode.isNotBlank()) {
                        Text(
                            "Saved: ${data.jurisdictionCode}. Current state library coverage is source-registry only; federal guidance remains primary.",
                            color = Amber400,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        item {
            val context = LocalContext.current
            OutlinedButton(
                onClick = { viewModel.signOut(context) },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose400),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Sign out and remove local data") }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Stored on this device", fontWeight = FontWeight.Black)
                    Text("${data.reports.size} reports • ${data.accounts.size} account entries • ${data.disputes.size} disputes • ${data.scores.size} score records", color = Slate400)
                    OutlinedButton(onClick = { confirmClear = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose400), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete all local data")
                    }
                }
            }
        }
        item {
            Text(
                "CreditRepairAI provides educational organization and drafting assistance, not legal advice, credit-repair guarantees, or lender decisions.",
                color = Slate400,
                fontSize = 12.sp,
            )
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete all local data?") },
            text = { Text("Reports, parsed entries, findings, disputes, score history, freeze checklist, and assistant chat will be removed.") },
            confirmButton = { TextButton(onClick = { confirmClear = false; viewModel.clearData() }) { Text("Delete everything", color = Rose400) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MetricCard(modifier: Modifier, label: String, value: String, caption: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Navy900), modifier = modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, color = Slate400, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(caption, color = Slate400, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Navy900),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Blue500.copy(.15f), shape = RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = Cyan400, modifier = Modifier.padding(11.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Slate400, fontSize = 12.sp)
            }
            Icon(Icons.Default.OpenInNew, null, tint = Slate400, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun NoticeCard(icon: ImageVector, title: String, text: String, color: Color, modifier: Modifier = Modifier) {
    OutlinedCard(border = BorderStroke(1.dp, color.copy(.35f)), modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(text, color = Slate400, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, text: String) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Description, null, tint = Slate400, modifier = Modifier.size(34.dp))
            Text(title, fontWeight = FontWeight.Black)
            Text(text, color = Slate400, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text.uppercase(), color = Slate400, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.2.sp)
}

@Composable
private fun StatusPill(status: DisputeStatus) {
    val color = when (status) {
        DisputeStatus.DRAFT -> Slate400
        DisputeStatus.READY -> Cyan400
        DisputeStatus.SENT, DisputeStatus.INVESTIGATING -> Amber400
        DisputeStatus.RESOLVED -> Emerald400
    }
    Surface(color = color.copy(.15f), shape = CircleShape) {
        Text(status.name, color = color, fontWeight = FontWeight.Black, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun LegalAccuracyNote() {
    NoticeCard(
        Icons.Default.Gavel,
        "Accuracy before action",
        "Dispute only information you genuinely believe is inaccurate or incomplete. Verify citations and facts; this app is educational and is not a law firm.",
        Amber400,
    )
}

private fun Context.displayName(uri: Uri): String {
    val fromQuery = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
    return fromQuery ?: uri.lastPathSegment ?: "Credit-report.pdf"
}

private fun Context.copyText(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun Context.shareText(subject: String, text: String) {
    startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, text)
            },
            "Share draft",
        ),
    )
}

private fun Context.openUrl(url: String) {
    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}
