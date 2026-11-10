# Native Apple clients

SwiftUI interfaces for iPhone, iPad and Mac. PDFKit extracts PDF text; Vision recognizes scanned pages locally. The exact web parser, comparison, redaction, case scoping, letter content and guide guard functions run in bundled JavaScriptCore, not a WebView. Shared AES-GCM/PBKDF2 encrypted vault files bind to the same Firebase UID.

Individual branches select their platform in `native-apple/platform.json`. All three Xcode schemes remain in each branch so a shared change can be checked across platforms.

```sh
npm ci --ignore-scripts
node scripts/build-apple-domain.mjs
brew install xcodegen
xcodegen generate --spec native-apple/project.yml
open native-apple/CreditRepairAIApple.xcodeproj
```

Before live acceptance, register each bundle (`com.creditrepairai.v2.ios`, `.ipados`, `.macos`) in the existing `credit-repair-ai-v2` Firebase project. Put its downloaded public `GoogleService-Info.plist` in the corresponding ignored `Config/ios`, `Config/ipados`, or `Config/macos` folder. Configure the target's `GOOGLE_REVERSED_CLIENT_ID`, Firebase-authorized Google client and Apple signing team, and set `CREDIT_AGENT_ENDPOINT` to the same protected production gateway used by Android. Signing/Keychain access must be validated on real Apple devices; unsigned simulator compilation is not sign-in acceptance. Production macOS distribution also requires signing/notarization.

The app requires fresh sign-in after process recreation. It allows a five-minute background-return window for email verification and official-portal handoffs, refreshes pending email verification when the app becomes active, and locks if it remains backgrounded. The explicit Google sign-in continuation is exempt during its authentication flow. Vault keys remain in memory; local files are encrypted, excluded from backup, use complete file protection on iOS and restricted file permissions on macOS. No original reports, sender fields or tracking numbers go to AI. Existing subscriptions are enforced by the shared backend; new Apple purchases stay disabled in this candidate.

`DomainTests` checks all 18 Android fixture variants, 2,400 mutations, parser limits, an actual generated PDF, malformed input and a web-generated encrypted interoperability fixture. These tests and all three native builds must pass in the Apple workflow before source is described as build-verified. Device OCR, accessibility, UI flows, Google/email verification, same-account transfer/AI/access/deletion and live service acceptance still require explicit evidence. Native source creation alone does not establish Android-equivalent readiness.
