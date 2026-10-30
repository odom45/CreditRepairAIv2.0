package com.creditrepairai.v2.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Tests use callbacks only: no real browser, document upload, payment or mailing. */
class PaidMailServiceInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val consent = "I understand mailing is a separate paid purchase with LetterStream"
    private val checkout = "Continue to LetterStream — paid mailing"

    @Test fun checkoutRequiresSavedReviewedPdfAndSeparatePurchaseConsent() {
        val saved = mutableStateOf(false)
        val reviewed = mutableStateOf(false)
        var opens = 0
        compose.setContent { CreditRepairTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PaidMailServiceCard("Fictional letter", reviewed.value, saved.value) { opens++ }
            }
        } }
        compose.onNodeWithText(consent).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText(checkout).performScrollTo().assertIsNotEnabled()
        compose.runOnIdle { saved.value = true }
        compose.onNodeWithText(checkout).assertIsNotEnabled()
        compose.runOnIdle { reviewed.value = true }
        compose.onNodeWithText(checkout).assertIsNotEnabled()
        compose.onNodeWithText(consent).performScrollTo().performClick()
        compose.onNodeWithText(checkout).performScrollTo().assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, opens) }
    }

    @Test fun editingLetterOrRevokingReviewRequiresFreshConsent() {
        val revision = mutableStateOf("Fictional letter A")
        val reviewed = mutableStateOf(true)
        val saved = mutableStateOf(true)
        compose.setContent { CreditRepairTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PaidMailServiceCard(revision.value, reviewed.value, saved.value) { error("Must not open a real service") }
            }
        } }
        compose.onNodeWithText(consent).performScrollTo().performClick()
        compose.onNodeWithText(checkout).performScrollTo().assertIsEnabled()
        compose.runOnIdle { revision.value = "Fictional letter B"; saved.value = false }
        compose.onNodeWithText(checkout).assertIsNotEnabled()
        compose.runOnIdle { saved.value = true }
        compose.onNodeWithText(checkout).assertIsNotEnabled()
        compose.onNodeWithText(consent).performScrollTo().performClick()
        compose.onNodeWithText(checkout).performScrollTo().assertIsEnabled()
        compose.runOnIdle { reviewed.value = false }
        compose.onNodeWithText(checkout).assertIsNotEnabled()
        compose.runOnIdle { reviewed.value = true }
        compose.onNodeWithText(checkout).assertIsNotEnabled()
    }
}
