package com.creditrepairai.v2.auth

/** Preserve an explicit result-bearing picker only; ordinary browser launches must still lock. */
internal class BackgroundLockPolicy {
    private val pendingResults = mutableSetOf<Int>()
    fun started(requestCode: Int) { if (requestCode >= 0) pendingResults.add(requestCode) }
    fun finished(requestCode: Int) { pendingResults.remove(requestCode) }
    fun shouldLock(changingConfiguration: Boolean, explicitGoogleSignIn: Boolean): Boolean =
        !changingConfiguration && !explicitGoogleSignIn && pendingResults.isEmpty()
}
