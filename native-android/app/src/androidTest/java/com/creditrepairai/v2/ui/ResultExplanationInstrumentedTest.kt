package com.creditrepairai.v2.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.creditrepairai.v2.model.Bureau
import com.creditrepairai.v2.model.Dispute
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ResultExplanationInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun actualResponseAndPrivacyReviewAreRequiredAndBureauChangeResetsThem() {
        val dispute = Dispute(findingId = "fictional", creditor = "Fictional Bank", bureaus = listOf(Bureau.EQUIFAX, Bureau.EXPERIAN), reason = "Fictional balance difference", letter = "Fictional draft")
        var question: String? = null
        compose.setContent { CreditRepairTheme { ResultExplanationDialog(dispute, {}) { question = it } } }
        compose.onNodeWithText("Ask AI about this result").assertIsNotEnabled()
        compose.onNodeWithText("What the bureau actually said").performScrollTo().performTextInput("The balance was verified. The response did not explain the dated payment.")
        compose.onNodeWithText("Ask AI about this result").assertIsNotEnabled()
        compose.onNodeWithText("I checked the response and removed identifiers").performScrollTo().performClick()
        compose.onNodeWithText("Ask AI about this result").assertIsEnabled()
        compose.onNodeWithText("Experian").performScrollTo().performClick()
        compose.onNodeWithText("Ask AI about this result").assertIsNotEnabled()
        compose.onNodeWithText("What the bureau actually said").performScrollTo().performTextInput("The item was corrected in this bureau's response.")
        compose.onNodeWithText("I checked the response and removed identifiers").performScrollTo().performClick()
        compose.onNodeWithText("Ask AI about this result").performClick()
        compose.runOnIdle {
            val sent = requireNotNull(question)
            assertTrue(sent.contains("Bureau: Experian")); assertTrue(sent.contains("item was corrected"))
            assertFalse(sent.contains("balance was verified")); assertTrue(sent.length <= 4000)
            assertTrue(sent.contains("Do not invent facts")); assertEquals(com.creditrepairai.v2.model.DisputeStatus.DRAFT, dispute.status)
        }
    }
}
