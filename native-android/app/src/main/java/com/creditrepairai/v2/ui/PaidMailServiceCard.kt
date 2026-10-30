package com.creditrepairai.v2.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A user-controlled external purchase: no document, identity or payment data is attached to the URL. */
internal const val PAID_MAIL_SERVICE_URL = "https://www.letterstream.com/creditrepairmail/"

@Composable
internal fun PaidMailServiceCard(
    letterRevision: String?,
    reviewed: Boolean,
    currentPdfSaved: Boolean,
    onOpenService: () -> Unit,
) {
    var consent by remember(letterRevision, reviewed, currentPdfSaved) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Have my letter printed and mailed", style = MaterialTheme.typography.titleLarge)
            Text("Optional paid service through LetterStream. Downloading and printing your own letter remains included with your app access.")
            Text("1. Save the current bureau letter PDF above. Complete any required signature, bureau form, identity copies and supporting documents before uploading your final packet.")
            Text("2. Open LetterStream, create or sign in to your provider account, upload your packet and verify both addresses and every page in the mailing proof.")
            Text("3. Choose First-Class Mail, Certified Mail with tracking, or Certified Mail with an electronic return receipt. Review the exact total and pay LetterStream separately. Fees vary by pages and service; your app subscription does not cover mailing.")
            Text("4. After the provider confirms mailing, return here to record mailing and tracking. Opening checkout or paying does not confirm that the letter has been mailed or delivered.")
            Text("You choose what to upload. CreditRepairAI sends no PDF, name, address, account number or payment details when opening this website. Documents you upload are shared with LetterStream under its terms and privacy policy.")
            if (!currentPdfSaved) Text("Save the current letter PDF before continuing. Editing the letter requires a new save and review.")
            FilterChip(
                selected = consent,
                onClick = { consent = !consent },
                enabled = reviewed && currentPdfSaved && letterRevision != null,
                label = { Text("I understand mailing is a separate paid purchase with LetterStream") },
            )
            Button(
                onClick = onOpenService,
                enabled = reviewed && currentPdfSaved && letterRevision != null && consent,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue to LetterStream — paid mailing") }
        }
    }
}
