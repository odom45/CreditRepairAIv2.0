# CreditRepairAI v2.0 — shared web workspace

The `main` web client is being brought to parity with the pinned native Android 2.0.9 baseline. The current implementation and exact test/acceptance boundaries are recorded in [cross-platform parity](docs/CROSS_PLATFORM_PARITY.md). Live integration and Apple acceptance remain HOLD until verified.

The web client uses Firebase identity and the same authenticated Convex legal-agent, policy, and billing contracts as Android. Reports parse on-device; only explicitly consented, redacted snapshots go to AI. Original PDFs and private letter sender fields are excluded. Cases are saved in a passphrase-encrypted local vault with explicit encrypted export/import.

```sh
npm ci --ignore-scripts
npm run typecheck
npm run test:web
npm run test:agent
npm run build
npm run dev
```

Copy `.env.example` for build configuration. `VITE_CREDIT_AGENT_ENDPOINT` must resolve to the Android production gateway. The browser's exact HTTPS origin must be authorized in the backend `CREDIT_WEB_ORIGINS` environment value, and in Firebase's authorized domains. Never place an OpenAI key in a `VITE_*` variable. Firebase project identifiers in `config/firebase-web-public.json` are public client configuration, reused from the Android project; they do not grant server access.

`npm run dev` and `npm run build` bundle the OCR worker, WASM and English language model from installed packages. These are served locally to the client. In development only, `/__qa__/parser` runs invented embedded/scanned PDF checks through the real importer. That harness is excluded from production builds and is not an authentication bypass.

The Android standard remains on `android-native-rebuild`. Apple clients are maintained on individual `ios-native`, `ipados-native`, and `macos-native` branches, with shared contracts and explicit platform acceptance gates. This repository is proprietary; see LICENSE and EULA.

Author: Benjamin Odom · 3six0 Studios
