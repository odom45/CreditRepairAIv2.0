import { authTables } from "@convex-dev/auth/server";
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

const schema = defineSchema({
  ...authTables,
  playSubscriptions: defineTable({
    owner: v.string(), token: v.string(), productId: v.string(), state: v.string(),
    expiresAt: v.number(), active: v.boolean(), checkedAt: v.number(),
  }).index("by_owner", ["owner"]).index("by_token", ["token"]),
  playCasePurchases: defineTable({
    owner: v.string(), token: v.string(), caseHash: v.string(), state: v.string(), active: v.boolean(), checkedAt: v.number(),
  }).index("by_owner", ["owner"]).index("by_token", ["token"]),
  billingAudit: defineTable({ actor: v.string(), subscriptionId: v.string(), action: v.string(), at: v.number(), result: v.string() }).index("by_actor", ["actor"]).index("by_subscription", ["subscriptionId"]),
  agentRateLimits: defineTable({ owner: v.string(), windowStart: v.number(), count: v.number() }).index("by_owner", ["owner"]),
  billingDeletionMarkers: defineTable({ ownerHash: v.string() }).index("by_owner_hash", ["ownerHash"]),
  policyAcceptances: defineTable({ ownerHash: v.string(), version: v.string(), digest: v.string(), aiSharing: v.boolean(), acceptedAt: v.number(), choiceAt: v.number() }).index("by_owner_hash", ["ownerHash"]),
  users: defineTable({
    name: v.optional(v.string()),
    email: v.optional(v.string()),
    phone: v.optional(v.string()),
    image: v.optional(v.string()),
    emailVerificationTime: v.optional(v.number()),
    phoneVerificationTime: v.optional(v.number()),
    isAnonymous: v.optional(v.boolean()),
    plan: v.optional(v.string()),
    discount: v.optional(v.number()),
    freeMonths: v.optional(v.number()),
  }).index("by_email", ["email"]),
  reports: defineTable({
    userId: v.id("users"),
    bureau: v.union(
      v.literal("transunion"),
      v.literal("experian"),
      v.literal("equifax"),
      v.literal("unknown")
    ),
    accounts: v.array(
      v.object({
        accountName: v.string(),
        accountNumber: v.string(),
        balance: v.number(),
        status: v.string(),
        bureau: v.string(),
      })
    ),
    parseDate: v.string(),
  }).index("by_userId", ["userId"]),
  disputes: defineTable({
    userId: v.id("users"),
    accountName: v.string(),
    bureau: v.string(),
    reason: v.string(),
    status: v.union(
      v.literal("draft"),
      v.literal("pending"),
      v.literal("sent"),
      v.literal("resolved")
    ),
    createdAt: v.string(),
  }).index("by_userId", ["userId"]),
});

export default schema;
