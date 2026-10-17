# CreditRepairAI v2.0

AI-powered 3-bureau credit report analysis and legal dispute generation.

## Current native release — 2026-10-17

Signed **2.0.5 / 20261018** tester APK and Play AAB are built with the existing owner certificate. This update prevents silently partial AI case snapshots, keeps a saved plan when AI fails, and protects filing receipts/responses from stale edits and repeat submissions. Verified native source `6440464c` passed **65 JVM tests, 26 Android device tests, lint with zero errors, and Android 16 release installation/update/launch**. The device run also passed the full 60-case synthetic parser batch: 54 supported reports, 1,620 core fields and six expected rejections. These fictional fixtures do not establish current real-bureau or owner-phone acceptance.

Maintenance source `8cc79049` passed **47 backend/configuration/maintenance regressions**, typecheck, policy synchronization, source-pattern scan and official-source freshness checks. The existing development legal store now contains the amended Gorman opinion and corrected source card; no new store was created. A live fictional AI check passed with three HTTP-200 provider responses and 16 accepted source results. Read [the complete 2.0.5 verification record](docs/ENGINEERING_COMPLETION_2026-10-17.md).

**Launch remains on hold:** Firebase package/certificate registration and native sign-in are unresolved. Production deployment, Play billing acceptance, owner-phone/current-report testing, operational support verification, independent legal/security reviews and the real-person closed test remain open. Purchases are disabled. The dated sections below are historical records; their old API-credit blockers and counts do not describe this latest verified snapshot.

## Native branch engineering update — 2026-10-15

Native filing now has reviewable AI-generated field drafts; source supports monthly/yearly subscriptions and server-bound one-dispute purchases, with purchases disabled until account setup and review. Version 2.0.2 adds signed-release install/update acceptance. Current validation and account-dependent gates are tracked in [engineering completion](docs/ENGINEERING_COMPLETION_2026-10-15.md). The older release record below describes earlier tested source, not verification of these changes.

## Native branch release status — 2026-10-12

The native branch now implements persisted acquisition/filing guides, encrypted AI explanations alongside the filing steps, user-reviewed copy/paste drafts, receipt/response tracking, goals and AI explanations, Play monthly software-access billing, server-authorized subscription management, onboarding and policy screens. Session/privacy hardening and regression checks were added. Final source `2472e382` passed 37 unit, 20 backend and 15 API-35 device tests, debug assembly/signature checks and lint (zero errors, 29 warnings). npm and resolved Android dependency scans report zero advisories. Complete manual/device diversity, permissioned real layouts, native Firebase/Play integration, signed release and external reviews remain required. Custom Tab PDF downloads use an explicit user-selected Open/Share/import handoff.

Policy version `2026-10-12.2` adds explicit agreement/adult confirmation, a separate optional AI-sharing disclosure, encrypted choice records, and server-side current-policy checks. Privacy, terms and disclaimer drafts are maintained together in [policies/documents.json](policies/documents.json); `npm run policies:generate` updates Android/backend metadata, readable Markdown and public HTML, and CI checks for drift. [Legal review handoff](policies/LEGAL_REVIEW_HANDOFF.md) identifies required counsel decisions; the drafts do not make the operator immune from liability. Draft public policy/deletion pages are hosted on the authorized Convex development site and still require a configured monitored contact and qualified approval before commercial launch.

Native CI now also runs on-device instrumentation and lint on a GitHub-hosted emulator, with fictional embedded/scanned PDF fixtures, plus a read-only protected-configuration preflight. The preflight reports only setting presence and HTTP statuses. A private comparison confirmed the supplied GitHub OpenAI key already matches Convex. Development deployment succeeded, but real Responses calls return 429 `credit_balance_exhausted`; [run 38241196324](https://github.com/odom45/CreditRepairAIv2.0/actions/runs/38241196324) is therefore overall failed despite four passing code/preflight jobs. Restore API-project credit and rerun that live gate. Purchase/native authenticated-AI integration is still unproven.

Read [verification results](docs/VERIFICATION_RESULTS.md) and [funding preparation](docs/FUNDING_NEXT_STEPS.md). Read [RELEASE_OWNER_CHECKLIST.md](RELEASE_OWNER_CHECKLIST.md), [parser/device validation](docs/PARSER_VALIDATION.md), and [the architectural assessment](docs/PRODUCTION_ASSESSMENT.md). A finding is a review prompt; no deletion, score change, approval or outcome date is promised. Paid purchases default to disabled until owner configuration and qualified review.

The `src/` website is a separate legacy React prototype plus the admin interface. Its report/analysis pages contain sample data and must not be presented as production-native behavior. The native app does not display those web pages or use their simulated parser. The admin subscription screen has real server APIs in source; live Play permission/configuration is still unverified.

## 🚀 Features

- **Native 3-Bureau Analysis**: Extract structured fields and flag cross-bureau differences for user verification.
- **Tool-enabled Legal Agent**: A server-side agent can use redacted case tools and retrieve from a versioned official-source library. It never treats a flag as proof or promises deletion.
- **CFPB Assistant**: Guided, editable answers with prerequisite checks and an official-portal handoff; the user reviews and submits.
- **Secondary Bureau Freezes**: One-click access to freeze SageStream, CoreLogic, Innovis, and more.
- **Modern Dashboard**: Sleek, professional UI designed for clarity and ease of use.
- **Admin Panel**: Server-authorized Play subscription refresh, renewal cancellation, and confirmed refund/revoke actions; no simulated revenue or refund success.

## 🛠️ Tech Stack

- **Android**: Kotlin and Jetpack Compose (no WebView or website wrapper).
- **Backend**: Authenticated Convex HTTP gateway.
- **AI**: OpenAI Responses API with case functions and legal-library file search.

## 📦 Installation (Local Development)

1. Clone the repository:
   ```bash
   git clone https://github.com/odom45/CreditRepairAIv2.0.git
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the development server:
   ```bash
   npm run dev
   ```

## 📱 Play Store Submission

The debug APK is for testing, not Play submission. Production requires the signed-release workflow, an owner-controlled upload key, native OIDC sign-in, deployed agent secrets/vector store, real-report parser validation, and legal/privacy/commercial review. See `native-android/README.md`.

## 👤 Author

**Benjamin Odom**
- Email: benjaminjodom45@gmail.com
- GitHub: [@odom45](https://github.com/odom45)

---
*Built with 🤖 by Viktor AI*
