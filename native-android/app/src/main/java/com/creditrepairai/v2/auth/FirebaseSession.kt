package com.creditrepairai.v2.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.creditrepairai.v2.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException

/** Owns Firebase's short-lived user session; no token is persisted by the app. */
class FirebaseSession(
    private val auth: FirebaseAuth = Firebase.auth,
) : NativeSession {
    override val email: String? get() = auth.currentUser?.email
    override val uid: String? get() = auth.currentUser?.uid
    override val isEmailVerified: Boolean get() = auth.currentUser?.isEmailVerified == true
    val isSignedIn: Boolean get() = auth.currentUser != null

    override suspend fun bearerToken(expectedSubject: String): String? {
        val user = auth.currentUser?.takeIf { it.uid == expectedSubject } ?: return null
        val token = user.getIdToken(false).await().token
        return token.takeIf { auth.currentUser?.uid == expectedSubject }
    }

    override fun observeSession(onChange: (String?) -> Unit): () -> Unit {
        val listener = FirebaseAuth.AuthStateListener { onChange(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        return { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signInWithEmail(email: String, password: String) = authAction {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        Unit
    }

    override suspend fun createAccount(email: String, password: String) {
        authAction { auth.createUserWithEmailAndPassword(email.trim(), password).await() }
        try { resendVerification() }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            throw AuthFailure("VERIFICATION_SEND_FAILED")
        }
    }

    override suspend fun sendPasswordReset(email: String) = authAction {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    override suspend fun resendVerification() = authAction {
        val user = auth.currentUser ?: throw AuthFailure("ERROR_REQUIRES_RECENT_LOGIN")
        if (!user.isEmailVerified) user.sendEmailVerification().await()
        Unit
    }

    override suspend fun refreshUser() = authAction {
        val user = auth.currentUser ?: throw AuthFailure("ERROR_REQUIRES_RECENT_LOGIN")
        val owner = user.uid
        user.reload().await()
        if (auth.currentUser?.uid != owner) throw AuthFailure("ERROR_REQUIRES_RECENT_LOGIN")
        // A cached ID token may still contain email_verified:false after the link is opened.
        user.getIdToken(true).await()
        Unit
    }

    override suspend fun deleteAccount() {
        val user = requireNotNull(auth.currentUser)
        // Firebase requires recent authentication for this sensitive operation.
        user.delete().await()
    }

    override suspend fun signInWithGoogle(activity: Activity) = authAction {
        val googleIdOption = GetSignInWithGoogleOption.Builder(activity.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        val credential = try {
            CredentialManager.create(activity).getCredential(activity, request).credential
        } catch (_: NoCredentialException) {
            throw AuthFailure("GOOGLE_NO_CREDENTIAL")
        } catch (_: GetCredentialCancellationException) {
            throw AuthFailure("GOOGLE_CANCELED")
        } catch (_: GetCredentialException) {
            throw AuthFailure("GOOGLE_UNAVAILABLE")
        }
        require(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            "Google did not return a valid sign-in credential."
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun signOut(context: Context) {
        auth.signOut()
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private suspend fun <T> authAction(action: suspend () -> T): T = try { action() }
    catch (error: Exception) {
        if (error is CancellationException || error is AuthFailure) throw error
        throw AuthFailure(when (error) {
            is FirebaseAuthException -> error.errorCode
            is FirebaseNetworkException -> "ERROR_NETWORK_REQUEST_FAILED"
            is FirebaseTooManyRequestsException -> "ERROR_TOO_MANY_REQUESTS"
            else -> "UNKNOWN"
        })
    }
}
