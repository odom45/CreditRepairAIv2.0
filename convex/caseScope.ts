/** Limit a case-only purchase to that case, independently of native UI controls. */
export function scopeCaseSnapshot(snapshot: Record<string, unknown>, caseId: string): Record<string, unknown> {
  const rows = (key: string) => (Array.isArray(snapshot[key]) ? snapshot[key] : [])
    .filter((row): row is Record<string, unknown> => !!row && typeof row === "object" && !Array.isArray(row));
  const disputes = rows("disputes").filter(row => row.id === caseId);
  if (disputes.length !== 1) throw new Error("Select the purchased dispute before asking AI.");
  const dispute = disputes[0];
  const bureaus = Array.isArray(dispute.bureaus) ? dispute.bureaus : [];
  return {
    jurisdictionCode: snapshot.jurisdictionCode,
    disputes,
    findings: rows("findings").filter(row => row.id === dispute.findingId),
    accounts: rows("accounts").filter(row => row.creditor === dispute.creditor && bureaus.includes(row.bureau)),
    reports: rows("reports").filter(row => bureaus.includes(row.bureau)),
  };
}
