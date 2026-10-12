import { createAccount, retrieveAccount } from "@convex-dev/auth/server";
import { v } from "convex/values";
import { Scrypt } from "lucia";
import { internalAction } from "./_generated/server";

const TEST_USER = {
  email: "agent@test.local",
  name: "Test Agent",
} as const;

export const seedTestUser = internalAction({
  args: {},
  returns: v.object({
    success: v.boolean(),
    message: v.string(),
  }),
  handler: async ctx => {
    const password = process.env.CREDIT_TEST_USER_PASSWORD;
    if (process.env.CREDIT_ENABLE_TEST_AUTH !== "true" || !password || password.length < 12) throw new Error("Test seeding is disabled or not configured.");
    try {
      await retrieveAccount(ctx, {
        provider: "test",
        account: { id: TEST_USER.email },
      });
      return { success: true, message: "Test user already exists" };
    } catch {
      // User doesn't exist, create them
    }

    try {
      const hashedPassword = await new Scrypt().hash(password);
      await createAccount(ctx, {
        provider: "test",
        account: {
          id: TEST_USER.email,
          secret: hashedPassword,
        },
        profile: {
          email: TEST_USER.email,
          name: TEST_USER.name,
          emailVerificationTime: Date.now(),
        },
        shouldLinkViaEmail: false,
      });
      return { success: true, message: "Test user created successfully" };
    } catch (error) {
      return {
        success: false,
        message: `Failed to create test user: ${error}`,
      };
    }
  },
});
