> Historical snapshot. Superseded by the current assessment and release-owner checklist on October 26, 2026. Do not follow these older configuration instructions as current status.

> Latest evidence: [2.0.5 completion record, October 17](../ENGINEERING_COMPLETION_2026-10-17.md). The historical score below has not been recalculated. Firebase/native sign-in and external launch acceptance remain open.

> Historical assessment from October 12. For current 2.0.3 code, signing and test results, read [the latest verification record](../VERIFICATION_RESULTS.md#203-completion-follow-up--2026-10-16-utc). The older counts and score below have not been recalculated and must not be presented as a current audit.

### 📊 1. Production Readiness Score

**Engineering release-readiness estimate: 65/100. Verdict: HOLD production Play deployment.** This is a review score, not a probability that the application is secure and not a claim that only 65% of the features exist. A production verdict includes configuration, integration, device, signing and human review evidence; feature code presence is a different measure.

Review date: 2026-10-12. Application source: `2472e38210ac525616c014a9b9c4b65f45a03476`, branch `android-native-rebuild`, [run 38241196324](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38241196324). Four code/preflight jobs passed. The development deployment succeeded, but the live Responses check returned 429, `credit_balance_exhausted`, making the overall run failed. That integration gate remains blocked. A separate protected check confirmed the GitHub and Convex OpenAI keys already match.

The four names are simulated review lenses used by one reviewer; Claude, Qodo and CodeRabbit services were not independently invoked. This is a release-evidence score, not a feature-completion percentage or a prediction of customer satisfaction.

Versioned agreement and separate AI-sharing consent cover native entry, encrypted persistence, server verification and deletion. Draft public policy/deletion pages are hosted on the authorized development deployment; a monitored contact and qualified approval remain required. Authentication dependencies and inherited Android crypto dependencies were updated. The current locked npm graph has zero known advisories; the resolved Android OSV scan completed across 213 Maven modules with zero reported advisories. See `docs/VERIFICATION_RESULTS.md` for source-specific counts and limitations and `policies/LEGAL_REVIEW_HANDOFF.md` for counsel decisions.

Targeted scans covered the root/build/manifest/workflows and the actual authentication, state, billing, persistence and linked HTTP/authorization files. The whole repository was not read into the review at once. Automated credential-pattern scans are separately bounded checks, not a full manual repository/security audit.

Repository mapping from the build and entry-point scans:

| Root / build entry | Responsibility and connection |
|---|---|
| `native-android/settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts` | Single native `:app`; Kotlin/Compose, Firebase Auth, Play Billing, PDFBox/ML Kit, WorkManager and native test configuration. |
| `native-android/app/src/main/AndroidManifest.xml` | Native `MainActivity`, explicit PDF intents, network/notification permissions; cleartext/backup disabled. No browser automation service or WebView. |
| `native-android/app/src/main/java/com/creditrepairai/v2/` | `ui` → `MainViewModel` → local `analysis`/`data` and authenticated `ai`/`billing`; `guidance` holds persisted checklist and filing logic. |
| `convex/schema.ts`, `auth.config.ts`, `http.ts` | Server schema, identity configuration and authenticated HTTP routes; `creditAgent*`, `playBilling.ts` and `billingData.ts` handle native agent/access/deletion. |
| `src/`, root `package.json` / `package-lock.json` | Separate React web prototype and admin UI; locked npm build/typecheck/test scripts. |
| `legal-library/`, `scripts/`, `tests/` | Versioned legal sources and check tooling; backend guardrail/storage regression tests. |
| `.github/workflows/native-android.yml`, `signed-android-release.yml`, `deploy-legal-agent.yml` | Debug/native/backend gates, owner-key signed release, protected agent deployment. |

| Review dimension | Weight | Earned | Evidence/limit |
|---|---:|---:|---|
| Native implementation and unit/build gates | 35 | 30 | Requested native feature source, 37 unit tests and owner-signed debug build pass; complete manual acceptance remains open. |
| Backend/admin correctness and integration | 20 | 10 | Build/typecheck, 20 regressions and deployment pass; API credit blocks live retrieval, and native Firebase/Play integration is unverified. |
| Privacy/security controls | 15 | 12 | Keystore encryption, redaction, UID/generation fences, separate consent, disabled operator/test paths and no backups; independent penetration test and remaining library TLS warning review required. |
| Release configuration and signing evidence | 10 | 6 | Existing credentials and debug APK signature verified; Firebase certificate registration, contact, Play authorization, production selection and release AAB/R8 remain gates. |
| Device, accessibility and actual report layouts | 10 | 7 | All 15 API-35 device scenarios, encrypted/embedded/OCR fixtures and double-font agreement pass; no complete manual/TalkBack/OEM/current-bureau-layout acceptance. |
| Required external launch reviews/submission | 10 | 0 | Legal review, penetration test, real-user testing and owner Play submission remain outside this work. |
| **Total** | **100** | **65** | **Staged engineering is tested; commercial Play launch remains withheld.** |

| Simulated lens | Unified opinion |
|---|---|
| ChatGPT Core | Dependencies and main orchestration are mapped; account/deletion fences and request contracts are corrected. Complete authenticated integrations before release. |
| Claude Simulation | Native Kotlin/Compose structure and platform isolation are intact; tested screen paths work on API 35. Large ViewModel/screen files and broader device acceptance remain review limits. |
| Qodo Simulation | New regressions cover stale work, encrypted ownership, deletion races, consent revocation, empty retrieval and timeout/completion handling. Fixture success does not prove all live runtime cases. |
| CodeRabbit Simulation | Build, dependency inventories and scans are reproducible; protected development deployment and debug signing are verified. Lint warnings, owner settings, failed live credit gate and signed-release evidence prevent production approval. |

Structural health: one native `:app` Gradle module with package-separated UI, session, analysis, storage, billing and guidance. MainViewModel remains a large orchestration class and the existing Compose screen file remains monolithic. The web React prototype is a separate deliverable; its sample report/analysis pages must not be marketed as live native behavior. The newly written admin screen uses real server actions, but source code alone does not establish Play permissions or refund execution.

| Requested addition | Current implementation | Acceptance still needed |
|---|---|---|
| Guided report acquisition | Saved checklist, official Custom Tab, selected PDF Open/Share/import, bureau capture/missing prompts, print/save instructions | Silent download interception is unavailable through Custom Tabs. Explicit user-selected handoff is the implemented platform adaptation; validate browser/PDF handoff on devices. |
| Guided dispute/complaint filing | Editable chunks, review before copy, user-only submission, receipts, response records and 30/45-day check-ins; AI help is displayed and encrypted alongside the saved step | Receipt/review/response orchestration passed on the emulator; still test actual current official forms, browser layouts, delivered reminders and funded AI guidance. The app does not observe portal state. |
| Planning and education | Actual-finding prioritized checklist, goal-based saved AI plan, finding/draft/citation explanations, progress/manual score history and educational labels | Validate authenticated model/tool outputs and guardrails with fictional cases. No output filter can certify every possible model response. |
| Monthly software access | Native Play client, server verification/acknowledgment/ownership, restore/manage, authorized admin actions | Owner Play product/service-account setup and license lifecycle tests; paid access disabled by default. |
| Launch hardening | Onboarding, draft policies/terms, support/delete paths, parser fixtures, release config checks, native launcher/theme assets | Double-font agreement, privacy flags and fictional parsing passed; still validate full accessibility/performance, permissioned layouts, approved policies/contact and signed release. |

**The additions are in the code being reviewed; they are not deferred until after this review. A 100% acceptance claim remains withheld because the platform adaptation and the listed engineering proof are not complete launch evidence.**

Completed evidence for the final source: **37 native unit tests, 20 backend tests, and all 15 API-35 device tests passed with zero failures**; debug APK assembled and verified against the existing owner certificate. Android lint passed with **zero errors and 29 warnings**, including one retained library TLS trust-manager warning. The resolved runtime inventory confirms all three Bouncy Castle packages at 1.86 and OSV reports no advisories for 213 modules. npm audit reports zero known advisories. Policy drift and source-pattern gates pass.

The backend deployment succeeded and all four policy/deletion HTML routes return 200; unauthenticated agent, acceptance and deletion calls return 401. **Real AI retrieval could not run because the matching existing server/GitHub OpenAI key returned `credit_balance_exhausted`.** No successful grounded live answer is claimed. The private key-consistency check passed separately, [run 38241914828](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38241914828).

Device tests cover actual Keystore persistence, generated embedded/scanned/encrypted PDF parsing, acquisition checklist progression, five-page onboarding, explicit agreement/adult confirmation, separate AI disclosure, double-font controls, activity privacy flags, and offline ViewModel/UI filing and Home/demo/Reports/Analysis navigation. These do not establish real Firebase authentication, Play purchase/refund lifecycle, full TalkBack/device diversity, current permissioned bureau layouts or human feedback. The engineering pass is complete to the recorded scope; **100% launch acceptance remains open**.

The existing owner signing credentials were validated using a temporary signing probe. A CI debug APK was signed and verified with that same certificate; no new upload key was generated. This establishes debug signing, **not release AAB/R8 acceptance**. The fingerprint does not match the Android OAuth certificates in either committed Firebase configuration. Source pattern scanning covers tracked source and a bounded historical scan; it is not a penetration test. Zero npm advisories does not establish that the Android dependency graph or all vendor behavior is vulnerability-free.

### 🛑 2. Critical Blockers & Safety Vulnerabilities (Qodo & CodeRabbit Lens)

The following are confirmed code findings from the pre-patch inspection and follow-up review. Regions below refer to the current corrected source where possible; pre-patch regions are explicitly identified. A fixed finding is not an outstanding exploitable behavior proven on a live deployment. Native paths shown as `native-android/.../` use the exact prefix `native-android/app/src/main/java/com/creditrepairai/v2/`.

| Severity/status | File and region | Trigger and consequence | Correction/evidence |
|---|---|---|---|
| High dependency exposure — patched; exploitability not established | `native-android/app/build.gradle.kts`, crypto constraints; inherited PDFBox POM requested Bouncy Castle 1.72 | Old crypto packages predate published fixes, including untrusted password-KDF resource bounds. No exploit against this app was performed. | Align `bcprov`, `bcpkix`, `bcutil` jdk15to18 to 1.86; encrypted-PDF device compatibility passed; resolved 213-module OSV scan reported zero advisories. One library TLS lint warning remains for independent review; native source does not install a permissive TLS manager. |
| High — patched | `convex/creditAgentCore.ts`, `runCreditAgent` retrieval loop and `callResponsesApi`; regression tests in `tests/agent-retrieval.test.mjs` | A `file_search_call` with empty/failed results previously satisfied the legal-grounding gate. An incomplete provider response could be presented as complete; provider calls had no overall deadline. | Request actual search results; require completed, identifiable, non-empty retrieval plus the case-summary tool; reject incomplete responses; abort after one 60-second budget and clear timers. Eight new mocked API regression tests exercise success and failure paths. Live retrieval is blocked by exhausted API credit. |
| High — patched | `native-android/.../MainViewModel.kt`, pre-patch 259–296; current `askAssistant`, `invalidateCase`, `clearData` | Start AI/import, then delete case or change account before completion. Old result can recreate encrypted data or attach to the new account. | Generation/owner tickets invalidate queued work; jobs canceled; cancellation rethrown; result commits guarded; requests bind the expected Firebase UID. SessionFence regression tests cover deletion, A→B→A switching and unsigned sessions. |
| High — patched | `native-android/.../billing/PlayBillingManager.kt`, pre-patch 28–33 and 75–91; current 32–113 | Delayed status/purchase callbacks can restore entitlement after reset/sign-out; failed verify previously left old entitlement. | Account generation fence on status/product/purchase callbacks; bound-token service calls; aggregate verified status; duplicate-token verification suppression; failed verification clears local entitlement. Server remains authoritative for paid AI. |
| High data-integrity — patched; device regression passed | `native-android/.../data/AppRepository.kt`, 30–35 and 66–71 | Clear case while still signed in → save a new case → restart. Clearing removed the ownership marker, so startup treated the new case as a different owner and destroyed it. | Preserve the hashed marker during case deletion/corrupt recovery; remove it only on switch/sign-out. Added an instrumented reproduction using actual Keystore storage. |
| High — patched; live race test pending | `convex/billingData.ts`, upsert/deleteOwner; `convex/playBilling.ts`, deleteOwner/verify | A refresh already in flight when account deletion committed could reinsert a purchase record. | Deletion writes a minimal hashed marker; upsert checks it in the same serializable database mutation as insertion. No report or purchase token is kept in the marker. Retention is disclosed for qualified privacy review. |
| High — patched | `convex/auth.ts`, 39–46; `convex/testAuth.ts`, authorization start; `convex/seedTestUser.ts` | Always-registered test provider accepted `@test.local` signups. Prior source contained a public test password; seeded/verified test identities were inappropriate for production. | Test auth/seeding disabled unless explicitly enabled in a protected staging environment; password comes from environment. Native AI only accepts Firebase issuer and verified email. No email-based admin bypass remains in the new admin UI. |
| High — patched | `convex/viktorTools.ts`, public search/image handlers (around 47–81) | Unauthenticated callers could invoke an external, potentially billable tool using server credentials; upstream error text could be exposed. | Disabled by default; exact server admin allowlist required; text size bounded; upstream failure body not returned. |
| High release gate — pending live proof | `convex/playBilling.ts`, 17–62; `convex/billingData.ts`, upsert | Acknowledgment failure could leave persisted active access; refreshing an old replacement token could resurrect it; a retired lookup could prevent later subscription status checks. | Reserve without access → acknowledge → grant. REPLACED is terminal in the store; linked tokens invalidated only after a valid active replacement. Known retired Google 410 tokens are marked unavailable. Must validate on a staging backend/Play license account. |
| Medium — patched | `native-android/.../analysis/CreditReportParser.kt`, 129–153 and OCR page loop | Uppercase labels such as `YOUR SCORE:` were parsed as creditor headings; adjacent fields could bleed across accounts. Malformed extremely tall pages could request huge bitmaps. | Narrowed heading recognition, bounded account windows and OCR dimensions; retained the fixture expectation and fixed code rather than weakening the test. |
| Medium memory safety — patched | `native-android/.../ai/CreditAgentGateway.kt`, 57–58; shared `ai/BoundedResponse.kt`, 6–22 | `readText().take(64000)` allocated the whole response before truncating it; an oversized upstream response could exhaust memory. | A shared bounded reader stops at the limit plus one character and rejects overflow instead of parsing truncated JSON. Tests include an infinite source and exact-boundary input. Billing/deletion use the same reader. |
| Production blocker — pending | workflows, owner settings, device checks | Signed release, correct production deployment, support mailbox, actual Google OAuth fingerprints and Play service-account access are not proven for this snapshot. The four code/preflight jobs passed; the overall run failed at the billable live AI gate. | Release fails on missing gateway/support/signing; owner checklist gives exact configuration and staging order. No launch-ready claim until remaining evidence exists. |

Privacy controls preserved: no WebView login instrumentation, no portal DOM inspection, no accessibility/overlay service, no bureau/government credential collection, no autofill and no auto-submission. Custom Tabs cannot silently intercept browser downloads; PDF handoff is deliberately user-selected Share/Open with or the document picker. Partial Tab UI support varies by browser; users may need to return to the saved checklist.

### ⚙️ 3. Structural Logic & Flow Bugs (Claude & ChatGPT Lens)

1. **Login redirection after a case change:** the earlier `mutate` implementation constructed a fresh AppUiState and lost `signedInEmail`. It now copies the existing UI state. Authentication changes also have a lifecycle-owned listener; it is removed when the ViewModel clears.
2. **Successful import spinner remained active:** preserving state exposed the need to explicitly clear `isProcessing` and progress text after import. This is now done. Multiple selected PDFs are queued with a mutex and session tickets; they are not dropped by a global busy flag.
3. **Account deletion is multi-service, not atomic:** native deletion removes Convex app records before asking Firebase to delete the login. Recent-login/auth failures are reported as incomplete deletion; the user must re-sign in/retry. A hashed deletion marker blocks in-flight billing work from recreating records; it contains no case data/purchase token. Google Play renewals and browser PDFs are separate and not automatically deleted/canceled. Live failure-path testing remains required.
4. **Stale goal results:** an AI answer for an earlier goal must not overwrite a newly edited goal. Saved planning output is applied only when the goal description and target still match the request snapshot.
5. **Purchase state and acknowledgment:** purchased is not sufficient for access. Expected account ownership, product, Google state, expiry and acknowledgment are checked; pending/hold/paused/expired cannot unlock. Canceled can retain access until verified expiry. Server refresh is hourly plus foreground/AI checks; RTDN is not claimed configured.
6. **Portal guidance cannot observe actual steps:** the guide stores the user's checklist progress, not browser page state. Character limits are user-adjustable; chunks must be matched with the actual official form. Only the user submits, then records an actual receipt. Reporting-company receipts and responses are distinct records.
   **Inline filing AI follow-up:** `ui/LaunchScreens.kt` displays an explanation alongside the step rather than navigating to the assistant. `guidance/GuidedWorkflow.kt` hashes the relevant saved filing/dispute context, rejects stale answers, and hides cached help after edits or step changes. `MainViewModel.explainFiling` also checks the account generation before storing answers. `data/AppRepository.kt` persists the note in the existing encrypted case. Errors and pending states remain on the filing screen; the static checklist remains available without AI. It never reads browser pages or changes/submits a form.
7. **Legacy web simulation boundary:** `src/pages/UploadPage.tsx` stores sample account rows; `AnalysisPage.tsx` is driven by sample records; `convex/reports.ts` retains a sample analysis action. These are not used by the native app and must not be shipped as a real report-analysis website. Completing that separate web product is outside the native launch scope.
8. **CI blind spots:** root `tsc --noEmit` did not traverse referenced projects as an effective complete gate, native CI did not cover backend tests, and npm dependency changes could desynchronize Bun's frozen lockfile. Workflows now use locked npm installation; typecheck covers frontend and Convex; native-branch changes trigger backend/admin checks and source audit too. Connected-device/lint proof is recorded per source snapshot; signed-release proof remains a separate gate.
9. **Structure/performance limits:** MainViewModel and the large existing screen file still concentrate responsibilities. Whole-case JSON encryption currently occurs synchronously on local edits; long-session/large-font/device performance must be measured. This targeted review does not certify every backend endpoint or every legacy web screen.

### 🔧 4. Consolidated Production Patches (The Consensus Edit)

These corrections are applied in source, not merely suggested. They combine native session/lifecycle boundaries, deterministic rejection of stale work, and preservation of encryption/ownership. The exact corrected files are in the repository. They are candidate production corrections with passing local unit/build evidence where recorded, not a substitute for device/integration/security review.

#### Patch 1 — prevent deleted or cross-account case resurrection

Files: `auth/SessionFence.kt`, `MainViewModel.kt`, `auth/FirebaseSession.kt`, `ai/CreditAgentGateway.kt`, `ai/SecureService.kt` under `native-android/app/src/main/java/com/creditrepairai/v2/`.

Problematic completion pattern (pre-patch MainViewModel):

```kotlin
val current = _uiState.value.data
val updated = current.copy(
    chat = (current.chat + ChatMessage(fromUser = false, text = answer)).takeLast(40),
    goal = if (saveAsPlan) current.goal.copy(aiPlan = answer) else current.goal,
)
repository.save(updated)
```

There was no check that the account or deleted-case generation still matched.

Fully corrected shared fence:

```kotlin
package com.creditrepairai.v2.auth

class SessionFence(private var owner: String?) {
    data class Ticket(val owner: String?, val generation: Long)
    private var generation = 0L
    @Synchronized fun ticket() = Ticket(owner, generation)
    @Synchronized fun invalidate(newOwner: String? = owner) {
        owner = newOwner
        generation++
    }
    @Synchronized fun accepts(ticket: Ticket, currentOwner: String?) =
        ticket.owner != null && ticket.owner == owner &&
        owner == currentOwner && ticket.generation == generation
}
```

Fully corrected bound-token method in FirebaseSession:

```kotlin
override suspend fun bearerToken(expectedSubject: String): String? {
    val user = auth.currentUser?.takeIf { it.uid == expectedSubject } ?: return null
    val token = user.getIdToken(false).await().token
    return token.takeIf { auth.currentUser?.uid == expectedSubject }
}
```

Fully corrected invalidation method in MainViewModel:

```kotlin
private fun invalidateCase(owner: String? = session.uid) {
    caseFence.invalidate(owner)
    aiJob?.cancel()
    filingHelpJob?.cancel()
    _uiState.update { it.copy(filingHelp = FilingHelpStatus(), showAiDisclosure = false, pendingAiRequest = null) }
    activeImportJob?.cancel()
    FollowUpWorker.cancelAll(context)
}
```

Requests capture a ticket before starting and call `caseFence.accepts(ticket, session.uid)` immediately before saving results. CancellationException is rethrown, not converted into a persisted assistant message. Clear, sign-out, account changes and demo replacement invalidate the generation. `SessionFenceTest` contains executable reproduction assertions.

#### Patch 2 — isolate billing verification to the active account

Files: `billing/PlayBillingManager.kt`, `auth/SessionFence.kt`, `ai/SecureService.kt`; server verification in `convex/playBilling.ts` and `convex/billingData.ts`.

Problematic status pattern:

```kotlin
scope.launch {
    runCatching { service.post("/billing/status") }
        .onSuccess { json -> state.update { it.copy(entitlement = decode(json)) } }
}
fun reset() { state.value = BillingUiState() }
```

A reset did not invalidate the delayed response.

Corrected complete refresh method:

```kotlin
fun refresh() {
    val uid = subject() ?: return
    if (sessionFence.ticket().owner != uid) reset()
    val ticket = sessionFence.ticket()
    if (statusJob?.isActive != true) statusJob = scope.launch {
        runCatching { service.post("/billing/status", uid) }.onSuccess { json ->
            if (sessionFence.accepts(ticket, subject()))
                state.update { it.copy(entitlement = decode(json)) }
        }.onFailure {
            if (it is CancellationException) throw it
            if (sessionFence.accepts(ticket, subject()))
                state.update { it.copy(entitlement = Entitlement(), message = "Subscription verification unavailable. Refresh when connected.") }
        }
    }
    if (client.isReady) query(ticket) else if (!connecting) {
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (!sessionFence.accepts(ticket, subject())) return
                if (result.responseCode == BillingClient.BillingResponseCode.OK) query(ticket)
                else state.update { it.copy(available = false, busy = false, message = "Google Play Billing is unavailable for this build/account. Local tools remain available.") }
            }
            override fun onBillingServiceDisconnected() {
                connecting = false
                state.update { it.copy(available = false, message = "Google Play disconnected. Refresh to retry.") }
            }
        })
    }
}
```

Corrected reset:

```kotlin
fun reset() {
    sessionFence.invalidate(subject())
    scope.coroutineContext.cancelChildren()
    verifying.clear()
    state.value = BillingUiState()
}
```

Product/purchase callbacks also check the same ticket. Verification failures clear local access; the service obtains a token only for the captured UID. The server reserves ownership without access, acknowledges the verified purchased entitlement, then persists access; terminal replacement tokens cannot be revived. Google APIs/actual Play lifecycle behavior must still be validated with owner-provided staging permissions.

#### Patch 3 — preserve new encrypted records after clearing a case

File: `native-android/app/src/main/java/com/creditrepairai/v2/data/AppRepository.kt`.

Problematic method:

```kotlin
fun clear() {
    preferences.edit().clear().commit()
    keyStore().apply {
        if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
    }
}
```

Fully corrected method:

```kotlin
fun clear(preserveOwner: Boolean = true) {
    val owner = if (preserveOwner) preferences.getString("owner", null) else null
    check(preferences.edit().clear().putString("owner", owner).commit()) {
        "Local deletion could not be saved."
    }
    keyStore().apply {
        if (containsAlias(KEY_ALIAS)) deleteEntry(KEY_ALIAS)
    }
}
```

Account switching/sign-out use `clear(preserveOwner = false)`; local case clearing/corrupt recovery preserve the hashed ownership marker. The old encryption key is still destroyed. No plaintext case data is retained. `AppRepositoryInstrumentedTest.clearingThenSavingPreservesNewCaseAcrossRestart` checks the actual reproduction on a disposable debug device; that device test has not been executed here.

Reviewed code is published and all four code/preflight jobs passed. The overall workflow remains failed at the live API-credit gate. Required continuation order: restore existing API-project credit and rerun live retrieval → configure monitored contact/Firebase certificates/Play permissions → stage verified-email native Firebase/Convex/AI and Play license tests → complete manual device/parser/accessibility/deletion walkthrough and qualified legal/security review → remediate findings → signed release-mode verification with the existing owner key → owner Play submission. Exact configuration and remaining human actions are in `RELEASE_OWNER_CHECKLIST.md`. No real consumer report, real dispute/complaint submission, actual purchase/refund or Play submission was used in this work.
