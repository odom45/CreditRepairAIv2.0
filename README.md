# CreditRepairAI v2.0

**Private credit-report review, legal-source AI guidance, and user-reviewed dispute preparation.**

CreditRepairAI helps people compare credit reports, understand differences, organize supporting records, prepare factual disputes, and track the responses. The product includes a native Android app and a web workspace, with native iPhone, iPad, and Mac clients in development.

**Created by Benjamin Odom · 3six0 Studios**

[Explore Android](https://github.com/odom45/CreditRepairAIv2.0/tree/android-native-rebuild) · [Explore the web app](https://github.com/odom45/CreditRepairAIv2.0/tree/main) · [Explore Apple development](https://github.com/odom45/CreditRepairAIv2.0/tree/ios-native) · [Verification record](docs/CROSS_PLATFORM_PARITY.md)

## What the product does

| Capability | Experience |
| --- | --- |
| Three-bureau report review | Import Equifax, Experian, and TransUnion PDFs; extract embedded text or recognize scanned pages locally; review the extracted accounts against the originals. |
| Comparison and evidence | Flag differences in balances, statuses, bureau coverage, and possible duplicate tradelines. A flag prompts verification; it does not establish an error. |
| Interactive AI guidance | Ask follow-up questions about the case, evidence, dispute steps, goals, or an actual bureau response. The authenticated service uses a redacted snapshot and retrieves legal sources for its answers. |
| Guided dispute preparation | Follow separate bureau and CFPB workflows, edit factual answers, review supporting records, copy appropriately sized text, and record actual receipts and responses. Users personally submit through the official portals. |
| Physical dispute letters | Prepare separate bureau letters, keep sender details local, export multipage PDFs, and record mailing, delivery, and response information. Android also supports native printing. |
| Optional paid mailing handoff | Review and export a letter, then upload and pay through LetterStream's website. The provider sets the fee; the app does not collect that mailing fee or place the order. |
| Personal planning and tracking | Save goals and AI plans, record dated scores, track dispute status and correspondence, and keep a freeze-action checklist. |
| Account and privacy controls | Firebase email/Google sign-in, verification handling, explicit sign-out, fresh-session protections, AI-sharing choices, encrypted local case storage, and account-deletion flows. |

These describe the product's implemented Android/web scope. Apple source targets the same experience; the platform status below identifies what has been verified.

## Platforms and branches

Android is the reference implementation for feature-parity work. The default `main` branch is the web app and the entry point for this product overview.

| Platform | Branch / source | Current scope and status |
| --- | --- | --- |
| **Web** | [main](https://github.com/odom45/CreditRepairAIv2.0/tree/main) | React/TypeScript workspace with local PDF/OCR import, case review, guided disputes, AI integration, letters, tracking, and an encrypted vault. Merged implementation and automated build/tests pass; live integration acceptance remains pending. |
| **Android** | [android-native-rebuild](https://github.com/odom45/CreditRepairAIv2.0/tree/android-native-rebuild) | Native Kotlin/Jetpack Compose app, version **2.0.9**. Includes on-device PDFBox/ML Kit parsing, Android Keystore storage, guided filing, interactive AI, physical letters, and provider mailing handoff. This is the benchmark for the other clients. |
| **iPhone / iOS** | [ios-native](https://github.com/odom45/CreditRepairAIv2.0/tree/ios-native) | Native SwiftUI source with shared iPhone, iPad, and Mac targets. Uses PDFKit/Vision and a bundled shared domain through JavaScriptCore. The latest native Apple validation failed; this is a development candidate. |
| **iPad / iPadOS** | Planned `ipados-native`; [shared target source](https://github.com/odom45/CreditRepairAIv2.0/blob/ios-native/native-apple/project.yml) | iPad target source is present in `ios-native`. Its individual branch has **not yet been created**, and platform acceptance is pending. |
| **Mac / macOS** | Planned `macos-native`; [shared target source](https://github.com/odom45/CreditRepairAIv2.0/blob/ios-native/native-apple/project.yml) | Mac target source is present in `ios-native`. Its individual branch has **not yet been created**; build, signing, and platform acceptance remain pending. |

Platform guides: [Android implementation and evidence](https://github.com/odom45/CreditRepairAIv2.0/blob/android-native-rebuild/README.md) · [Android build guide](https://github.com/odom45/CreditRepairAIv2.0/blob/android-native-rebuild/native-android/README.md) · [Apple setup and acceptance](https://github.com/odom45/CreditRepairAIv2.0/blob/ios-native/native-apple/README.md)

## How the workflow fits together

1. **Obtain the original bureau reports.** Save a PDF for each bureau you want to compare.
2. **Import and verify locally.** Review extracted account details and comparison flags against the dated originals.
3. **Ask for context.** With affirmative AI-sharing consent, send a redacted case snapshot to the authenticated legal-source assistant.
4. **Prepare a supported dispute.** Edit the facts and evidence list, use a bureau walkthrough, or export a physical letter.
5. **Submit and keep records.** Personally submit through an official portal or mailing provider, then record the actual receipt, delivery, and response.

CFPB preparation uses recorded prior-dispute and response information. Portal navigation, attachments, credentials, and submission stay under the user's control.

## What connects the clients

The clients use the same Firebase project and the shared Convex contracts for legal-source AI, policy acceptance, subscription status, and case-scoped access. Client code does not contain an OpenAI service key.

Case storage remains private to each device. The web client has explicit encrypted vault export/import. A companion Android transfer bridge is in [PR #22](https://github.com/odom45/CreditRepairAIv2.0/pull/22), and the Apple candidate implements the same versioned envelope. Transfer requires the **same Firebase account and transfer passphrase**.

**Automatic cloud case synchronization is not implemented.** The Android bridge has not yet been merged into the reference branch, and live cross-device transfer acceptance is still pending. Sharing an account alone does not move a case between devices.

### Privacy by platform

| Layer | Implementation |
| --- | --- |
| Report processing | Original PDFs and extracted raw report text are processed locally. AI receives an explicitly consented, structured, redacted snapshot. |
| AI data minimization | High-risk identifiers are redacted before AI requests. Original PDFs, private letter-sender fields, and mailing tracking numbers are excluded from the agent snapshot. |
| Android storage | AES-GCM with a non-exportable Android Keystore key, screenshot protection, and disabled Android backup. |
| Web storage | Passphrase-encrypted AES-256-GCM vault with PBKDF2-SHA256, random salt/IV, and the Firebase UID bound as authenticated additional data. The passphrase/key is not persisted. |
| Apple candidate | CryptoKit encrypted-vault source, iOS file-protection and backup-exclusion controls, and restricted macOS file permissions. Runtime and device acceptance are still required. |

Browser protections differ from Android's native protections. Platform-specific verification is part of completion.

## Verification and availability

**Evidence checked November 1, 2026.** Test counts describe specific checks and fixtures, not a guarantee of report accuracy or live cross-platform acceptance.

| Area | Evidence |
| --- | --- |
| Web on `main` | [Successful web validation](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41348911629): full web/backend TypeScript checks, **28 web regressions + 41 shared agent/billing regressions**, and production build. |
| Web parser and browser checks | The [verification record](docs/CROSS_PLATFORM_PARITY.md) documents all **18 Android text-corpus variants**, **2,400 seeded mutations**, six exact synthetic PDF checks across embedded text/OCR, and two malformed-input rejections. |
| Android benchmark | The [Android evidence guide](https://github.com/odom45/CreditRepairAIv2.0/blob/android-native-rebuild/README.md) links the dated parser, JVM, emulator, and release records. Its documented synthetic batch includes **72/72 supported PDFs**, **6/6 rejection guards**, and **1,950/1,950 matched fields**. |
| Android transfer bridge | [PR #22](https://github.com/odom45/CreditRepairAIv2.0/pull/22) remains a separate review branch. Its [native journey checks](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41349440802) and [measured parser batch](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41349440771) passed; the bridge is not yet in the reference branch. |
| Additional web safeguards | [PR #23](https://github.com/odom45/CreditRepairAIv2.0/pull/23) contains portable-build and vault safeguards. Its [web validation](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41349486661) passed, including **29 web regressions**; these follow-up changes are not yet merged into `main`. |
| Apple native clients | [Native Apple validation](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41349106140) failed. A passing web job on that branch does not establish an Apple build or device result. |

Live same-account sign-in, verification, consent changes, AI, entitlement restoration, deletion, and encrypted case transfer still need accepted integration evidence. Apple registration, signing, simulator/device results, and distribution have separate requirements. App-store availability and production launch approval are not established by these source branches.

Current acceptance details: [cross-platform record](docs/CROSS_PLATFORM_PARITY.md) · [Android release-owner checklist](https://github.com/odom45/CreditRepairAIv2.0/blob/android-native-rebuild/RELEASE_OWNER_CHECKLIST.md)

## Build and explore

### Web — `main`

Use Node.js 24, as in the web validation workflow.

```sh
git clone --branch main https://github.com/odom45/CreditRepairAIv2.0.git creditrepairai-web
cd creditrepairai-web
npm ci --ignore-scripts
cp .env.example .env.local
# Configure the shared backend endpoint in .env.local.
npm run typecheck
npm run test:web
npm run test:agent
npm run build
npm run dev
```

`VITE_CREDIT_AGENT_ENDPOINT` must resolve to the Android production gateway. Authorize the exact HTTPS web origin in the backend's `CREDIT_WEB_ORIGINS` value and in Firebase's authorized domains. Never put an OpenAI key in a `VITE_*` variable. The Firebase values in `config/firebase-web-public.json` are public client configuration.

Development and production builds bundle a self-hosted OCR worker, WASM cores, and English language model. The development-only `/__qa__/parser` harness exercises invented PDFs through the real importer and is excluded from production builds.

### Android — `android-native-rebuild`

Use JDK 17 and Android SDK 36. Signing and production configuration are covered by the [Android build guide](https://github.com/odom45/CreditRepairAIv2.0/blob/android-native-rebuild/native-android/README.md).

```sh
git clone --branch android-native-rebuild https://github.com/odom45/CreditRepairAIv2.0.git creditrepairai-android
cd creditrepairai-android
python3 scripts/check-parser-evidence.py
cd native-android
bash ./gradlew testDebugUnitTest assembleDebug
```

### Apple development — `ios-native`

Use macOS and Xcode. The [Apple guide](https://github.com/odom45/CreditRepairAIv2.0/blob/ios-native/native-apple/README.md) explains shared-domain generation, XcodeGen, the three schemes, Firebase bundle registration, Google sign-in configuration, signing, and acceptance checks. Apple apps are not ready for installation or distribution based on the current native validation result.

## Repository guide

| Location | Purpose |
| --- | --- |
| [`src/workspace/`](src/workspace/) | Active web interface, local report parser, comparison rules, guided workflows, and encrypted vault. |
| [`convex/`](convex/) | Shared authenticated backend contracts, agent tools, policies, and access enforcement. |
| [`tests/`](tests/) | Synthetic web fixtures and agent/billing regression checks. |
| [`policies/`](policies/) and [`public/legal/`](public/legal/) | Policy sources and user-facing legal/privacy pages. |
| [`native-android/` on Android](https://github.com/odom45/CreditRepairAIv2.0/tree/android-native-rebuild/native-android) | Native Android application and its platform tests. |
| [`native-apple/` on iOS](https://github.com/odom45/CreditRepairAIv2.0/tree/ios-native/native-apple) | Shared SwiftUI Apple source, platform schemes, and validation setup. |

## Supporting development branches

| Branch | Purpose |
| --- | --- |
| [android-cross-platform-transfer](https://github.com/odom45/CreditRepairAIv2.0/tree/android-cross-platform-transfer) | Companion encrypted case-transfer work targeting Android; tracked in PR #22. |
| [web-parity-hardening](https://github.com/odom45/CreditRepairAIv2.0/tree/web-parity-hardening) | Additional web-vault safeguards and host-portable dependency setup; tracked in PR #23. |
| [web-android-parity](https://github.com/odom45/CreditRepairAIv2.0/tree/web-android-parity) | Web implementation work merged into `main` through [PR #21](https://github.com/odom45/CreditRepairAIv2.0/pull/21). |

<details>
<summary>Other engineering and maintenance branches</summary>

Focused feature, fix, parser, and integration branches are retained for engineering history and review:

- [codex/parser-robustness-2026-10-26](https://github.com/odom45/CreditRepairAIv2.0/tree/codex/parser-robustness-2026-10-26)
- [feature/paid-mail-handoff](https://github.com/odom45/CreditRepairAIv2.0/tree/feature/paid-mail-handoff)
- [fix/explain-actual-dispute-result](https://github.com/odom45/CreditRepairAIv2.0/tree/fix/explain-actual-dispute-result)
- [fix/native-account-dispute-mail](https://github.com/odom45/CreditRepairAIv2.0/tree/fix/native-account-dispute-mail)
- [fix/runtime-build-2026-08-25](https://github.com/odom45/CreditRepairAIv2.0/tree/fix/runtime-build-2026-08-25)
- [parser-robustness-completion](https://github.com/odom45/CreditRepairAIv2.0/tree/parser-robustness-completion)
- [vexo-analytics-installation-1775507781464](https://github.com/odom45/CreditRepairAIv2.0/tree/vexo-analytics-installation-1775507781464)

Dependency-maintenance branches:

- [dependabot/npm_and_yarn/brace-expansion-5.0.6](https://github.com/odom45/CreditRepairAIv2.0/tree/dependabot/npm_and_yarn/brace-expansion-5.0.6)
- [dependabot/npm_and_yarn/multi-44a969df55](https://github.com/odom45/CreditRepairAIv2.0/tree/dependabot/npm_and_yarn/multi-44a969df55)
- [dependabot/npm_and_yarn/multi-a4987b4d91](https://github.com/odom45/CreditRepairAIv2.0/tree/dependabot/npm_and_yarn/multi-a4987b4d91)
- [dependabot/npm_and_yarn/multi-d9247b733d](https://github.com/odom45/CreditRepairAIv2.0/tree/dependabot/npm_and_yarn/multi-d9247b733d)
- [dependabot/npm_and_yarn/npm_and_yarn-1adf47b82b](https://github.com/odom45/CreditRepairAIv2.0/tree/dependabot/npm_and_yarn/npm_and_yarn-1adf47b82b)

These branch names are an inventory checked on November 1, 2026. Use the platform table above to find the product entry points and their documented status.

</details>

## Scope and ownership

CreditRepairAI provides educational organization and drafting assistance, not legal or financial advice. It does not guarantee deletion, score changes, lender approval, or response timing. Users verify their records and personally submit disputes and mailing orders.

This repository is proprietary. See [LICENSE](LICENSE) and [EULA](EULA).

**Benjamin Odom · 3six0 Studios** · [GitHub profile](https://github.com/odom45)
