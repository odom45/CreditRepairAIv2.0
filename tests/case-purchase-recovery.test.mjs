import test from "node:test";
import assert from "node:assert/strict";
import { buildSync } from "esbuild";
import { createRequire } from "node:module";
import { runInNewContext } from "node:vm";
import { createHash } from "node:crypto";
const realRequire = createRequire(import.meta.url);
const bundle = buildSync({ entryPoints: ["convex/caseBilling.ts"], bundle: true, platform: "node", format: "cjs", packages: "external", write: false }).outputFiles[0].text;
function fixture() {
  const owner = "fictional-owner", caseHash = "a".repeat(64);
  const receipt = { obfuscatedExternalAccountId: createHash("sha256").update(owner).digest("hex"), obfuscatedExternalProfileId: caseHash,
    purchaseStateContext: { purchaseState: "PURCHASED" }, productLineItem: [{ productId: "creditrepairai_dispute", productOfferDetails: { quantity: 1, refundableQuantity: 1, consumptionState: "CONSUMPTION_STATE_YET_TO_BE_CONSUMED" } }] };
  const state = { receipt, saved: null, consumed: 0, failConsume: false, failGrant: false, deleted: false };
  class GoogleAuth { getClient() { return { request: async request => {
    if (request.method === "GET") return { data: structuredClone(receipt) };
    assert.match(request.url, /:consume$/); assert.equal(request.data, undefined);
    assert.equal(state.saved.caseHash, caseHash); assert.equal(state.saved.active, false);
    if (state.failConsume) throw new Error("Fictional network failure");
    receipt.productLineItem[0].productOfferDetails.consumptionState = "CONSUMPTION_STATE_CONSUMED";
    state.consumed++; return { data: undefined };
  } }; } }
  const module = { exports: {} };
  const env = { GOOGLE_PLAY_SERVICE_ACCOUNT_JSON: "{}" };
  runInNewContext(bundle, { module, exports: module.exports, require: name => name === "google-auth-library" ? { GoogleAuth } : realRequire(name), process: { env }, console });
  const ctx = { runMutation: async (_ref, args) => {
    if (state.deleted) throw new Error("deletion");
    if (args.active && state.failGrant) throw new Error("Fictional final commit failure");
    state.saved = { ...args }; return true;
  } };
  return { state, owner, caseHash, ctx, verify: module.exports.verify._handler };
}
test("receipt is durably bound before consumption and only then grants access", async () => {
  const f = fixture(); const result = await f.verify(f.ctx, { owner: f.owner, token: "fictional-token" });
  assert.equal(result.active, true); assert.equal(f.state.consumed, 1); assert.equal(f.state.saved.active, true);
});
test("failed consumption leaves no active grant and can be retried", async () => {
  const f = fixture(); f.state.failConsume = true;
  await assert.rejects(f.verify(f.ctx, { owner: f.owner, token: "fictional-token" }), /network failure/);
  assert.equal(f.state.saved.active, false);
  f.state.failConsume = false;
  assert.equal((await f.verify(f.ctx, { owner: f.owner, token: "fictional-token" })).active, true);
});
test("a crash after consumption recovers without consuming the receipt twice", async () => {
  const f = fixture(); f.state.failGrant = true;
  await assert.rejects(f.verify(f.ctx, { owner: f.owner, token: "fictional-token" }), /commit failure/);
  assert.equal(f.state.consumed, 1); assert.equal(f.state.saved.active, false);
  f.state.failGrant = false;
  assert.equal((await f.verify(f.ctx, { owner: f.owner, token: "fictional-token" })).active, true);
  assert.equal(f.state.consumed, 1);
});
test("wrong account and pending receipts never consume or grant", async () => {
  const f = fixture();
  await assert.rejects(f.verify(f.ctx, { owner: "other", token: "fictional-token" }), /account does not match/);
  assert.equal(f.state.saved, null); assert.equal(f.state.consumed, 0);
  f.state.receipt.purchaseStateContext.purchaseState = "PENDING";
  assert.equal((await f.verify(f.ctx, { owner: f.owner, token: "fictional-token" })).active, false);
  assert.equal(f.state.consumed, 0);
});
