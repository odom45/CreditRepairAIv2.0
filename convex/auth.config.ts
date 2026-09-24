import { type AuthConfig } from "convex/server";

export default {
  providers: [
    {
      domain: process.env.CONVEX_SITE_URL,
      applicationID: "convex",
    },
    {
      // Firebase Authentication ID tokens are short-lived JWTs. The Android app
      // obtains them only after email/password or Google sign-in; no OAuth or
      // Firebase secret is shipped in the APK.
      type: "customJwt",
      applicationID: "credit-repair-ai-v2",
      issuer: "https://securetoken.google.com/credit-repair-ai-v2",
      jwks: "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com",
      algorithm: "RS256",
    },
  ],
} satisfies AuthConfig;
