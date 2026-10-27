package com.creditrepairai.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.Dispute

/** Response summaries stay in memory until the user explicitly asks the consent-gated agent. */
@Composable
internal fun ResultExplanationDialog(dispute: Dispute, onDismiss: () -> Unit, onExplain: (String) -> Unit) {
    val bureaus = dispute.bureaus.filter { it != Bureau.UNKNOWN }.distinct()
    var bureau by remember(dispute.id) { mutableStateOf(bureaus.firstOrNull()) }
    var response by remember(dispute.id, bureau) { mutableStateOf("") }
    var records by remember(dispute.id, bureau) { mutableStateOf("") }
    var checked by remember(dispute.id, bureau, response, records) { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Explain my result") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Summarize the actual response for ${dispute.creditor}. Remove names, account numbers, addresses and other identifiers. This does not mark an item corrected or a dispute resolved.")
                bureaus.forEach { item -> FilterChip(bureau == item, { bureau = item }, label = { Text(item.displayName) }) }
                OutlinedTextField(response, { response = it.take(1800) }, label = { Text("What the bureau actually said") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(records, { records = it.take(600) }, label = { Text("What my supporting records show (optional)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                FilterChip(checked, { checked = !checked }, label = { Text("I checked the response and removed identifiers") })
                Text("The assistant can explain the reported result, identify missing facts and suggest a next action. AI sharing still requires your separate consent. Verify guidance against the response and your records.")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = {
            TextButton(enabled = checked && response.isNotBlank() && bureau != null, onClick = {
                onExplain("Explain my dispute result in plain language. Treat this as my unverified summary, not proof of an error or a successful correction. Bureau: ${bureau?.displayName}. Item: ${dispute.creditor.take(160)}. Original issue: ${dispute.reason.take(500)}. Actual response: $response. Supporting records: ${records.ifBlank { "Not supplied" }}. Explain what the response says and does not establish, ask one focused question if needed, and suggest one evidence-based next step. Retrieve official sources for legal claims. Do not invent facts, mark a status, or promise an outcome.")
            }) { Text("Ask AI about this result") }
        },
    )
}
