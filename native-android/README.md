# CreditRepairAI — Native Android

This directory contains the native Android rebuild. It uses Kotlin and Jetpack Compose; it does not use Capacitor, a WebView, or a wrapped website for the application interface.

## Included in the test build

- Dark navy/blue CreditRepairAI visual identity and native navigation
- PDF import through Android's secure document picker
- Embedded PDF text extraction with on-device OCR fallback for scanned reports
- Bureau and score detection plus structured account extraction
- Three-bureau comparisons for balance, status, missing-reporting, negative-item review, and possible duplicate patterns
- Fact-specific FCRA dispute drafts, clipboard/share actions, and tracking statuses
- CFPB walkthrough with a reviewable complaint narrative and official-portal handoff
- Secondary consumer-report freeze links and a private confirmation checklist
- Authenticated, tool-enabled report-aware agent contract plus realistic immediate/short/medium-term action guidance
- Manual score history, action progress, safe fictional demo data, and complete local-data deletion

## Important boundaries

- A finding is a review prompt, not proof that reported information is wrong.
- Users must review every statement, replace placeholders, attach evidence, and submit disputes or CFPB complaints themselves.
- The app does not guarantee deletion, approval, a score increase, or timing.
- Report parsing and deterministic review flags run on-device. The interactive assistant only calls an HTTPS, authenticated server-side agent using a redacted structured case snapshot. Set `CREDIT_AI_GATEWAY_URL` only for that gateway. API keys and legal-vector-store IDs must never be embedded in the Android app.

## Signed Play release

The debug APK is for testing only. Google Play requires the release AAB to be signed with the app's upload key. Run the **Signed Android release** GitHub workflow after configuring these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- `CREDIT_AI_GATEWAY_URL` (HTTPS)

For an existing Play listing, use the upload key already registered with that app. For a new listing, create the owner-controlled upload key once, back it up securely, and never commit it. The workflow emits a signed AAB for Play and a signed release APK for testers, then verifies both signatures.

When starting the workflow, enter a Play `version_code` greater than every previously uploaded build. The default `1` is appropriate only for a brand-new Play listing.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
