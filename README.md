# CreditRepairAI v2.0

**Private credit-report review, legal-source AI guidance, and user-reviewed dispute preparation.**

CreditRepairAI helps people compare credit reports, understand differences, organize supporting records, prepare factual disputes, and track the responses. The product is maintained across five product branches: web, native Android, native iPhone, native iPad, and native Mac. The Apple clients share a SwiftUI codebase and are built independently for their platform targets.

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

These are the shared product capabilities. Each client uses platform-native UI and security controls; the platform table below separates simulator/CI evidence from live account and device acceptance.

## Platforms and branches

Android is the reference implementation for feature-parity work. The default `main` branch is the web app and the entry point for this product overview.

| Platform | Branch / source | Current scope and status |
| --- | --- | --- |
| **Web** | [main](https://github.com/odom45/CreditRepairAIv2.0/tree/main) | React/TypeScript workspace with local PDF/OCR import, case review, guided disputes, AI integration, letters, tracking, and an encrypted vault. Merged implementation and automated build/tests pass; live integration acceptance remains pending. |
| **Android** | [android-native-rebuild](https://github.com/odom45/CreditRepairAIv2.0/tree/android-native-rebuild) | Native Kotlin/Jetpack Compose app, version **2.0.9**. Includes on-device PDFBox/ML Kit parsing, Android Keystore storage, guided filing, interactive AI, physical letters, and provider mailing handoff. This is the benchmark for the other clients. |
| **iPhone / iOS** | [ios-native](https://github.com/odom45/CreditRepairAIv2.0/tree/ios-native) | Native SwiftUI iPhone client. PDFKit/Vision parsing, shared JavaScriptCore domain, Firebase sign-in, encrypted local vault, guided disputes, letters, and AI UI. Latest iOS/iPad simulator builds and macOS regression suite passed in [Apple CI run 42736736736](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42736736736). Live signing and device acceptance remain pending. |
| **iPad / iPadOS** | [ipados-native](https://github.com/odom45/CreditRepairAIv2.0/tree/ipados-native) | Dedicated native branch and iPad SwiftUI target. iPhone/iPad simulator compilation and macOS regression tests passed in [Apple CI run 42736740789](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42736740789). Live signing and iPad-device acceptance remain pending. |
| **Mac / macOS** | [macos-native](https://github.com/odom45/CreditRepairAIv2.0/tree/macos-native) | Dedicated native branch and macOS SwiftUI target. iPhone/iPad simulator compilation and macOS regression tests passed in [Apple CI run 42736744999](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42736744999). Developer ID signing, notarization, and real-Mac acceptance remain pending. |

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

Case storage remains private to each device. The web, Android, and Apple clients now implement the same versioned AES-GCM/PBKDF2 encrypted-vault envelope with Firebase-UID-bound authenticated data. The Android transfer bridge was merged into `android-native-rebuild` in [PR #22](https://github.com/odom45/CreditRepairAIv2.0/pull/22); web vault safeguards were merged into `main` in [PR #23](https://github.com/odom45/CreditRepairAIv2.0/pull/23). Transfer requires the **same Firebase account and transfer passphrase**.

**Automatic cloud case synchronization is not implemented.** Cross-device transfer is explicit and encrypted; live transfer acceptance across real devices is still pending. Sharing an account alone does not move a case between devices.

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

**Evidence updated November 9, 2026.** Test counts describe specific checks and fixtures, not a guarantee of report accuracy or live cross-platform acceptance.

| Area | Evidence |
| --- | --- |
| Web on `main` | [Successful web validation](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/41348911629): full web/backend TypeScript checks, **28 web regressions + 41 shared agent/billing regressions**, and production build. |
| Web parser and browser checks | The [verification record](docs/CROSS_PLATFORM_PARITY.md) documents all **18 Android text-corpus variants**, **2,400 seeded mutations**, six exact synthetic PDF checks across embedded text/OCR, and two malformed-input rejections. |
| Android benchmark | After PR #22, native device tests passed in [run 42737792061](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792061), native account/dispute/letter journeys in [42737792195](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792195), measured parser batch in [42737792155](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792155), and signed release/install acceptance in [42737792243](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737792243). |
| Android transfer bridge | [PR #22](https://github.com/odom45/CreditRepairAIv2.0/pull/22) merged into `android-native-rebuild` at `0b28929`; the post-merge native, parser, journey, signed-build and emulator install/update checks passed. Live cross-device transfer still needs real-device acceptance. |
| Additional web safeguards | [PR #23](https://github.com/odom45/CreditRepairAIv2.0/pull/23) was merged into `main` at `dd553a6`. The post-merge web validation passed in [run 42737833248](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42737833248). |
| Apple native clients | All three base branches passed web checks, iPhone/iPad simulator builds, and macOS regression tests. The latest email-verification gate, five-minute return flow, and secure Safari portal-handoff changes are in CI now: [iOS](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42739782188), [iPadOS](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42739788091), and [macOS](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/42739793769). Apple signing, real-device sign-in/OCR, and distribution acceptance remain pending. |

The Android backend/admin gate currently remains HOLD because the federal legal-library review is 55 days old against its configured 45-day freshness limit; the date and threshold have not been falsified or weakened. Live same-account sign-in, verification, consent changes, AI, entitlement restoration, deletion, and encrypted case transfer still need accepted integration evidence. Apple registration, signing, simulator/device results, and distribution have separate requirements. App-store availability and production launch approval are not established by these source branches.

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

### Apple development — `ios-native`, `ipados-native`, and `macos-native`

Use macOS and Xcode. Each branch contains the shared SwiftUI source and the three Xcode schemes; `native-apple/platform.json` selects the branch's platform identity. The [Apple guide](https://github.com/odom45/CreditRepairAIv2.0/blob/ios-native/native-apple/README.md) explains shared-domain generation, XcodeGen, Firebase bundle registration, Google sign-in configuration, signing, and acceptance checks. Simulator compilation and macOS regression tests pass; Apple apps are not yet signed or accepted on live devices.

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
| [android-cross-platform-transfer](https://github.com/odom45/CreditRepairAIv2.0/tree/android-cross-platform-transfer) | Merged into `android-native-rebuild` through PR #22. |
| [web-parity-hardening](https://github.com/odom45/CreditRepairAIv2.0/tree/web-parity-hardening) | Merged into `main` through PR #23. |
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

These branch names are an inventory checked on November 9, 2026. Use the platform table above to find the product entry points and their documented status.

</details>

## Scope and ownership

CreditRepairAI provides educational organization and drafting assistance, not legal or financial advice. It does not guarantee deletion, score changes, lender approval, or response timing. Users verify their records and personally submit disputes and mailing orders.

This repository is proprietary. See [LICENSE](LICENSE) and [EULA](EULA).

**Benjamin Odom · 3six0 Studios** · [GitHub profile](https://github.com/odom45)
