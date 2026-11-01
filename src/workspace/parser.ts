import { id, type Account, type Bureau, type Report } from "./model.ts";

export function validateText(text: string) {
  if (text.length > 1_000_000)
    throw Error("Report exceeds 1,000,000 characters.");
  let line = 0,
    fields = 1;
  for (const c of text) {
    if ("\r\n|".includes(c)) {
      line = 0;
      if (++fields > 50_000) throw Error("Report exceeds the field limit.");
    } else if (++line > 4096)
      throw Error("Report line exceeds 4,096 characters.");
  }
}
const heading =
  /^(?:creditor|account name|company|subscriber)\s*[:#-]?\s*(.*)$/i;
const number = /^(?:account(?:\s*(?:number|no\.?|#))?|acct)\s*[:#-]/i;
const labels =
  "account number|account no[.]?|account #|acct|account status|payment status|pay status|date opened|date reported|last reported|original creditor|account type|credit limit|high balance|highest balance|current balance|responsibility|ownership|past due|balance|status|opened|limit|type|remarks|comments|creditor|account name|company|subscriber".replaceAll(
    " ",
    "\\s*",
  );
const empty = new RegExp(`^(?:${labels})\\s*[:#-]?$`, "i");
const field = new RegExp(`^(?:${labels})\\s*[:#-]`, "i");
const ignored = new Set([
  "ACCOUNT INFORMATION",
  "ACCOUNT HISTORY",
  "CREDIT REPORT",
  "PERSONAL INFORMATION",
  "SUMMARY",
]);
export function detectBureau(name: string, text: string): Bureau {
  const source = (name.slice(0, 256) + text.slice(0, 8000))
    .toLowerCase()
    .replace(/\s+/g, "");
  return (
    (["transunion", "experian", "equifax"] as Bureau[]).find(b =>
      source.includes(b),
    ) || "unknown"
  );
}
export function money(raw: string): number | null {
  if (raw.length > 64) return null;
  const n = raw.replace(/[$ ]/g, "").trim();
  const whole = /^[0-9]{1,3}(?:\.[0-9]{3})+$/.test(n)
    ? n.replaceAll(".", "")
    : /^(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\.[0-9]{1,2})?$/.test(n)
      ? n.split(".")[0].replaceAll(",", "")
      : "";
  const value = whole ? Number(whole) : NaN;
  return Number.isSafeInteger(value) && value >= 0 && value <= 2147483647
    ? value
    : null;
}
export function parseText(
  text: string,
  fileName: string,
  extractionMethod = "Embedded PDF text",
): { report: Report; accounts: Account[] } {
  validateText(text);
  const bureau = detectBureau(fileName, text);
  const scoreMatch =
    /\b(?:credit\s+score|fico[®™ ]*(?:score)?|vantage(?:score)?[®™ ]*(?:score)?)[^0-9]{0,25}([3-8][0-9]{2})(?![0-9])/i.exec(
      text,
    ) ||
    /(?:your|current) score[^0-9]{0,15}([3-8][0-9]{2})(?![0-9])/i.exec(text);
  const value = Number(scoreMatch?.[1]);
  const score = value >= 300 && value <= 850 ? value : null;
  const raw = text
    .split(/[\r\n|]/)
    .map(l =>
      l
        .replace(/[\u00a0\u202f]/g, " ")
        .replace(/\s+/g, " ")
        .trim()
        .replace(/^date\s+[o0]?pened\b/i, "Date Opened"),
    )
    .filter(Boolean);
  const lines: string[] = [];
  for (let i = 0; i < raw.length; i++) {
    if (empty.test(raw[i]) && raw[i + 1] && !field.test(raw[i + 1]))
      lines.push(raw[i].replace(/[:# -]+$/, "") + ": " + raw[++i]);
    else lines.push(raw[i]);
  }
  const starts = lines.flatMap((l, i) =>
    heading.test(l) ||
    (l.length >= 3 &&
      l.length <= 64 &&
      /[a-z]/i.test(l) &&
      !l.includes(":") &&
      !ignored.has(l.toUpperCase()) &&
      !field.test(l) &&
      !/^[*Xx -]*[0-9]{3,}$/.test(l) &&
      number.test(lines[i + 1] || ""))
      ? [i]
      : [],
  );
  if (starts.length > 150)
    throw Error(
      "Report exceeds 150 account sections. Import smaller sections.",
    );
  const accounts = starts.flatMap((start, index): Account[] => {
    const window = lines.slice(start, starts[index + 1] ?? lines.length);
    const creditor = (heading.exec(window[0])?.[1] || window[0])
      .replace(/[^A-Za-z0-9 &.'/-]/g, " ")
      .replace(/\s+/g, " ")
      .replace(/^[ :\-]+|[ :\-]+$/g, "")
      .slice(0, 64);
    if (creditor.length < 3 || ignored.has(creditor.toUpperCase())) return [];
    const get = (...names: string[]): string => {
      for (const name of names) {
        const p = new RegExp(
          `^${name.replaceAll(" ", "\\s*")}\\s*[:#-]?\\s*(.+)$`,
          "i",
        );
        for (const line of window) {
          const m = p.exec(line);
          if (m) return m[1].trim();
        }
      }
      return "";
    };
    const cash = (...names: string[]) => {
      for (const name of names) {
        const p = new RegExp(
          `^${name.replaceAll(" ", "\\s*")}\\s*[:#-]?\\s*\\$?([0-9][0-9,. ]*)`,
          "i",
        );
        for (const line of window) {
          const m = p.exec(line);
          if (m && money(m[1]) !== null) return money(m[1]);
        }
      }
      return null;
    };
    const date = (s: string) =>
      s
        .replace(/(?<=\d)\s+(?=\d)/g, "")
        .replace(/\s*\/\s*/g, "/")
        .slice(0, 40);
    const n = window.find(l => number.test(l)) || "";
    const suffix =
      /^(?:account(?:\s*(?:number|no\.?|#))?|acct)\s*[:#-]+\s*([*Xx\s-]*[A-Z0-9-]{3,})/i.exec(
        n,
      )?.[1] || "";
    return [
      {
        id: id(),
        creditor,
        accountSuffix: suffix.replace(/[^a-z0-9]/gi, "").slice(-4),
        bureau,
        balance: cash("current balance", "balance"),
        status: (
          get("account status", "status") ||
          window
            .find(l =>
              /charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid/i.test(
                l,
              ),
            )
            ?.match(
              /charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid/i,
            )?.[0] ||
          "Unknown"
        ).slice(0, 80),
        paymentStatus: get("payment status", "pay status").slice(0, 80),
        openedDate: date(get("date opened", "opened")),
        accountType: get("account type", "type").slice(0, 80),
        responsibility: get("responsibility", "ownership").slice(0, 80),
        dateReported: date(get("date reported", "last reported")),
        originalCreditor: get("original creditor").slice(0, 80),
        pastDue: cash("past due"),
        creditLimit: cash("credit limit", "limit"),
        highBalance: cash("high balance", "highest balance"),
        remarks: get("remarks", "comments").slice(0, 200),
      },
    ];
  });
  return {
    report: {
      id: id(),
      fileName,
      bureau,
      score,
      accountCount: accounts.length,
      importedAt: Date.now(),
      extractionMethod,
    },
    accounts,
  };
}
