# Credit Repair AI v2.0 - Android Build Instructions

## Quick Start (Local Build)

### Prerequisites
- Node.js v18+
- Java 17+ (for Gradle)
- Android SDK (API 30+)
- Android Studio (optional but recommended)

### Build Steps

```bash
# 1. Install dependencies
npm install --legacy-peer-deps

# 2. Build web assets
npm run build

# 3. Initialize/sync Capacitor
npx cap sync android

# 4. Build APK (Debug) - for testing
cd android
./gradlew assembleDebug
# Output: android/app/build/outputs/apk/debug/app-debug.apk

# 5. Build APK (Release) - for distribution
./gradlew assembleRelease
# Output: android/app/build/outputs/apk/release/app-release.apk

# 6. Build AAB (App Bundle) - for Google Play Store
./gradlew bundleRelease
# Output: android/app/build/outputs/bundle/release/app-release.aab
```

## Bypassing Convex Authentication

If you get Convex auth errors:

```bash
# Option 1: Skip type generation
mkdir -p convex
touch convex/_generated.d.ts

# Option 2: Use your Convex deployment key
export CONVEX_DEPLOY_KEY="your-key:your-team-123"
npx convex codegen
```

## CI/CD Build (Codemagic)

The `codemagic.yaml` is configured to:
1. Install dependencies
2. Build web app with Vite
3. Generate missing Convex types
4. Sync to Capacitor Android
5. Generate both APK and AAB files

### Expected Artifacts
- **app-debug.apk** - For immediate testing (~50-100MB)
- **app-release.apk** - For sideloading/distribution (smaller, optimized)
- **app-release.aab** - For Google Play Store submission (official format)

## Testing the Build

```bash
# Install debug APK on connected device
adb install android/app/build/outputs/apk/debug/app-debug.apk

# Or use Android Studio to run the app
open android  # Opens Android Studio
```

## Troubleshooting

### Gradle Build Fails
```bash
cd android
./gradlew clean
./gradlew assembleDebug --stacktrace
```

### Memory Errors
```bash
export GRADLE_OPTS="-Xmx4g"
./gradlew assembleRelease
```

### Convex Type Errors
```bash
# Generate empty types placeholder
echo "// Generated types" > convex/_generated.d.ts
npm run build
```

## Key Files Modified
- `codemagic.yaml` - Updated build pipeline
- `android/local.properties` - SDK configuration
- This guide

## Next Steps
1. Merge `android-native-rebuild` to a working branch
2. Test locally with `./gradlew assembleDebug`
3. Upload APK to device for testing
4. Use AAB for production/Play Store
