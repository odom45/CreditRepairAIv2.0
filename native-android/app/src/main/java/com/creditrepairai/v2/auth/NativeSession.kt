package com.creditrepairai.v2.auth

import android.app.Activity
import android.content.Context
import com.creditrepairai.v2.ai.SessionTokenProvider

/** Production implementation is FirebaseSession; device tests supply an offline identity. */
interface NativeSession : SessionTokenProvider {
    val email: String?
    val uid: String?
    val isEmailVerified: Boolean
    fun observeSession(onChange: (String?) -> Unit): () -> Unit
    suspend fun signInWithEmail(email: String, password: String)
    suspend fun createAccount(email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    suspend fun resendVerification()
    suspend fun refreshUser()
    suspend fun signInWithGoogle(activity: Activity)
    suspend fun deleteAccount()
    suspend fun signOut(context: Context)
}
