export type Bureau = "transunion" | "experian" | "equifax" | "unknown";
export const BUREAUS: Bureau[] = ["equifax", "experian", "transunion"];
export interface Account {
  id: string;
  creditor: string;
  accountSuffix: string;
  bureau: Bureau;
  status: string;
  balance: number | null;
  paymentStatus: string;
  openedDate: string;
  accountType: string;
  responsibility: string;
  dateReported: string;
  originalCreditor: string;
  pastDue: number | null;
  creditLimit: number | null;
  highBalance: number | null;
  remarks: string;
}
export interface Report {
  id: string;
  fileName: string;
  bureau: Bureau;
  score: number | null;
  accountCount: number;
  importedAt: number;
  extractionMethod: string;
}
export interface Finding {
  id: string;
  type: string;
  title: string;
  detail: string;
  severity: "HIGH" | "MEDIUM" | "LOW";
  creditor: string;
  bureaus: Bureau[];
  accountIds: string[];
  legalBasis: string[];
}
export interface Dispute {
  id: string;
  findingId: string;
  creditor: string;
  bureaus: Bureau[];
  reason: string;
  letter: string;
  status: "DRAFT" | "READY" | "SENT" | "INVESTIGATING" | "RESOLVED";
  createdAt: number;
  sentAt?: number;
  craResponseDueAt?: number;
}
export interface Filing {
  id: string;
  disputeId: string;
  portal: Bureau | "cfpb";
  step: number;
  reviewed: boolean;
  problem: string;
  companyResponse: string;
  resolution: string;
  evidence: string;
  confirmation: string;
  submittedAt?: number;
  responseReceivedAt?: number;
}
export interface Mailing {
  disputeId: string;
  bureau: Bureau;
  mailedAt: number;
  trackingNumber: string;
  deliveredAt?: number;
  responseReceivedAt?: number;
}
export interface Message {
  id: string;
  fromUser: boolean;
  text: string;
  caseScopeId?: string;
  createdAt: number;
}
export interface CaseState {
  version: 1;
  jurisdictionCode: string;
  onboardingComplete: boolean;
  reports: Report[];
  accounts: Account[];
  findings: Finding[];
  disputes: Dispute[];
  filings: Filing[];
  mailings: Mailing[];
  scores: { bureau: Bureau; score: number; recordedAt: number }[];
  chat: Message[];
  goal: { description: string; targetMonths: number; aiPlan: string };
  frozenBureaus: string[];
  policyAcceptance?: {
    version: string;
    digest: string;
    aiSharing: boolean;
    acceptedAt: number;
  };
}
export const emptyCase = (): CaseState => ({
  version: 1,
  jurisdictionCode: "",
  onboardingComplete: false,
  reports: [],
  accounts: [],
  findings: [],
  disputes: [],
  filings: [],
  mailings: [],
  scores: [],
  chat: [],
  goal: { description: "", targetMonths: 12, aiPlan: "" },
  frozenBureaus: [],
});
export const id = (): string => {
  if (typeof crypto.randomUUID === "function") return crypto.randomUUID();
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  bytes[6] = (bytes[6] & 15) | 64;
  bytes[8] = (bytes[8] & 63) | 128;
  const hex = Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
};
export const DAY = 86_400_000;
