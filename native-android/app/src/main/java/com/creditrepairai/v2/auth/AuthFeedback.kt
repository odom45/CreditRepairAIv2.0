package com.creditrepairai.v2.auth

/** Only allowlisted codes reach the UI; provider messages can contain account details. */
class AuthFailure(val code: String) : Exception(AuthFeedback.message(code))

object AuthFeedback {
    fun message(code: String): String = when (code) {
        "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND" ->
            "The email or password could not be verified. Try again, reset your password, or use your original sign-in method. (AUTH_CREDENTIALS)"
        "ERROR_INVALID_EMAIL" -> "Enter a valid email address. (AUTH_EMAIL)"
        "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "Use the sign-in method you originally used for this account, or reset its password. (AUTH_EXISTING_ACCOUNT)"
        "ERROR_WEAK_PASSWORD", "ERROR_PASSWORD_DOES_NOT_MEET_REQUIREMENTS" ->
            "Choose a stronger password that meets this project's password requirements. (AUTH_PASSWORD_POLICY)"
        "ERROR_OPERATION_NOT_ALLOWED", "ERROR_PASSWORD_LOGIN_DISABLED" ->
            "This sign-in method is unavailable. Contact app support with code AUTH_PROVIDER_DISABLED."
        "ERROR_APP_NOT_AUTHORIZED", "ERROR_INVALID_API_KEY", "INVALID_CERT_HASH", "ERROR_INVALID_CERT_HASH", "ERROR_INVALID_OAUTH_CLIENT_ID" ->
            "This app build is not authorized for sign-in. Contact app support with code AUTH_APP_CONFIGURATION."
        "ERROR_USER_DISABLED" -> "This account is unavailable. Contact app support with code AUTH_ACCOUNT_DISABLED."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait before trying again. (AUTH_RATE_LIMIT)"
        "ERROR_NETWORK_REQUEST_FAILED" -> "Could not reach the sign-in service. Check your connection and try again. (AUTH_NETWORK)"
        "ERROR_REQUIRES_RECENT_LOGIN", "ERROR_USER_TOKEN_EXPIRED", "ERROR_INVALID_USER_TOKEN" ->
            "Sign out, then sign in again before retrying this action. (AUTH_SIGN_IN_AGAIN)"
        "GOOGLE_CANCELED" -> "Google sign-in was canceled. You can try again."
        "GOOGLE_NO_CREDENTIAL" -> "No Google credential is available. Add a Google account on this device or use email sign-in. (AUTH_GOOGLE_ACCOUNT)"
        "GOOGLE_UNAVAILABLE" -> "Google sign-in is unavailable. Check your connection and Google Play services, then retry. Contact support with code AUTH_GOOGLE if it continues."
        "VERIFICATION_SEND_FAILED" -> "Your account was created, but the verification email could not be sent. Use Resend verification email in Settings. (AUTH_VERIFICATION)"
        else -> "Sign-in could not complete. Try again or contact app support with code AUTH_UNKNOWN."
    }

    fun failureMessage(error: Throwable): String = (error as? AuthFailure)?.message ?: message("UNKNOWN")
}
