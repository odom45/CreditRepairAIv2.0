# Firebase sign-in: exact account configuration still required

The signed release and owner-signed tester APK use the existing Benjamin Odom upload certificate. The committed release OAuth client lists a different SHA-1, and the debug configuration reuses the release Firebase app ID without a matching debug Android OAuth registration. This is a configuration defect; changing text in the client JSON cannot register an OAuth client on Google's servers.

## Apply in the existing owner project

1. Open Firebase project **credit-repair-ai-v2**, Project settings → General → Your apps.
2. Select Android package **com.creditrepairai.v2**. Add the public owner certificate fingerprints below, keeping other valid registrations.
3. Select or register a separate Android app for **com.creditrepairai.v2.debug** in the same Firebase project. Add the same certificate because CI uses the existing owner key for tester signing. The two Android apps must have different Firebase app IDs.
4. Authentication → Sign-in method: verify Google and Email/Password are enabled. Use the owner-approved support contact **admin@synergyfundai.com**.
5. Download the real refreshed `google-services.json` for each Android app. Replace the release file at `native-android/app/google-services.json` and debug file at `native-android/app/src/debug/google-services.json`. Keep their web OAuth client; the Credential Manager request deliberately uses the **web** client ID.
6. For a Play-delivered closed test, obtain **Play App Signing** SHA-1/SHA-256 from Play Console → App integrity and register those for the release Firebase app too. The upload-key certificate alone does not cover a Play re-signed APK.
7. Run `node scripts/firebase-signing-readiness.mjs --require-ready`, build the applicable APK, and perform the native checks below. This local check detects downloaded-config mismatches; it cannot prove console provider settings or successful login.

| Certificate | Public fingerprint |
| --- | --- |
| Owner upload SHA-1 | `B0:36:16:11:7F:D6:A0:37:92:77:07:1B:26:35:E4:19:64:74:6A:87` |
| Owner upload SHA-256 | `1E:C8:AA:A5:A3:B2:59:AC:8D:0E:C1:6F:66:B8:05:DD:F8:2E:32:A7:3F:C5:F4:86:DC:E3:20:84:4E:8D:09:AD` |

## Acceptance evidence

- Install the exact signed APK on a device with Google Play services. Select an owned Google account and observe Firebase sign-in followed by the real app Home screen. A canceled chooser or test-only identity is not a successful login.
- Create an owned staging email account, verify its email, sign out/back in, and verify the authenticated policy endpoint accepts the current documents. No real consumer records are needed.
- With API funds available, accept AI sharing and request an explanation of the built-in fictional case. Confirm Firebase → Convex → OpenAI returns retrieved sources; separately confirm unverified/unauthenticated requests are rejected.
- Sign out and switch accounts; verify the previous encrypted case does not reappear. Test recent-login-required deletion and retry on a disposable staging account only.
- Run the same Google login test using a Play-installed closed-test build, which validates Play's certificate.

Current blocker: on 2026-10-12 this agent's Firebase and Play Console sign-in pages returned Google-login connection errors. No authenticated console configuration or live native login was completed. Supply the two freshly downloaded configs after the registration above, or provide working console access through the secure browser sign-in flow; never send passwords or codes in chat.

Official setup: https://firebase.google.com/docs/auth/android/google-signin
Certificate registration: https://support.google.com/firebase/answer/9137403
