import test from "node:test";
import assert from "node:assert/strict";
import { evaluateSubscription, evaluateCasePurchase, YEARLY_PRODUCT, CASE_PRODUCT } from "../convex/billingPolicy.ts";
import { scopeCaseSnapshot } from "../convex/caseScope.ts";
const sample = () => ({ obfuscatedExternalProfileId: "a".repeat(64), purchaseStateContext: { purchaseState: "PURCHASED" },
  productLineItem: [{ productId: CASE_PRODUCT, productOfferDetails: { quantity: 1, refundableQuantity: 1, consumptionState: "CONSUMPTION_STATE_YET_TO_BE_CONSUMED" } }] });
test("yearly subscriptions use the same expiry and lifecycle checks as monthly", () => {
  const purchase = { subscriptionState: "SUBSCRIPTION_STATE_ACTIVE", lineItems: [{ productId: YEARLY_PRODUCT, expiryTime: "2030-01-01T00:00:00Z" }] };
  assert.equal(evaluateSubscription(purchase, 1).active, true);
  assert.equal(evaluateSubscription(purchase, Date.parse("2030-01-01T00:00:00Z")).active, false);
  assert.equal(evaluateSubscription({ ...purchase, lineItems: [{ productId: "unrelated", expiryTime: "2030-01-01T00:00:00Z" }] }, 1).active, false);
});
test("one-case access rejects pending, canceled, refunded, wrong products and missing bindings", () => {
  assert.equal(evaluateCasePurchase(sample()).active, true);
  for (const state of ["PENDING", "CANCELLED", "PURCHASE_STATE_UNSPECIFIED"]) {
    assert.equal(evaluateCasePurchase({ ...sample(), purchaseStateContext: { purchaseState: state } }).active, false);
  }
  for (const field of ["quantity", "refundableQuantity"]) {
    const purchase = sample(); purchase.productLineItem[0].productOfferDetails[field] = 0;
    assert.equal(evaluateCasePurchase(purchase).active, false);
    if (field === "refundableQuantity") assert.equal(evaluateCasePurchase(purchase).state, "REFUNDED");
  }
  assert.equal(evaluateCasePurchase({ ...sample(), obfuscatedExternalProfileId: "case-id" }).active, false);
  const wrong = sample(); wrong.productLineItem[0].productId = "unrelated";
  assert.equal(evaluateCasePurchase(wrong).active, false);
  const multi = sample(); multi.productLineItem.push(multi.productLineItem[0]);
  assert.equal(evaluateCasePurchase(multi).active, false);
});
test("consumption permits another purchase without losing the original case binding", () => {
  const purchase = sample(); purchase.productLineItem[0].productOfferDetails.consumptionState = "CONSUMPTION_STATE_CONSUMED";
  const result = evaluateCasePurchase(purchase);
  assert.equal(result.active, true); assert.equal(result.consumed, true); assert.equal(result.caseHash, "a".repeat(64));
});
test("server scopes a one-case request independently of the client snapshot", () => {
  const snapshot = { jurisdictionCode: "WA", disputes: [{ id: "one", creditor: "A", findingId: "f1", bureaus: ["equifax"] }, { id: "two", creditor: "B", findingId: "f2", bureaus: ["experian"] }],
    findings: [{ id: "f1" }, { id: "f2" }], accounts: [{ creditor: "A", bureau: "equifax" }, { creditor: "B", bureau: "experian" }, { creditor: "A", bureau: "transunion" }],
    reports: [{ bureau: "equifax" }, { bureau: "experian" }], extra: "drop" };
  const scoped = scopeCaseSnapshot(snapshot, "one");
  assert.deepEqual(scoped.disputes.map(d => d.id), ["one"]); assert.deepEqual(scoped.findings.map(f => f.id), ["f1"]);
  assert.deepEqual(scoped.accounts, [{ creditor: "A", bureau: "equifax" }]); assert.deepEqual(scoped.reports, [{ bureau: "equifax" }]);
  assert.equal(scoped.extra, undefined);
  assert.throws(() => scopeCaseSnapshot(snapshot, "missing"), /purchased dispute/);
  assert.throws(() => scopeCaseSnapshot({ ...snapshot, disputes: [snapshot.disputes[0], snapshot.disputes[0]] }, "one"), /purchased dispute/);
});
