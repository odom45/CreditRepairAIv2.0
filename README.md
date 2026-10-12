# CreditRepairAI v2.0

AI-powered 3-bureau credit report analysis and legal dispute generation.

## Native branch release status — 2026-10-12

The native branch now implements persisted acquisition/filing guides, encrypted AI explanations alongside the filing steps, user-reviewed copy/paste drafts, receipt/response tracking, goals and AI explanations, Play monthly software-access billing, server-authorized subscription management, onboarding and policy screens. Session/privacy hardening and regression checks were added. Implemented does not mean launch verified: full device flows, permissioned real layouts, live Firebase/Convex/OpenAI/Play integration, signed release and external reviews remain required. Custom Tab PDF downloads use an explicit user-selected Open/Share/import handoff.

Policy version `2026-10-12.2` adds explicit agreement/adult confirmation, a separate optional AI-sharing disclosure, encrypted choice records, and server-side current-policy checks. Privacy, terms and disclaimer drafts are maintained together in [policies/documents.json](policies/documents.json); `npm run policies:generate` updates Android/backend metadata, readable Markdown and public HTML, and CI checks for drift. [Legal review handoff](policies/LEGAL_REVIEW_HANDOFF.md) identifies required counsel decisions; the drafts do not make the operator immune from liability. Public deletion/support pages are prepared under `public/legal` and still require a configured monitored contact and public hosting before launch.

Native CI now also runs on-device instrumentation and lint on a GitHub-hosted emulator, with fictional embedded/scanned PDF fixtures, plus a read-only protected-configuration preflight. The preflight reports only setting presence and HTTP statuses; it neither prints credential values nor claims that purchase/authenticated-AI integration is proven.

Read [RELEASE_OWNER_CHECKLIST.md](RELEASE_OWNER_CHECKLIST.md), [parser/device validation](docs/PARSER_VALIDATION.md), and [the architectural assessment](docs/PRODUCTION_ASSESSMENT.md). A finding is a review prompt; no deletion, score change, approval or outcome date is promised. Paid purchases default to disabled until owner configuration and qualified review.

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
