var CreditDomain = (() => {
  var __defProp = Object.defineProperty;
  var __getOwnPropDesc = Object.getOwnPropertyDescriptor;
  var __getOwnPropNames = Object.getOwnPropertyNames;
  var __hasOwnProp = Object.prototype.hasOwnProperty;
  var __export = (target, all) => {
    for (var name in all)
      __defProp(target, name, { get: all[name], enumerable: true });
  };
  var __copyProps = (to, from, except, desc) => {
    if (from && typeof from === "object" || typeof from === "function") {
      for (let key of __getOwnPropNames(from))
        if (!__hasOwnProp.call(to, key) && key !== except)
          __defProp(to, key, { get: () => from[key], enumerable: !(desc = __getOwnPropDesc(from, key)) || desc.enumerable });
    }
    return to;
  };
  var __toCommonJS = (mod) => __copyProps(__defProp({}, "__esModule", { value: true }), mod);

  // src/shared/domain.ts
  var domain_exports = {};
  __export(domain_exports, {
    ADDRESSES: () => ADDRESSES,
    POLICY_DIGEST: () => POLICY_DIGEST,
    POLICY_VERSION: () => POLICY_VERSION,
    PORTALS: () => PORTALS,
    STEPS: () => STEPS,
    analyze: () => analyze,
    canReview: () => canReview,
    cfpbReady: () => cfpbReady,
    chunks: () => chunks,
    composeLetter: () => composeLetter,
    emptyCase: () => emptyCase,
    parseAnswerDraft: () => parseAnswerDraft,
    parseText: () => parseText,
    redactText: () => redactText,
    sanitizeSnapshot: () => sanitizeSnapshot,
    snapshot: () => snapshot,
    submitReceipt: () => submitReceipt,
    validateText: () => validateText
  });

  // src/workspace/model.ts
  var emptyCase = () => ({
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
    frozenBureaus: []
  });
  var id = () => {
    if (typeof crypto.randomUUID === "function") return crypto.randomUUID();
    const bytes = crypto.getRandomValues(new Uint8Array(16));
    bytes[6] = bytes[6] & 15 | 64;
    bytes[8] = bytes[8] & 63 | 128;
    const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
    return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
  };
  var DAY = 864e5;

  // src/workspace/parser.ts
  function validateText(text) {
    if (text.length > 1e6)
      throw Error("Report exceeds 1,000,000 characters.");
    let line = 0, fields = 1;
    for (const c of text) {
      if ("\r\n|".includes(c)) {
        line = 0;
        if (++fields > 5e4) throw Error("Report exceeds the field limit.");
      } else if (++line > 4096)
        throw Error("Report line exceeds 4,096 characters.");
    }
  }
  var heading = /^(?:creditor|account name|company|subscriber)\s*[:#-]?\s*(.*)$/i;
  var number = /^(?:account(?:\s*(?:number|no\.?|#))?|acct)\s*[:#-]/i;
  var labels = "account number|account no[.]?|account #|acct|account status|payment status|pay status|date opened|date reported|last reported|original creditor|account type|credit limit|high balance|highest balance|current balance|responsibility|ownership|past due|balance|status|opened|limit|type|remarks|comments|creditor|account name|company|subscriber".replaceAll(
    " ",
    "\\s*"
  );
  var empty = new RegExp(`^(?:${labels})\\s*[:#-]?$`, "i");
  var field = new RegExp(`^(?:${labels})\\s*[:#-]`, "i");
  var ignored = /* @__PURE__ */ new Set([
    "ACCOUNT INFORMATION",
    "ACCOUNT HISTORY",
    "CREDIT REPORT",
    "PERSONAL INFORMATION",
    "SUMMARY"
  ]);
  function detectBureau(name, text) {
    const source = (name.slice(0, 256) + text.slice(0, 8e3)).toLowerCase().replace(/\s+/g, "");
    return ["transunion", "experian", "equifax"].find(
      (b) => source.includes(b)
    ) || "unknown";
  }
  function money(raw) {
    if (raw.length > 64) return null;
    const n = raw.replace(/[$ ]/g, "").trim();
    const whole = /^[0-9]{1,3}(?:\.[0-9]{3})+$/.test(n) ? n.replaceAll(".", "") : /^(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\.[0-9]{1,2})?$/.test(n) ? n.split(".")[0].replaceAll(",", "") : "";
    const value = whole ? Number(whole) : NaN;
    return Number.isSafeInteger(value) && value >= 0 && value <= 2147483647 ? value : null;
  }
  function parseText(text, fileName, extractionMethod = "Embedded PDF text") {
    validateText(text);
    const bureau = detectBureau(fileName, text);
    const scoreMatch = /\b(?:credit\s+score|fico[®™ ]*(?:score)?|vantage(?:score)?[®™ ]*(?:score)?)[^0-9]{0,25}([3-8][0-9]{2})(?![0-9])/i.exec(
      text
    ) || /(?:your|current) score[^0-9]{0,15}([3-8][0-9]{2})(?![0-9])/i.exec(text);
    const value = Number(scoreMatch?.[1]);
    const score = value >= 300 && value <= 850 ? value : null;
    const raw = text.split(/[\r\n|]/).map(
      (l) => l.replace(/[\u00a0\u202f]/g, " ").replace(/\s+/g, " ").trim().replace(/^date\s+[o0]?pened\b/i, "Date Opened")
    ).filter(Boolean);
    const lines = [];
    for (let i = 0; i < raw.length; i++) {
      if (empty.test(raw[i]) && raw[i + 1] && !field.test(raw[i + 1]))
        lines.push(raw[i].replace(/[:# -]+$/, "") + ": " + raw[++i]);
      else lines.push(raw[i]);
    }
    const starts = lines.flatMap(
      (l, i) => heading.test(l) || l.length >= 3 && l.length <= 64 && /[a-z]/i.test(l) && !l.includes(":") && !ignored.has(l.toUpperCase()) && !field.test(l) && !/^[*Xx -]*[0-9]{3,}$/.test(l) && number.test(lines[i + 1] || "") ? [i] : []
    );
    if (starts.length > 150)
      throw Error(
        "Report exceeds 150 account sections. Import smaller sections."
      );
    const accounts = starts.flatMap((start, index) => {
      const window = lines.slice(start, starts[index + 1] ?? lines.length);
      const creditor = (heading.exec(window[0])?.[1] || window[0]).replace(/[^A-Za-z0-9 &.'/-]/g, " ").replace(/\s+/g, " ").replace(/^[ :\-]+|[ :\-]+$/g, "").slice(0, 64);
      if (creditor.length < 3 || ignored.has(creditor.toUpperCase())) return [];
      const get = (...names) => {
        for (const name of names) {
          const p = new RegExp(
            `^${name.replaceAll(" ", "\\s*")}\\s*[:#-]?\\s*(.+)$`,
            "i"
          );
          for (const line of window) {
            const m = p.exec(line);
            if (m) return m[1].trim();
          }
        }
        return "";
      };
      const cash = (...names) => {
        for (const name of names) {
          const p = new RegExp(
            `^${name.replaceAll(" ", "\\s*")}\\s*[:#-]?\\s*\\$?([0-9][0-9,. ]*)`,
            "i"
          );
          for (const line of window) {
            const m = p.exec(line);
            if (m && money(m[1]) !== null) return money(m[1]);
          }
        }
        return null;
      };
      const date = (s) => s.replace(/(?<=\d)\s+(?=\d)/g, "").replace(/\s*\/\s*/g, "/").slice(0, 40);
      const n = window.find((l) => number.test(l)) || "";
      const suffix = /^(?:account(?:\s*(?:number|no\.?|#))?|acct)\s*[:#-]+\s*([*Xx\s-]*[A-Z0-9-]{3,})/i.exec(
        n
      )?.[1] || "";
      return [
        {
          id: id(),
          creditor,
          accountSuffix: suffix.replace(/[^a-z0-9]/gi, "").slice(-4),
          bureau,
          balance: cash("current balance", "balance"),
          status: (get("account status", "status") || window.find(
            (l) => /charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid/i.test(
              l
            )
          )?.match(
            /charge[ -]?off|collection|repossession|late|delinquent|past due|closed|paid/i
          )?.[0] || "Unknown").slice(0, 80),
          paymentStatus: get("payment status", "pay status").slice(0, 80),
          openedDate: date(get("date opened", "opened")),
          accountType: get("account type", "type").slice(0, 80),
          responsibility: get("responsibility", "ownership").slice(0, 80),
          dateReported: date(get("date reported", "last reported")),
          originalCreditor: get("original creditor").slice(0, 80),
          pastDue: cash("past due"),
          creditLimit: cash("credit limit", "limit"),
          highBalance: cash("high balance", "highest balance"),
          remarks: get("remarks", "comments").slice(0, 200)
        }
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
        extractionMethod
      },
      accounts
    };
  }

  // src/workspace/analysis.ts
  function analyze(accounts, reports) {
    const imported = new Set(
      reports.map((r) => r.bureau).filter((b) => b !== "unknown")
    );
    const groups = /* @__PURE__ */ new Map();
    for (const a of accounts) {
      const name = a.creditor.toLowerCase().replace(/[^a-z0-9]/g, "");
      const key = name + (a.accountSuffix ? ":suffix:" + a.accountSuffix.toUpperCase() : a.openedDate ? ":opened:" + (a.openedDate + ":" + a.accountType).toLowerCase().replace(/[^a-z0-9:]/g, "") : ":unmatched:" + a.id);
      groups.set(key, [...groups.get(key) || [], a]);
    }
    const findings = [];
    for (const versions of groups.values()) {
      const bureaus = [
        ...new Set(versions.map((v) => v.bureau).filter((b) => b !== "unknown"))
      ];
      const creditor = versions[0].creditor;
      const add = (type, title, detail, severity = "HIGH", list = versions, bs = bureaus) => findings.push({
        id: id(),
        type,
        title,
        detail,
        severity,
        creditor,
        bureaus: bs,
        accountIds: list.map((a) => a.id),
        legalBasis: type === "MISSING_BUREAU" ? [
          "No general duty to report to every bureau; verify any reported inaccuracy."
        ] : ["15 U.S.C. \xA7 1681e(b)", "15 U.S.C. \xA7 1681i"]
      });
      const balances = [
        ...new Set(
          versions.map((v) => v.balance).filter((n) => n !== null)
        )
      ];
      if (balances.length > 1 && Math.max(...balances) - Math.min(...balances) >= 25)
        add(
          "BALANCE_MISMATCH",
          "Balance differs across bureaus",
          versions.map(
            (v) => `${v.bureau}: ${v.balance === null ? "not shown" : "$" + v.balance}`
          ).join(" \u2022 ")
        );
      const statuses = new Set(
        versions.map(
          (v) => v.status.toLowerCase().replace("charge off", "charge-off").replace("paid in full", "paid").trim()
        ).filter((s) => s !== "unknown")
      );
      if (statuses.size > 1)
        add(
          "STATUS_MISMATCH",
          "Account status does not match",
          versions.map((v) => `${v.bureau}: ${v.status}`).join(" \u2022 ")
        );
      if (imported.size >= 2 && bureaus.length && [...imported].some((b) => !bureaus.includes(b)))
        add(
          "MISSING_BUREAU",
          "Reporting differs by bureau",
          `Present on ${bureaus.join(", ")}; not found on ${[...imported].filter((b) => !bureaus.includes(b)).join(", ")}. This is a review flag, not proof of an error.`,
          "MEDIUM",
          versions,
          [.../* @__PURE__ */ new Set([...bureaus, ...imported])]
        );
      for (const b of bureaus) {
        const dup = versions.filter((v) => v.bureau === b);
        if (dup.length > 1)
          add(
            "POSSIBLE_DUPLICATE",
            "Possible duplicate tradeline",
            `${dup.length} similar entries on ${b}. Confirm identity before disputing.`,
            "HIGH",
            dup,
            [b]
          );
      }
      if (versions.length === 1 && /charge|collection|late|delinquent|past due|repossession/i.test(
        versions[0].status + versions[0].paymentStatus
      ))
        add(
          "REVIEW_NEEDED",
          "Negative item needs verification",
          "Compare the report with your statements before choosing a dispute reason.",
          "MEDIUM"
        );
    }
    return findings.sort(
      (a, b) => ["HIGH", "MEDIUM", "LOW"].indexOf(a.severity) - ["HIGH", "MEDIUM", "LOW"].indexOf(b.severity) || a.creditor.localeCompare(b.creditor)
    );
  }
  var PORTALS = {
    experian: "https://www.experian.com/disputes/main.html",
    equifax: "https://www.equifax.com/personal/credit-report-services/credit-dispute/",
    transunion: "https://www.transunion.com/credit-disputes/dispute-your-credit",
    cfpb: "https://www.consumerfinance.gov/complaint/",
    unknown: ""
  };
  var STEPS = [
    [
      "Review facts",
      "Compare with dated original reports and supporting records. Differences may reflect reporting dates or parsing errors."
    ],
    [
      "Open the official portal",
      "Enter credentials yourself. CreditRepairAI cannot read the portal or operate its form."
    ],
    [
      "Choose the reported item",
      "Choose the company and account matching your original report. Follow current portal instructions."
    ],
    [
      "Copy your explanation",
      "Copy only reviewed facts. Adjust the chunk limit to the field size shown by the portal."
    ],
    [
      "Attach evidence",
      "Attach copies of supporting records yourself. Keep the originals."
    ],
    [
      "Review and submit yourself",
      "Review every answer and personally press Submit in the official portal."
    ],
    [
      "Record the receipt",
      "Record the confirmation only after actual submission. Check-in dates are not response guarantees."
    ]
  ];
  function chunks(text, limit) {
    if (!Number.isInteger(limit) || limit < 100 || limit > 1e4)
      throw Error("Chunk limit must be 100\u201310,000.");
    const out = [];
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
  var canReview = (f) => [f.problem, f.companyResponse, f.resolution, f.evidence].every(
    (v) => !/\[(?:ADD|YOUR|INSERT|VERIFY)\b/i.test(v)
  ) && !!f.problem.trim() && !!f.resolution.trim() && !!f.evidence.trim();
  function submitReceipt(f, now) {
    if (!f.reviewed || !canReview(f) || f.step !== 6 || f.submittedAt || !f.confirmation.trim() || now <= 0)
      throw Error(
        "Review complete facts, reach the receipt step, and enter the actual confirmation first."
      );
    return { ...f, submittedAt: now };
  }
  function cfpbReady(d, filings, mailings, now = Date.now()) {
    const valid = (t, start) => !!t && t >= start && t <= now;
    if (mailings.some(
      (m) => m.disputeId === d.id && m.mailedAt > 0 && m.mailedAt <= now && (valid(m.responseReceivedAt, m.mailedAt) || valid(m.deliveredAt, m.mailedAt) && now - m.deliveredAt >= 45 * DAY)
    ))
      return true;
    return !!d.sentAt && d.sentAt > 0 && d.sentAt <= now && (filings.some(
      (f) => f.disputeId === d.id && f.portal !== "cfpb" && valid(f.submittedAt, d.sentAt) && valid(f.responseReceivedAt, f.submittedAt)
    ) || !!d.craResponseDueAt && d.craResponseDueAt >= d.sentAt && d.craResponseDueAt <= now);
  }
  function snapshot(state, caseId) {
    const d = caseId ? state.disputes.find((d2) => d2.id === caseId) : void 0;
    if (caseId && !d) throw Error("Selected dispute no longer exists.");
    const accounts = d ? state.accounts.filter(
      (a) => a.creditor === d.creditor && d.bureaus.includes(a.bureau)
    ) : state.accounts;
    const findings = d ? state.findings.filter((f) => f.id === d.findingId) : state.findings;
    const disputes = d ? [d] : state.disputes;
    if (accounts.length > 200 || findings.length > 100 || disputes.length > 100)
      throw Error(
        "Case exceeds AI limits. Select one dispute for focused help. Nothing was sent."
      );
    return {
      jurisdictionCode: state.jurisdictionCode,
      reports: d ? state.reports.filter((r) => d.bureaus.includes(r.bureau)) : state.reports,
      accounts,
      findings,
      disputes: disputes.map((dispute) => {
        const mail = state.mailings.filter(
          (m) => m.disputeId === dispute.id && m.mailedAt > 0 && m.mailedAt <= Date.now()
        );
        return {
          ...dispute,
          responseReceived: state.filings.some(
            (f) => f.disputeId === dispute.id && f.portal !== "cfpb" && !!f.submittedAt && f.submittedAt > 0 && !!f.responseReceivedAt && f.responseReceivedAt >= f.submittedAt && f.responseReceivedAt <= Date.now()
          ) || mail.some(
            (m) => !!m.responseReceivedAt && m.responseReceivedAt >= m.mailedAt && m.responseReceivedAt <= Date.now()
          ),
          mailingHistory: mail,
          sentAt: dispute.sentAt || mail.map((m) => m.mailedAt).sort((a, b) => a - b)[0],
          craResponseDueAt: dispute.craResponseDueAt || mail.filter(
            (m) => m.deliveredAt && m.deliveredAt >= m.mailedAt && m.deliveredAt <= Date.now()
          ).map((m) => m.deliveredAt + 45 * DAY).sort((a, b) => a - b)[0]
        };
      })
    };
  }

  // src/workspace/answerDraft.ts
  function parseAnswerDraft(answer) {
    const complete = answer.trimEnd();
    if (!complete.endsWith("\nEND_DRAFT"))
      throw Error("AI returned an incomplete draft. No facts were changed.");
    const body = complete.slice(0, -10), labels2 = [
      "WHAT_HAPPENED",
      "COMPANY_RESPONSE",
      "REQUESTED_REVIEW",
      "EVIDENCE"
    ];
    const matches = [
      ...body.matchAll(
        /^(WHAT_HAPPENED|COMPANY_RESPONSE|REQUESTED_REVIEW|EVIDENCE):[ \t]*$/gm
      )
    ];
    if (matches.length !== 4 || matches.some((m, i) => m[1] !== labels2[i]))
      throw Error("Draft headings were incomplete. No facts were changed.");
    const values = matches.map(
      (m, i) => body.slice(m.index + m[0].length, matches[i + 1]?.index ?? body.length).trim()
    );
    if (values.some((v) => !v || v.length > 2e3))
      throw Error("Draft fields were empty or too long.");
    return {
      problem: values[0],
      companyResponse: values[1],
      resolution: values[2],
      evidence: values[3]
    };
  }

  // src/workspace/letterText.ts
  var ADDRESSES = {
    experian: {
      address: "Experian\nDispute by Mail\nP.O. Box 4500\nAllen, TX 75013",
      source: "https://www.experian.com/help/dispute-credit/"
    },
    equifax: {
      address: "Equifax Information Services LLC\nP.O. Box 740256\nAtlanta, GA 30374",
      source: "https://assets.equifax.com/assets/personal/Dispute.pdf"
    },
    transunion: {
      address: "TransUnion Consumer Solutions\nP.O. Box 2000\nChester, PA 19016-2000",
      source: "https://www.transunion.com/credit-disputes/dispute-your-credit/mail-or-phone"
    },
    unknown: { address: "", source: "" }
  };
  function composeLetter(d, bureau, name, address, facts, evidence, resolution, date = (/* @__PURE__ */ new Date()).toLocaleDateString()) {
    if (bureau === "unknown" || !d.bureaus.includes(bureau))
      throw Error("Choose a reporting bureau.");
    for (const [v, max] of [
      [name, 160],
      [address, 600],
      [facts, 1e4],
      [evidence, 2e3],
      [resolution, 2e3]
    ])
      if (!v.trim() || v.length > max)
        throw Error("Complete every letter field within the length limits.");
    return `${name.trim()}
${address.trim()}

${date}

${ADDRESSES[bureau].address}

Re: Credit report dispute - ${d.creditor}

To the consumer dispute department:

I am requesting an investigation of the following information in my ${bureau} consumer report.

${facts.trim()}

Requested review/correction:
${resolution.trim()}

Enclosed copies of supporting records:
${evidence.trim()}

Please review the information and supporting records, correct any inaccurate or incomplete reporting you identify, and send me your written investigation results and an updated report when applicable.

Sincerely,

Signature: ____________________________________
${name.trim()}`;
  }

  // convex/agentSafety.ts
  var accountPlaceholders = /* @__PURE__ */ new Set(["ending", "masked", "redacted", "unknown", "suffix", "digits", "optional", "required", "provided", "should", "available", "displayed", "hidden"]);
  var maskAccount = (raw) => `account \u2022\u2022\u2022\u2022${raw.replace(/[^A-Z0-9]/gi, "").slice(-4)}`;
  function redactText(text, maxLength = 4e3, preserveAnswerLabels = false) {
    return text.replace(/\b\d{3}[- ]?\d{2}[- ]?\d{4}\b/g, "[SSN REDACTED]").replace(/\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/gi, "[EMAIL REDACTED]").replace(/(?<!\d)(?:\+?1[-. ]?)?(?:\(\d{3}\)|\d{3})[-. ]?\d{3}[-. ]?\d{4}(?!\d)/g, "[PHONE REDACTED]").replace(/\b(?:dob|date of birth|born)\s*[:#-]?\s*(?:\d{1,2}[/-]){2}\d{2,4}\b/gi, "DOB: [REDACTED]").replace(/\b(?:account|acct)\b\s*(?:(?:number\b|no\b\.?)\s*[:#-]?\s*|[:#]\s*)([A-Z0-9-]{6,})\b/gi, (match, raw) => accountPlaceholders.has(raw.toLowerCase()) || preserveAnswerLabels && /[\r\n]/.test(match) && !/\d/.test(raw) ? match : maskAccount(raw)).replace(/\b(?:account|acct)\s*(?=[A-Z0-9-]*\d)([A-Z0-9-]{6,})\b/gi, (_, raw) => maskAccount(raw)).replace(/(?<!\d)\d{9,19}(?!\d)/g, "[LONG NUMBER REDACTED]").slice(0, maxLength);
  }
  function sanitizeSnapshot(snapshot2) {
    const fields = {
      reports: ["bureau", "score", "accountCount", "importedAt"],
      accounts: ["creditor", "accountSuffix", "bureau", "status", "balance", "paymentStatus", "openedDate", "accountType", "responsibility", "dateReported", "originalCreditor", "pastDue", "creditLimit", "highBalance", "remarks"],
      findings: ["id", "type", "title", "detail", "severity", "creditor", "bureaus", "legalBasis"],
      disputes: ["id", "findingId", "creditor", "bureaus", "reason", "status", "createdAt", "sentAt", "craResponseDueAt", "responseReceived", "mailingHistory"]
    };
    const clean = { jurisdictionCode: typeof snapshot2.jurisdictionCode === "string" ? snapshot2.jurisdictionCode.slice(0, 2) : "" };
    for (const [key, allow] of Object.entries(fields)) {
      clean[key] = (Array.isArray(snapshot2[key]) ? snapshot2[key] : []).map((raw) => {
        const row = raw && typeof raw === "object" ? raw : {};
        return Object.fromEntries(allow.filter((field2) => field2 in row).map((field2) => {
          const value = row[field2];
          if (field2 === "mailingHistory") {
            const history = (Array.isArray(value) ? value : []).slice(0, 3).flatMap((raw2) => {
              if (!raw2 || typeof raw2 !== "object") return [];
              const record = raw2;
              const bureau = String(record.bureau || "").toLowerCase();
              if (!["equifax", "experian", "transunion"].includes(bureau)) return [];
              const dates = Object.fromEntries(["mailedAt", "deliveredAt", "responseReceivedAt"].map((key2) => {
                const date = record[key2];
                return [key2, typeof date === "number" && Number.isSafeInteger(date) && date > 0 ? date : null];
              }));
              return [{ bureau, ...dates }];
            });
            return [field2, history];
          }
          const sanitized = typeof value === "string" ? redactText(value) : Array.isArray(value) ? value.slice(0, 20).map((v) => typeof v === "string" ? redactText(v) : null) : typeof value === "number" || typeof value === "boolean" || value === null ? value : null;
          return [field2, sanitized];
        }));
      });
    }
    return clean;
  }

  // convex/policyDocuments.ts
  var POLICY_VERSION = "2026-10-15.1";
  var POLICY_DIGEST = "faa313e2a1061b634808f6bcb5dcec1beb6a4d779bf5b180c81702e77bbbae42";
  return __toCommonJS(domain_exports);
})();
