# CreditRepairAI v2.0

AI-powered 3-bureau credit report analysis and legal dispute generation.

## 🚀 Features

- **Native 3-Bureau Analysis**: Extract structured fields and flag cross-bureau differences for user verification.
- **Tool-enabled Legal Agent**: A server-side agent can use redacted case tools and retrieve from a versioned official-source library. It never treats a flag as proof or promises deletion.
- **CFPB Assistant**: Guided, editable answers with prerequisite checks and an official-portal handoff; the user reviews and submits.
- **Secondary Bureau Freezes**: One-click access to freeze SageStream, CoreLogic, Innovis, and more.
- **Modern Dashboard**: Sleek, professional UI designed for clarity and ease of use.
- **Admin Panel**: Full control over users and system health (for administrators).

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
