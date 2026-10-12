package com.creditrepairai.v2.auth

import org.junit.Assert.*
import org.junit.Test

class SessionFenceTest {
    @Test fun deletionRejectsOldRepliesEvenForSameSignedInUser() {
        val fence = SessionFence("ownerA")
        val pendingAi = fence.ticket()
        val queuedImport = fence.ticket()
        fence.invalidate()
        assertFalse(fence.accepts(pendingAi, "ownerA"))
        assertFalse(fence.accepts(queuedImport, "ownerA"))
        assertTrue(fence.accepts(fence.ticket(), "ownerA"))
    }
    @Test fun accountSwitchAndReloginDoNotReuseTickets() {
        val fence = SessionFence("ownerA")
        val billingReply = fence.ticket()
        fence.invalidate("ownerB")
        assertFalse(fence.accepts(billingReply, "ownerB"))
        assertFalse(fence.accepts(billingReply, "ownerA"))
        fence.invalidate("ownerA")
        assertFalse(fence.accepts(billingReply, "ownerA"))
        assertTrue(fence.accepts(fence.ticket(), "ownerA"))
    }
    @Test fun unsignedOrChangedSessionCannotCommit() {
        val fence = SessionFence(null)
        assertFalse(fence.accepts(fence.ticket(), null))
        fence.invalidate("ownerA")
        assertFalse(fence.accepts(fence.ticket(), null))
        assertFalse(fence.accepts(fence.ticket(), "ownerB"))
    }
}
