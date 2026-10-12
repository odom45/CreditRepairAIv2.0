package com.creditrepairai.v2.ui

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.creditrepairai.v2.model.AppState
import com.creditrepairai.v2.guidance.AcquisitionStep
import com.creditrepairai.v2.ui.theme.CreditRepairTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class AcquisitionInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    @Test fun checklistReachesVerifyAndImportIsAnExplicitAction() {
        var data by mutableStateOf(AppState()); var imports = 0
        compose.setContent { CreditRepairTheme { AcquisitionScreen(data, { data = data.copy(acquisition = it) }, { imports++ }) } }
        compose.runOnIdle { assertEquals(0, imports) }
        repeat(4) { compose.onNodeWithText("Next step").performScrollTo().performClick() }
        compose.onNodeWithText("Next step").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(AcquisitionStep.VERIFY, data.acquisition.step) }
        compose.onNodeWithText("Import downloaded PDF").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, imports) }
    }
}
