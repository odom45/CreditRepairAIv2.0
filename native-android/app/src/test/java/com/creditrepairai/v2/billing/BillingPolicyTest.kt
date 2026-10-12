package com.creditrepairai.v2.billing
import org.junit.Assert.*
import org.junit.Test
class BillingPolicyTest {
    @Test fun pendingAndUnverifiedPurchasesNeverUnlock() {
        assertEquals(PurchaseDisposition.PENDING, BillingPolicy.disposition(true, false, true))
        assertEquals(PurchaseDisposition.VERIFY, BillingPolicy.disposition(true, true, false))
        assertEquals(PurchaseDisposition.IGNORE, BillingPolicy.disposition(false, true, false))
        assertEquals(PurchaseDisposition.IGNORE, BillingPolicy.disposition(true, false, false))
        assertFalse(Entitlement().hasAccess(0))
        assertFalse(Entitlement(true, 100).hasAccess(100))
        assertFalse(Entitlement(false, 100).hasAccess(1))
        assertTrue(Entitlement(true, 100).hasAccess(99))
    }
    @Test fun obfuscatedAccountIdMatchesServerSha256() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", BillingPolicy.accountId("abc"))
    }
}
