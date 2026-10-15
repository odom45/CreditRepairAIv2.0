export const MONTHLY_PRODUCT = "creditrepairai_monthly";
export const YEARLY_PRODUCT = "creditrepairai_yearly";
export const CASE_PRODUCT = "creditrepairai_dispute";
export const SUBSCRIPTION_PRODUCTS = [MONTHLY_PRODUCT, YEARLY_PRODUCT];
export type PlaySubscription = {
  subscriptionState?: string;
  acknowledgementState?: string;
  externalAccountIdentifiers?: { obfuscatedExternalAccountId?: string };
  linkedPurchaseToken?: string;
  lineItems?: Array<{ productId?: string; expiryTime?: string; autoRenewingPlan?: unknown }>;
};
export function evaluateSubscription(purchase: PlaySubscription, now: number) {
  const item = purchase.lineItems?.filter(i => SUBSCRIPTION_PRODUCTS.includes(i.productId || ""))
    .sort((a, b) => Date.parse(b.expiryTime || "") - Date.parse(a.expiryTime || ""))[0];
  const expiresAt = Date.parse(item?.expiryTime || "") || 0;
  const state = purchase.subscriptionState || "SUBSCRIPTION_STATE_UNSPECIFIED";
  const active = expiresAt > now && ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"].includes(state);
  return { productId: item?.productId || "", expiresAt, state, active };
}
export type PlayCasePurchase = {
  obfuscatedExternalAccountId?: string;
  obfuscatedExternalProfileId?: string;
  purchaseStateContext?: { purchaseState?: string };
  productLineItem?: Array<{ productId?: string; productOfferDetails?: {
    quantity?: number; refundableQuantity?: number; consumptionState?: string;
  } }>;
};
export function evaluateCasePurchase(purchase: PlayCasePurchase) {
  const items = purchase.productLineItem || [];
  const item = items.length === 1 && items[0].productId === CASE_PRODUCT ? items[0] : null;
  const offer = item?.productOfferDetails;
  const rawState = purchase.purchaseStateContext?.purchaseState || "UNSPECIFIED";
  const state = rawState === "PURCHASED" && offer?.refundableQuantity === 0 ? "REFUNDED" : rawState;
  const caseHash = purchase.obfuscatedExternalProfileId || "";
  const active = !!item && /^[a-f0-9]{64}$/.test(caseHash) && state === "PURCHASED" &&
    offer?.quantity === 1 && offer.refundableQuantity === 1 &&
    ["CONSUMPTION_STATE_YET_TO_BE_CONSUMED", "CONSUMPTION_STATE_CONSUMED"].includes(offer.consumptionState || "");
  return { caseHash, state, active, consumed: offer?.consumptionState === "CONSUMPTION_STATE_CONSUMED" };
}
export function isBillingAdmin(tokenIdentifier: string) {
  return (process.env.BILLING_ADMIN_IDENTITIES || "").split(",").map(s => s.trim()).filter(Boolean).includes(tokenIdentifier);
}
