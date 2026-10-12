import test from "node:test";
import assert from "node:assert/strict";
import { evaluateSubscription, isBillingAdmin } from "../convex/billingPolicy.ts";
import { educationalAnswer, sanitizeSnapshot, redactText, EDUCATIONAL_LABEL } from "../convex/agentSafety.ts";

test("Play lifecycle grants only verified eligible non-expired states", () => {
  const sample = { lineItems: [{ productId: "creditrepairai_monthly", expiryTime: "2030-01-01T00:00:00Z" }] };
  for (const state of ["ACTIVE", "IN_GRACE_PERIOD", "CANCELED"]) assert.equal(evaluateSubscription({ ...sample, subscriptionState: `SUBSCRIPTION_STATE_${state}` }, 1).active, true);
  for (const state of ["PENDING", "ON_HOLD", "PAUSED", "EXPIRED", "REVOKED"]) assert.equal(evaluateSubscription({ ...sample, subscriptionState: `SUBSCRIPTION_STATE_${state}` }, 1).active, false);
  assert.equal(evaluateSubscription({ ...sample, subscriptionState: "SUBSCRIPTION_STATE_ACTIVE" }, Date.parse("2030-01-01T00:00:00Z")).active, false);
  assert.equal(evaluateSubscription({}, 1).active, false);
});
test("admin authorization is a protected identity allowlist, never an email heuristic", () => {
  process.env.BILLING_ADMIN_IDENTITIES = "issuer|owner";
  assert.equal(isBillingAdmin("issuer|owner"), true);
  assert.equal(isBillingAdmin("agent@test.local"), false);
  assert.equal(isBillingAdmin(""), false);
  delete process.env.BILLING_ADMIN_IDENTITIES;
});
test("snapshot drops unapproved fields and redacts all approved text", () => {
  const snapshot = sanitizeSnapshot({ password: "never-forward", accounts: [{ creditor: "SSN 123-45-6789", remarks: "a@example.test", accountSuffix: "1234", arbitrary: "never-forward" }] });
  const text = JSON.stringify(snapshot);
  assert.ok(!text.includes("never-forward")); assert.ok(!text.includes("123-45-6789")); assert.ok(!text.includes("a@example.test"));
  assert.ok(redactText("phone (212) 555-0100").includes("REDACTED"));
});
test("all answers carry education label; promise patterns fail closed", () => {
  for (const text of ["You will be approved", "This will raise your score", "Guaranteed deletion", "This proves an error", "Deleted within 30 days"]) assert.ok(educationalAnswer(text).includes("did not pass"));
  assert.ok(educationalAnswer("Compare the dated report with your statement.").startsWith(EDUCATIONAL_LABEL));
});
