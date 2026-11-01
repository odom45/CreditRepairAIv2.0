import {
  id,
  DAY,
  type Account,
  type CaseState,
  type Dispute,
  type Filing,
  type Finding,
  type Mailing,
  type Report,
} from "./model.ts";
export function analyze(accounts: Account[], reports: Report[]): Finding[] {
  const imported = new Set(
    reports.map(r => r.bureau).filter(b => b !== "unknown"),
  );
  const groups = new Map<string, Account[]>();
  for (const a of accounts) {
    const name = a.creditor.toLowerCase().replace(/[^a-z0-9]/g, "");
    const key =
      name +
      (a.accountSuffix
        ? ":suffix:" + a.accountSuffix.toUpperCase()
        : a.openedDate
          ? ":opened:" +
            (a.openedDate + ":" + a.accountType)
              .toLowerCase()
              .replace(/[^a-z0-9:]/g, "")
          : ":unmatched:" + a.id);
    groups.set(key, [...(groups.get(key) || []), a]);
  }
  const findings: Finding[] = [];
  for (const versions of groups.values()) {
    const bureaus = [
      ...new Set(versions.map(v => v.bureau).filter(b => b !== "unknown")),
    ];
    const creditor = versions[0].creditor;
    const add = (
      type: string,
      title: string,
      detail: string,
      severity: "HIGH" | "MEDIUM" = "HIGH",
      list = versions,
      bs = bureaus,
    ) =>
      findings.push({
        id: id(),
        type,
        title,
        detail,
        severity,
        creditor,
        bureaus: bs,
        accountIds: list.map(a => a.id),
        legalBasis:
          type === "MISSING_BUREAU"
            ? [
                "No general duty to report to every bureau; verify any reported inaccuracy.",
              ]
            : ["15 U.S.C. § 1681e(b)", "15 U.S.C. § 1681i"],
      });
    const balances = [
      ...new Set(
        versions.map(v => v.balance).filter((n): n is number => n !== null),
      ),
    ];
    if (
      balances.length > 1 &&
      Math.max(...balances) - Math.min(...balances) >= 25
    )
      add(
        "BALANCE_MISMATCH",
        "Balance differs across bureaus",
        versions
          .map(
            v =>
              `${v.bureau}: ${v.balance === null ? "not shown" : "$" + v.balance}`,
          )
          .join(" • "),
      );
    const statuses = new Set(
      versions
        .map(v =>
          v.status
            .toLowerCase()
            .replace("charge off", "charge-off")
            .replace("paid in full", "paid")
            .trim(),
        )
        .filter(s => s !== "unknown"),
    );
    if (statuses.size > 1)
      add(
        "STATUS_MISMATCH",
        "Account status does not match",
        versions.map(v => `${v.bureau}: ${v.status}`).join(" • "),
      );
    if (
      imported.size >= 2 &&
      bureaus.length &&
      [...imported].some(b => !bureaus.includes(b))
    )
      add(
        "MISSING_BUREAU",
        "Reporting differs by bureau",
        `Present on ${bureaus.join(", ")}; not found on ${[...imported].filter(b => !bureaus.includes(b)).join(", ")}. This is a review flag, not proof of an error.`,
        "MEDIUM",
        versions,
        [...new Set([...bureaus, ...imported])],
      );
    for (const b of bureaus) {
      const dup = versions.filter(v => v.bureau === b);
      if (dup.length > 1)
        add(
          "POSSIBLE_DUPLICATE",
          "Possible duplicate tradeline",
          `${dup.length} similar entries on ${b}. Confirm identity before disputing.`,
          "HIGH",
          dup,
          [b],
        );
    }
    if (
      versions.length === 1 &&
      /charge|collection|late|delinquent|past due|repossession/i.test(
        versions[0].status + versions[0].paymentStatus,
      )
    )
      add(
        "REVIEW_NEEDED",
        "Negative item needs verification",
        "Compare the report with your statements before choosing a dispute reason.",
        "MEDIUM",
      );
  }
  return findings.sort(
    (a, b) =>
      ["HIGH", "MEDIUM", "LOW"].indexOf(a.severity) -
        ["HIGH", "MEDIUM", "LOW"].indexOf(b.severity) ||
      a.creditor.localeCompare(b.creditor),
  );
}
export const PORTALS = {
  experian: "https://www.experian.com/disputes/main.html",
  equifax:
    "https://www.equifax.com/personal/credit-report-services/credit-dispute/",
  transunion: "https://www.transunion.com/credit-disputes/dispute-your-credit",
  cfpb: "https://www.consumerfinance.gov/complaint/",
  unknown: "",
};
export const STEPS = [
  [
    "Review facts",
    "Compare with dated original reports and supporting records. Differences may reflect reporting dates or parsing errors.",
  ],
  [
    "Open the official portal",
    "Enter credentials yourself. CreditRepairAI cannot read the portal or operate its form.",
  ],
  [
    "Choose the reported item",
    "Choose the company and account matching your original report. Follow current portal instructions.",
  ],
  [
    "Copy your explanation",
    "Copy only reviewed facts. Adjust the chunk limit to the field size shown by the portal.",
  ],
  [
    "Attach evidence",
    "Attach copies of supporting records yourself. Keep the originals.",
  ],
  [
    "Review and submit yourself",
    "Review every answer and personally press Submit in the official portal.",
  ],
  [
    "Record the receipt",
    "Record the confirmation only after actual submission. Check-in dates are not response guarantees.",
  ],
];
export function chunks(text: string, limit: number): string[] {
  if (!Number.isInteger(limit) || limit < 100 || limit > 10000)
    throw Error("Chunk limit must be 100–10,000.");
  const out: string[] = [];
  let rest = text.trim();
  while (rest.length > limit) {
    let end = rest.lastIndexOf(" ", limit);
    if (end < limit / 2) end = limit;
    out.push(rest.slice(0, end));
    rest = rest.slice(end).trimStart();
  }
  if (rest) out.push(rest);
  return out;
}
export const canReview = (f: Filing) =>
  [f.problem, f.companyResponse, f.resolution, f.evidence].every(
    v => !/\[(?:ADD|YOUR|INSERT|VERIFY)\b/i.test(v),
  ) &&
  !!f.problem.trim() &&
  !!f.resolution.trim() &&
  !!f.evidence.trim();
export function submitReceipt(f: Filing, now: number): Filing {
  if (
    !f.reviewed ||
    !canReview(f) ||
    f.step !== 6 ||
    f.submittedAt ||
    !f.confirmation.trim() ||
    now <= 0
  )
    throw Error(
      "Review complete facts, reach the receipt step, and enter the actual confirmation first.",
    );
  return { ...f, submittedAt: now };
}
export function cfpbReady(
  d: Dispute,
  filings: Filing[],
  mailings: Mailing[],
  now = Date.now(),
): boolean {
  const valid = (t: number | undefined, start: number) =>
    !!t && t >= start && t <= now;
  if (
    mailings.some(
      m =>
        m.disputeId === d.id &&
        m.mailedAt > 0 &&
        m.mailedAt <= now &&
        (valid(m.responseReceivedAt, m.mailedAt) ||
          (valid(m.deliveredAt, m.mailedAt) &&
            now - m.deliveredAt! >= 45 * DAY)),
    )
  )
    return true;
  return (
    !!d.sentAt &&
    d.sentAt > 0 &&
    d.sentAt <= now &&
    (filings.some(
      f =>
        f.disputeId === d.id &&
        f.portal !== "cfpb" &&
        valid(f.submittedAt, d.sentAt!) &&
        valid(f.responseReceivedAt, f.submittedAt!),
    ) ||
      (!!d.craResponseDueAt &&
        d.craResponseDueAt >= d.sentAt &&
        d.craResponseDueAt <= now))
  );
}
export function snapshot(state: CaseState, caseId?: string) {
  const d = caseId ? state.disputes.find(d => d.id === caseId) : undefined;
  if (caseId && !d) throw Error("Selected dispute no longer exists.");
  const accounts = d
    ? state.accounts.filter(
        a => a.creditor === d.creditor && d.bureaus.includes(a.bureau),
      )
    : state.accounts;
  const findings = d
    ? state.findings.filter(f => f.id === d.findingId)
    : state.findings;
  const disputes = d ? [d] : state.disputes;
  if (accounts.length > 200 || findings.length > 100 || disputes.length > 100)
    throw Error(
      "Case exceeds AI limits. Select one dispute for focused help. Nothing was sent.",
    );
  return {
    jurisdictionCode: state.jurisdictionCode,
    reports: d
      ? state.reports.filter(r => d.bureaus.includes(r.bureau))
      : state.reports,
    accounts,
    findings,
    disputes: disputes.map(dispute => {
      const mail = state.mailings.filter(
        m =>
          m.disputeId === dispute.id &&
          m.mailedAt > 0 &&
          m.mailedAt <= Date.now(),
      );
      return {
        ...dispute,
        responseReceived:
          state.filings.some(
            f =>
              f.disputeId === dispute.id &&
              f.portal !== "cfpb" &&
              !!f.submittedAt &&
              f.submittedAt > 0 &&
              !!f.responseReceivedAt &&
              f.responseReceivedAt >= f.submittedAt &&
              f.responseReceivedAt <= Date.now(),
          ) ||
          mail.some(
            m =>
              !!m.responseReceivedAt &&
              m.responseReceivedAt >= m.mailedAt &&
              m.responseReceivedAt <= Date.now(),
          ),
        mailingHistory: mail,
        sentAt:
          dispute.sentAt || mail.map(m => m.mailedAt).sort((a, b) => a - b)[0],
        craResponseDueAt:
          dispute.craResponseDueAt ||
          mail
            .filter(
              m =>
                m.deliveredAt &&
                m.deliveredAt >= m.mailedAt &&
                m.deliveredAt <= Date.now(),
            )
            .map(m => m.deliveredAt! + 45 * DAY)
            .sort((a, b) => a - b)[0],
      };
    }),
  };
}
