package com.creditrepairai.v2

import android.content.pm.ApplicationInfo
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.*
import org.junit.Test

class ActivitySafetyInstrumentedTest {
    @Test fun actualLauncherKeepsScreensSecureAndBackupsDisabled() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                assertEquals(0, activity.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
                assertEquals(0, activity.applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC)
            }
        }
    }
}
