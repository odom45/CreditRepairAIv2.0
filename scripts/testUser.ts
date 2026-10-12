export const TEST_USER = {
  email: "agent@test.local",
  password: process.env.CREDIT_TEST_USER_PASSWORD || "",
  name: "Test Agent",
} as const;
