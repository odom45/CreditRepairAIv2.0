package com.creditrepairai.v2.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.creditrepairai.v2.guidance.CreditGoal
import com.creditrepairai.v2.model.AppState
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Run only on a disposable debug installation containing fictional test records. */
@RunWith(AndroidJUnit4::class)
class AppRepositoryInstrumentedTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val repository = AppRepository(context)
    @After fun cleanTestRecords() { repository.clear(preserveOwner = false) }
    @Test fun clearingThenSavingPreservesNewCaseAcrossRestart() {
        repository.selectOwner("fictional-owner-A")
        repository.save(AppState(goal = CreditGoal("Fictional old goal")))
        repository.clear()
        repository.save(AppState(goal = CreditGoal("Fictional new goal")))
        val restarted = AppRepository(context)
        restarted.selectOwner("fictional-owner-A")
        assertEquals("Fictional new goal", restarted.load().goal.description)
    }
    @Test fun otherOwnerCannotReadCaseAndCiphertextHasNoGoalText() {
        repository.selectOwner("fictional-owner-A")
        repository.save(AppState(goal = CreditGoal("Fictional private marker")))
        val prefs = context.getSharedPreferences("credit_repair_private", android.content.Context.MODE_PRIVATE)
        assertFalse(prefs.all.values.joinToString().contains("Fictional private marker"))
        repository.selectOwner("fictional-owner-B")
        assertEquals("", repository.load().goal.description)
    }
}
