# Firebase registration repair — 2026-10-17

Project: `credit-repair-ai-v2`. Updated release: `2.0.6`, version code `20261019`.

## Completed

- Signed in to the project owner console using the secure authentication flow.
- Retained the release registration `com.creditrepairai.v2` and its prior SHA-1 fingerprint. Added the existing signed APK owner SHA-1 and SHA-256 fingerprints.
- Created the missing debug registration `com.creditrepairai.v2.debug` with a distinct Firebase Android app ID and registered the same configured debug signing certificate.
- Downloaded genuine Firebase configurations for both registrations. The committed files contain the corresponding Android OAuth certificate registrations and a consistent web OAuth client ID.
- Confirmed in Firebase Authentication that Google and email/password are both enabled.
- Read-only public Firebase configuration checks accepted both current package/certificate/app-ID combinations with HTTP 200 and the correct project. All-zero certificate controls were rejected with HTTP 400 `INVALID_CERT_HASH`. See `evidence/firebase-live-registration-2026-10-17.json`.
- Offline configuration readiness passes, including distinct Firebase Android app IDs and current signing-certificate coverage.

Owner certificate SHA-1: `B0:36:16:11:7F:D6:A0:37:92:77:07:1B:26:35:E4:19:64:74:6A:87`.
Owner certificate SHA-256: `1E:C8:AA:A5:A3:B2:59:AC:8D:0E:C1:6F:66:B8:05:DD:F8:2E:32:A7:3F:C5:F4:86:DC:E3:20:84:4E:8D:09:AD`.

## Acceptance scope

The certificate registration check does not sign in a person or issue a Firebase user token. Native Google/email sign-in, verification and reauthentication/deletion paths on the new signed APK, followed by the consented Firebase-to-Convex-to-AI request, still require phone acceptance. Play App Signing uses its own certificate; register the actual Play certificate before accepting a Play-distributed build. Existing development gateway and disabled purchases are retained. Production and commercial-launch requirements from the engineering record remain open.

Build/test results will be recorded after the signed release workflow completes.
