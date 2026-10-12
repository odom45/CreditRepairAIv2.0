export const EDUCATIONAL_LABEL = "Educational information only — not legal or financial advice.";
export function redactText(text: string): string {
  return text.replace(/\b\d{3}[- ]?\d{2}[- ]?\d{4}\b/g, "[SSN REDACTED]")
    .replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, "[EMAIL REDACTED]")
    .replace(/(?<!\d)(?:\+?1[-. ]?)?(?:\(\d{3}\)|\d{3})[-. ]?\d{3}[-. ]?\d{4}(?!\d)/g, "[PHONE REDACTED]")
    .replace(/\b(?:dob|date of birth|born)\s*[:#-]?\s*(?:\d{1,2}[/-]){2}\d{2,4}\b/gi, "DOB: [REDACTED]")
    .replace(/\b(?:account|acct)\s*(?:number|no\.?|#)?\s*[:#-]?\s*([A-Z0-9-]{6,})/gi, (_, raw: string) => `account ••••${raw.replace(/[^A-Z0-9]/gi, "").slice(-4)}`)
    .replace(/(?<!\d)\d{9,19}(?!\d)/g, "[LONG NUMBER REDACTED]").slice(0, 4000);
}
export function sanitizeSnapshot(snapshot: Record<string, unknown>): Record<string, unknown> {
  const fields: Record<string, string[]> = {
    reports: ["bureau", "score", "accountCount", "importedAt"],
    accounts: ["creditor", "accountSuffix", "bureau", "status", "balance", "paymentStatus", "openedDate", "accountType", "responsibility", "dateReported", "originalCreditor", "pastDue", "creditLimit", "highBalance", "remarks"],
    findings: ["id", "type", "title", "detail", "severity", "creditor", "bureaus", "legalBasis"],
    disputes: ["id", "findingId", "creditor", "bureaus", "reason", "status", "createdAt", "sentAt", "craResponseDueAt", "responseReceived"],
  };
  const clean: Record<string, unknown> = { jurisdictionCode: typeof snapshot.jurisdictionCode === "string" ? snapshot.jurisdictionCode.slice(0, 2) : "" };
  for (const [key, allow] of Object.entries(fields)) {
    clean[key] = (Array.isArray(snapshot[key]) ? snapshot[key] : []).map(raw => {
      const row = raw && typeof raw === "object" ? raw as Record<string, unknown> : {};
      return Object.fromEntries(allow.filter(field => field in row).map(field => {
        const value = row[field];
        const sanitized = typeof value === "string" ? redactText(value) : Array.isArray(value) ? value.slice(0, 20).map(v => typeof v === "string" ? redactText(v) : null) : typeof value === "number" || typeof value === "boolean" || value === null ? value : null;
        return [field, sanitized];
      }));
    });
  }
  return clean;
}
export function educationalAnswer(text: string): string {
  // Reject a risky answer rather than add a disclaimer to an outcome promise. This
  // narrow deterministic filter is defense in depth, not a legal correctness proof.
  const risky = /\b(?:guaranteed?\s+(?:deletion|approval|results|score)|(?:will|definitely|certainly)\s+(?:be\s+)?(?:deleted|approved)|(?:will|definitely)\s+(?:raise|increase|improve|boost)\s+(?:your\s+)?(?:credit\s+)?score|(?:removed|deleted|approved)\s+(?:within|in)\s+\d+\s+(?:days|weeks|months)|(?:proves?|proof\s+of)\s+(?:an?\s+)?error)\b/i;
  if (risky.test(text)) return `${EDUCATIONAL_LABEL}\nThe answer did not pass the outcome-claim check. Verify the original records and official sources; ask for a factual explanation without outcome predictions.`;
  return `${EDUCATIONAL_LABEL}\n${redactText(text)}`;
}
