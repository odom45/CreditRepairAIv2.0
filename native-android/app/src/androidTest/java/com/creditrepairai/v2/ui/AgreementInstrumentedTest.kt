package com.creditrepairai.v2.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AgreementInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun agreementControlsRemainReachableAtDoubleFontScale() {
        compose.setContent { CreditRepairTheme {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) { AgreementScreen({ _, _ -> }, {}, {}) }
        } }
        compose.onNodeWithText("I agree to the displayed Terms of Use and acknowledge the Privacy Policy and Educational Disclaimer").performScrollTo().performClick()
        compose.onNodeWithText("I am at least 18 and will use records I own or have lawful permission to use").performScrollTo().performClick()
        compose.onNodeWithText("I agree — continue").performScrollTo().assertIsEnabled().assertIsDisplayed()
    }
    @Test fun uncheckedChoicesCannotAcceptAndReadDoesNotAccept() {
        var accepted = false; var opened = false
        compose.setContent { CreditRepairTheme { AgreementScreen({ _, _ -> accepted = true }, { opened = true }, {}) } }
        compose.onNodeWithText("I agree — continue").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Read full terms, privacy & disclaimer").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(opened); assertFalse(accepted) }
        compose.onNodeWithText("I agree to the displayed Terms of Use and acknowledge the Privacy Policy and Educational Disclaimer").performScrollTo().performClick()
        compose.onNodeWithText("I agree — continue").assertIsNotEnabled()
        compose.onNodeWithText("I am at least 18 and will use records I own or have lawful permission to use").performScrollTo().performClick()
        compose.onNodeWithText("I agree — continue").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(accepted) }
    }
    @Test fun decliningAiDoesNotCallTheConsentGrantCallback() {
        var granted: Boolean? = null; var dismissed = false
        compose.setContent { CreditRepairTheme { AiSharingDisclosureDialog(false, { granted = it }, { dismissed = true }) } }
        compose.onNodeWithText("Keep AI off").performClick()
        compose.runOnIdle { assertTrue(dismissed); assertNull(granted) }
    }
    @Test fun aiGrantRequiresExplicitAction() {
        var granted = false
        compose.setContent { CreditRepairTheme { AiSharingDisclosureDialog(false, { granted = it }, {}) } }
        compose.runOnIdle { assertFalse(granted) }
        compose.onNodeWithText("Allow AI sharing").performClick()
        compose.runOnIdle { assertTrue(granted) }
    }
}
