# Encrypted portable case bridge

Companion to the web `main` and individual native Apple branches. The existing Keystore-based local case remains unchanged. Settings adds an explicit AES-256-GCM transfer file using PBKDF2-SHA256 (310,000 iterations), 16-byte random salt, 12-byte random IV, a user-held passphrase and Firebase UID as authenticated additional data. Files interoperate with the Web Crypto and Apple CryptoKit envelope. No plaintext case or passphrase is uploaded.

Export before signing out if you need a portable copy; the existing Android explicit sign-out behavior removes its local case. Import replaces the current local case and requires the same account and transfer passphrase. Missing fields, oversized files, unsupported formats and full account suffixes are rejected before replacement. A changed session discards the result. Android Keystore keys are never exported.

`TransferVaultTest` imports the same synthetic Web Crypto envelope used by Apple tests and checks wrong owner, wrong passphrase, roundtrip, random IV and tampering. Existing Android JVM and instrumented suites must be accepted before merging this bridge into the baseline or shipping it in an APK. This branch does not change release version/signing configuration or claim that live cross-device acceptance has passed.
