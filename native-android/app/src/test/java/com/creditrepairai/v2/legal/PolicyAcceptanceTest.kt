package com.creditrepairai.v2.legal

import org.junit.Assert.*
import org.junit.Test

class PolicyAcceptanceTest {
    @Test fun agreementDoesNotSilentlyEnableAi() {
        val accepted = PolicyAcceptance.accept(true, true, 100)
        assertTrue(accepted.isCurrent)
        assertFalse(accepted.allowsAi)
        assertTrue(accepted.chooseAi(true, 101).allowsAi)
    }
    @Test fun alteredDocumentsOrOldVersionRequireNewAcceptance() {
        val accepted = PolicyAcceptance.accept(true, true, 100).chooseAi(true, 101)
        assertFalse(accepted.copy(version = "old").isCurrent)
        assertFalse(accepted.copy(digest = "different").allowsAi)
    }
    @Test fun rapidOrBackwardClockChoicesStillInvalidateOlderRequestOrder() {
        val enabled = PolicyAcceptance.accept(true, true, 100).chooseAi(true, 101)
        val revoked = enabled.chooseAi(false, 50)
        assertFalse(revoked.allowsAi)
        assertTrue(revoked.choiceAt > enabled.choiceAt)
    }
    @Test(expected = IllegalArgumentException::class) fun noAssentCannotCreateAcceptance() { PolicyAcceptance.accept(false, true, 100) }
    @Test(expected = IllegalArgumentException::class) fun noAdultConfirmationCannotCreateAcceptance() { PolicyAcceptance.accept(true, false, 100) }
}
