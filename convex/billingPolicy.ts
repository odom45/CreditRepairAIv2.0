export const MONTHLY_PRODUCT = "creditrepairai_monthly";
export type PlaySubscription = {
  subscriptionState?: string;
  acknowledgementState?: string;
  externalAccountIdentifiers?: { obfuscatedExternalAccountId?: string };
  linkedPurchaseToken?: string;
  lineItems?: Array<{ productId?: string; expiryTime?: string; autoRenewingPlan?: unknown }>;
};
export function evaluateSubscription(purchase: PlaySubscription, now: number) {
  const item = purchase.lineItems?.filter(i => i.productId === MONTHLY_PRODUCT)
    .sort((a, b) => Date.parse(b.expiryTime || "") - Date.parse(a.expiryTime || ""))[0];
  const expiresAt = Date.parse(item?.expiryTime || "") || 0;
  const state = purchase.subscriptionState || "SUBSCRIPTION_STATE_UNSPECIFIED";
  const active = expiresAt > now && ["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD", "SUBSCRIPTION_STATE_CANCELED"].includes(state);
  return { productId: item?.productId || "", expiresAt, state, active };
}
export function isBillingAdmin(tokenIdentifier: string) {
  return (process.env.BILLING_ADMIN_IDENTITIES || "").split(",").map(s => s.trim()).filter(Boolean).includes(tokenIdentifier);
}
