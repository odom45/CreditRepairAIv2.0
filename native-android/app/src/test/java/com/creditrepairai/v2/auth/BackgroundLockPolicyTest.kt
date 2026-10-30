package com.creditrepairai.v2.auth

import org.junit.Assert.*
import org.junit.Test

class BackgroundLockPolicyTest {
    @Test fun ordinaryBrowserLaunchDoesNotDisableBackgroundLock() {
        val policy = BackgroundLockPolicy()
        policy.started(-1) // Android startActivity uses this sentinel and never returns a result.
        assertTrue(policy.shouldLock(false, false))
        policy.started(-1)
        assertTrue(policy.shouldLock(false, false))
    }
    @Test fun documentPickerCanReturnBeforeTheNextBackgroundLocks() {
        val policy = BackgroundLockPolicy()
        policy.started(12345)
        assertFalse(policy.shouldLock(false, false))
        policy.finished(12345)
        assertTrue(policy.shouldLock(false, false))
    }
    @Test fun onlyRotationAndExplicitGoogleAuthenticationAreOtherContinuations() {
        val policy = BackgroundLockPolicy()
        assertFalse(policy.shouldLock(true, false))
        assertFalse(policy.shouldLock(false, true))
        assertTrue(policy.shouldLock(false, false))
    }
}
