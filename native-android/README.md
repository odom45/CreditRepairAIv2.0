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
- On-device report-aware assistant and realistic immediate/short/medium-term action guidance
- Manual score history, action progress, safe fictional demo data, and complete local-data deletion

## Important boundaries

- A finding is a review prompt, not proof that reported information is wrong.
- Users must review every statement, replace placeholders, attach evidence, and submit disputes or CFPB complaints themselves.
- The app does not guarantee deletion, approval, a score increase, or timing.
- The test APK uses the private on-device analysis/assistant. Set the `CREDIT_AI_GATEWAY_URL` Gradle property only when a separately secured, authenticated generative-AI gateway is available. API secrets must never be embedded in the Android app.

## Build

Requirements: JDK 17 and Android SDK 36.

```bash
./gradlew testDebugUnitTest assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```
