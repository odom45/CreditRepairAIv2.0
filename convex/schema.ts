import { authTables } from "@convex-dev/auth/server";
import { defineSchema, defineTable } from "convex/server";
import { v } from "convex/values";

const schema = defineSchema({
  ...authTables,
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
