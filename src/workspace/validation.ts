import { z } from "zod";
const bureau = z.preprocess(
  v => (typeof v === "string" ? v.toLowerCase() : v),
  z.enum(["equifax", "experian", "transunion", "unknown"]),
);
const text = (max: number) => z.string().max(max);
const stamp = z.number().int().nonnegative().max(8640000000000000);
const optionalStamp = z.preprocess(
  v => (v === null ? undefined : v),
  stamp.optional(),
);
const amount = z.number().int().min(0).max(2147483647).nullable();
const identifier = text(160).min(1);
const bounded = <T extends z.ZodType>(schema: T, max = 10000) =>
  z.array(schema).max(max);
const report = z
  .object({
    id: identifier,
    fileName: text(512),
    bureau,
    score: z.number().int().min(300).max(850).nullable(),
    accountCount: z.number().int().min(0).max(150),
    importedAt: stamp,
    extractionMethod: text(100),
  })
  .passthrough();
const account = z
  .object({
    id: identifier,
    creditor: text(64),
    accountSuffix: text(4),
    bureau,
    status: text(80),
    balance: amount,
    paymentStatus: text(80),
    openedDate: text(40),
    accountType: text(80),
    responsibility: text(80),
    dateReported: text(40),
    originalCreditor: text(80),
    pastDue: amount,
    creditLimit: amount,
    highBalance: amount,
    remarks: text(200),
  })
  .passthrough();
const finding = z
  .object({
    id: identifier,
    type: text(80),
    title: text(1000),
    detail: text(10000),
    severity: z.enum(["HIGH", "MEDIUM", "LOW"]),
    creditor: text(64),
    bureaus: bounded(bureau, 4),
    accountIds: bounded(identifier),
    legalBasis: bounded(text(1000), 30),
  })
  .passthrough();
const dispute = z
  .object({
    id: identifier,
    findingId: identifier,
    creditor: text(64),
    bureaus: bounded(bureau, 4),
    reason: text(10000),
    letter: text(50000),
    status: z.enum(["DRAFT", "READY", "SENT", "INVESTIGATING", "RESOLVED"]),
    createdAt: stamp,
    sentAt: optionalStamp,
    craResponseDueAt: optionalStamp,
  })
  .passthrough();
const filing = z
  .object({
    id: identifier,
    disputeId: identifier,
    portal: z.preprocess(
      v => (typeof v === "string" ? v.toLowerCase() : v),
      z.enum(["equifax", "experian", "transunion", "cfpb"]),
    ),
    step: z.number().int().min(0).max(6),
    reviewed: z.boolean(),
    problem: text(10000),
    companyResponse: text(10000),
    resolution: text(10000),
    evidence: text(10000),
    confirmation: text(160),
    submittedAt: optionalStamp,
    responseReceivedAt: optionalStamp,
  })
  .passthrough();
const mailing = z
  .object({
    disputeId: identifier,
    bureau,
    mailedAt: stamp,
    trackingNumber: text(160),
    deliveredAt: optionalStamp,
    responseReceivedAt: optionalStamp,
  })
  .passthrough();
export const caseSchema = z
  .object({
    version: z.literal(1),
    jurisdictionCode: text(2),
    onboardingComplete: z.boolean(),
    reports: bounded(report, 1000),
    accounts: bounded(account),
    findings: bounded(finding),
    disputes: bounded(dispute),
    filings: bounded(filing),
    mailings: bounded(mailing),
    scores: bounded(
      z.object({
        bureau,
        score: z.number().int().min(300).max(850),
        recordedAt: stamp,
      }),
    ),
    chat: bounded(
      z.object({
        id: identifier,
        fromUser: z.boolean(),
        text: text(16000),
        caseScopeId: z.preprocess(
          v => (v === null ? undefined : v),
          identifier.optional(),
        ),
        createdAt: stamp,
      }),
    ),
    goal: z.object({
      description: text(4000),
      targetMonths: z.number().int().min(1).max(120),
      aiPlan: text(16000),
    }),
    frozenBureaus: bounded(text(160), 100),
    policyAcceptance: z.preprocess(
      v => (v === null ? undefined : v),
      z
        .object({
          version: text(100),
          digest: text(100),
          aiSharing: z.boolean(),
          acceptedAt: stamp,
        })
        .passthrough()
        .optional(),
    ),
  })
  .passthrough();
