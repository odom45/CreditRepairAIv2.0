# CreditRepairAI native Android build

The current Android app is in `native-android/` and is built directly with Kotlin/Jetpack Compose. Do not run the legacy Capacitor instructions for this branch.

## Local test build

Requirements: JDK 17 and Android SDK 36.

```bash
cd native-android
./gradlew testDebugUnitTest assembleDebug
```

The test APK is written to `native-android/app/build/outputs/apk/debug/app-debug.apk`. It uses the `.debug` application ID suffix and is not a Play submission artifact.

## Signed production build

Use the manual **Signed Android release** GitHub Actions workflow documented in `native-android/README.md`. It requires the owner-controlled Play upload keystore and secure gateway configuration. The workflow builds and verifies:

- `app-release.aab` for Google Play upload
- `app-release.apk` for controlled tester distribution

Never commit a keystore, password, OpenAI key, vector-store ID, or permanent bearer token. For an existing Play app, the upload certificate must match the key registered in Play Console. Each upload must use a higher `versionCode` than the previous release.

The exact owner-supplied launch items are listed in `RELEASE_OWNER_CHECKLIST.md`.
