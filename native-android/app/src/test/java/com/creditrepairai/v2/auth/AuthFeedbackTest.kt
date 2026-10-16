package com.creditrepairai.v2.auth

import org.junit.Assert.*
import org.junit.Test

class AuthFeedbackTest {
    @Test fun credentialFailuresDoNotRevealWhetherAnEmailExists() {
        val codes = listOf("ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND")
        assertEquals(1, codes.map(AuthFeedback::message).toSet().size)
        assertTrue(AuthFeedback.message(codes.first()).contains("AUTH_CREDENTIALS"))
    }
    @Test fun configurationAndProviderFailuresHaveDifferentActions() {
        assertTrue(AuthFeedback.message("ERROR_APP_NOT_AUTHORIZED").contains("AUTH_APP_CONFIGURATION"))
        assertTrue(AuthFeedback.message("INVALID_CERT_HASH").contains("AUTH_APP_CONFIGURATION"))
        assertTrue(AuthFeedback.message("ERROR_OPERATION_NOT_ALLOWED").contains("AUTH_PROVIDER_DISABLED"))
        assertTrue(AuthFeedback.message("ERROR_NETWORK_REQUEST_FAILED").contains("AUTH_NETWORK"))
    }
    @Test fun unknownExceptionsNeverExposeProviderDetails() {
        val secret = "fictional-user@example.invalid fictional-provider-token"
        assertFalse(AuthFeedback.failureMessage(IllegalStateException(secret)).contains(secret))
        assertFalse(AuthFeedback.failureMessage(AuthFailure(secret)).contains(secret))
        assertTrue(AuthFeedback.failureMessage(AuthFailure(secret)).contains("AUTH_UNKNOWN"))
    }
    @Test fun cancellationAndMissingGoogleAccountsRemainDistinct() {
        assertTrue(AuthFeedback.message("GOOGLE_CANCELED").contains("canceled"))
        assertTrue(AuthFeedback.message("GOOGLE_NO_CREDENTIAL").contains("AUTH_GOOGLE_ACCOUNT"))
        assertTrue(AuthFeedback.message("GOOGLE_UNAVAILABLE").contains("AUTH_GOOGLE"))
    }
    @Test fun verificationDeliveryFailureDoesNotSayAccountCreationFailed() {
        val message = AuthFeedback.message("VERIFICATION_SEND_FAILED")
        assertTrue(message.contains("account was created"))
        assertTrue(message.contains("Resend verification email"))
    }
}
