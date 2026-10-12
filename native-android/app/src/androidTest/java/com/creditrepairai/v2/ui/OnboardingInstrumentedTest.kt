package com.creditrepairai.v2.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun onboardingHasNoDeadEndAndAllFiveStepsAreReachable() {
        var complete = false
        compose.setContent { CreditRepairTheme { OnboardingScreen({ complete = true }, {}) } }
        repeat(4) { compose.onNodeWithText("Continue").performScrollTo().performClick() }
        compose.onNodeWithText("I understand — continue").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(complete) }
    }
}
