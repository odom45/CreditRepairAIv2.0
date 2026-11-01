import { parseAnswerDraft } from "./answerDraft";
import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { onAuthStateChanged, type User } from "firebase/auth";
import {
  auth,
  configured,
  login,
  googleLogin,
  logout,
  resetPassword,
  verifyEmail,
  refreshUser,
  acceptPolicies,
  askAgent,
  deleteAccount,
  securePost,
  POLICY_VERSION,
  POLICY_DIGEST,
} from "./service";
import {
  BUREAUS,
  DAY,
  id,
  type Bureau,
  type CaseState,
  type Dispute,
  type Filing,
} from "./model";
import {
  analyze,
  canReview,
  cfpbReady,
  chunks,
  PORTALS,
  STEPS,
  submitReceipt,
} from "./analysis";
import { openVault, seal, unseal, vaultName } from "./vault";
import { parsePdf } from "./pdf";
import { ADDRESSES, composeLetter, download, saveLetterPdf } from "./letters";
import { redactText } from "../../convex/agentSafety";
import "./workspace.css";

type Panel =
  | "Overview"
  | "Reports"
  | "Analysis"
  | "Disputes"
  | "Guided filing"
  | "AI guide"
  | "Goals & scores"
  | "Account";
const panels: Panel[] = [
  "Overview",
  "Reports",
  "Analysis",
  "Disputes",
  "Guided filing",
  "AI guide",
  "Goals & scores",
  "Account",
];
const err = (e: unknown) =>
  e instanceof Error ? e.message : "The action could not complete.";
const label = (s: string) =>
  s === "transunion" ? "TransUnion" : s[0]?.toUpperCase() + s.slice(1);
const showDate = (t?: number) =>
  t ? new Date(t).toLocaleDateString() : "Not recorded";
function Field({ title, children }: { title: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{title}</span>
      {children}
    </label>
  );
}
function Link({ href, children }: { href: string; children: ReactNode }) {
  return (
    <a href={href} target="_blank" rel="noopener noreferrer">
      {children} ↗
    </a>
  );
}
function Card({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="case-card">
      <h2>{title}</h2>
      {children}
    </section>
  );
}
function Login({
  action,
  busy,
}: {
  action: (fn: () => Promise<void>) => void;
  busy: boolean;
}) {
  const [email, setEmail] = useState(""),
    [password, setPassword] = useState(""),
    [create, setCreate] = useState(false);
  return (
    <main className="auth-shell">
      <div className="brand-mark">C</div>
      <p className="eyebrow">PRIVATE CREDIT WORKSPACE</p>
      <h1>
        Understand your reports.
        <br />
        Take the next step.
      </h1>
      <p>
        Read reports on your device, verify differences, prepare supported
        disputes and track actual responses.
      </p>
      <Card title={create ? "Create your account" : "Welcome back"}>
        <form
          onSubmit={e => {
            e.preventDefault();
            action(async () => {
              await login(email, password, create);
              setPassword("");
            });
          }}
        >
          <Field title="Email">
            <input
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={e => setEmail(e.target.value)}
            />
          </Field>
          <Field title="Password">
            <input
              type="password"
              autoComplete={create ? "new-password" : "current-password"}
              minLength={6}
              required
              value={password}
              onChange={e => setPassword(e.target.value)}
            />
          </Field>
          <button disabled={busy || !configured}>
            {create ? "Create account" : "Sign in"}
          </button>
        </form>
        <button
          className="secondary"
          disabled={busy || !configured}
          onClick={() => action(googleLogin)}
        >
          Continue with Google
        </button>
        <div className="row">
          <button className="text-button" onClick={() => setCreate(!create)}>
            {create ? "Use an existing account" : "Create account"}
          </button>
          <button
            className="text-button"
            disabled={!email || busy}
            onClick={() =>
              action(async () => {
                await resetPassword(email);
                alert("If available, a reset email has been sent.");
              })
            }
          >
            Reset password
          </button>
        </div>
        {!configured && (
          <p className="warning">
            Firebase web configuration has not been supplied to this build.
            Sign-in is unavailable.
          </p>
        )}
        <p className="muted">
          Your session stays in memory. Closing or reloading this app requires a
          fresh sign-in.
        </p>
      </Card>
      <p className="legal">
        Educational information only — not legal or financial advice. No score,
        deletion, approval or timing guarantees.
      </p>
    </main>
  );
}
export default function Workspace() {
  const [user, setUser] = useState<User | null>(null),
    [checking, setChecking] = useState(true),
    [busy, setBusy] = useState(false),
    [message, setMessage] = useState(""),
    [progress, setProgress] = useState("");
  const [state, setState] = useState<CaseState | null>(null),
    [password, setPassword] = useState(""),
    [panel, setPanel] = useState<Panel>("Overview");
  const generation = useRef(0),
    key = useRef<CryptoKey | null>(null),
    salt = useRef<Uint8Array | null>(null),
    saveVersion = useRef(0),
    currentState = useRef<CaseState | null>(null);
  const [saveStatus, setSaveStatus] = useState(""),
    [terms, setTerms] = useState(false),
    [sharing, setSharing] = useState(false);
  const [selectedDispute, setSelectedDispute] = useState(""),
    [portal, setPortal] = useState<Bureau | "cfpb">("equifax"),
    [question, setQuestion] = useState(""),
    [caseId, setCaseId] = useState(""),
    [answer, setAnswer] = useState(""),
    [chunkLimit, setChunkLimit] = useState(1000);
  const [sender, setSender] = useState(""),
    [address, setAddress] = useState(""),
    [letterFacts, setLetterFacts] = useState(""),
    [evidence, setEvidence] = useState(""),
    [resolution, setResolution] = useState(""),
    [letterBureau, setLetterBureau] = useState<Bureau>("equifax");
  const [pendingDraft, setPendingDraft] = useState<{
    context: string;
    fields: ReturnType<typeof parseAnswerDraft>;
  } | null>(null);
  const [responseText, setResponseText] = useState(""),
    [billing, setBilling] = useState(""),
    [deletion, setDeletion] = useState("");
  useEffect(() => {
    if (!auth) {
      setChecking(false);
      return;
    }
    return onAuthStateChanged(auth, u => {
      generation.current++;
      key.current = null;
      salt.current = null;
      currentState.current = null;
      setState(null);
      setUser(u);
      setChecking(false);
      setPassword("");
      setAnswer("");
      setSender("");
      setAddress("");
      setLetterFacts("");
      setEvidence("");
      setResolution("");
      setResponseText("");
      setBusy(false);
      setMessage("");
      setSelectedDispute("");
      setCaseId("");
      setBilling("");
      setSaveStatus("");
    });
  }, []);
  useEffect(() => {
    if (!state || !user || !key.current || !salt.current) return;
    currentState.current = state;
    const epoch = generation.current,
      version = ++saveVersion.current,
      uid = user.uid;
    setSaveStatus("Encrypting changes…");
    void seal(state, key.current, salt.current, uid)
      .then(envelope => {
        if (saveVersion.current !== version) return;
        localStorage.setItem(vaultName(uid), JSON.stringify(envelope));
        if (generation.current === epoch)
          setSaveStatus("Saved encrypted on this device");
      })
      .catch(e => {
        if (generation.current === epoch && saveVersion.current === version) {
          setSaveStatus(
            "Save failed — keep this session open and export your vault",
          );
          setMessage(err(e));
        }
      });
  }, [state, user]);
  useEffect(() => {
    let lastActivity = Date.now();
    const activity = () => {
      lastActivity = Date.now();
    };
    const close = () => {
      generation.current++;
      key.current = null;
      currentState.current = null;
      setState(null);
      void logout();
    };
    const timer = setInterval(() => {
      if (auth?.currentUser && Date.now() - lastActivity > 5 * 60 * 1000)
        close();
    }, 15000);
    window.addEventListener("pointerdown", activity);
    window.addEventListener("keydown", activity);
    window.addEventListener("pagehide", close);
    return () => {
      clearInterval(timer);
      window.removeEventListener("pointerdown", activity);
      window.removeEventListener("keydown", activity);
      window.removeEventListener("pagehide", close);
    };
  }, []);
  const update = (fn: (s: CaseState) => CaseState) =>
    setState(s => (s ? fn(s) : s));
  const run = async (
    fn: (commit: typeof update, valid: () => boolean) => Promise<void>,
  ) => {
    const epoch = generation.current;
    const valid = () => generation.current === epoch;
    setBusy(true);
    setMessage("");
    setAnswer("");
    const commit: typeof update = fn => {
      if (valid()) update(fn);
    };
    try {
      await fn(commit, valid);
    } catch (e) {
      if (valid()) setMessage(err(e));
    } finally {
      if (valid()) {
        setBusy(false);
        setProgress("");
      }
    }
  };
  const loginAction = (fn: () => Promise<void>) => {
    setBusy(true);
    setMessage("");
    void fn()
      .catch(e => setMessage(err(e)))
      .finally(() => setBusy(false));
  };
  const signOut = () => {
    generation.current++;
    key.current = null;
    salt.current = null;
    currentState.current = null;
    setState(null);
    setUser(null);
    setAnswer("");
    setSender("");
    setAddress("");
    setResponseText("");
    void logout().catch(e => setMessage(err(e)));
  };
  const unlock = (e: FormEvent) => {
    e.preventDefault();
    void run(async (_, valid) => {
      if (!user) return;
      const vault = await openVault(user.uid, password);
      if (!valid()) return;
      key.current = vault.key;
      salt.current = vault.salt;
      setState(vault.state);
      setPassword("");
    });
  };
  const d = state?.disputes.find(d => d.id === selectedDispute);
  const filing = state?.filings.find(
    f => f.disputeId === selectedDispute && f.portal === portal,
  );
  const editFiling = (changes: Partial<Filing>) =>
    update(s => ({
      ...s,
      filings: s.filings.map(f =>
        f.id === filing?.id
          ? {
              ...f,
              ...changes,
              ...("reviewed" in changes ? {} : { reviewed: false }),
            }
          : f,
      ),
    }));
  const chooseFiling = (dispute: Dispute, p: Bureau | "cfpb") => {
    if (p === "unknown") return;
    if (
      p === "cfpb" &&
      state &&
      !cfpbReady(dispute, state.filings, state.mailings)
    ) {
      setMessage(
        "First record the bureau response or reach the recorded 45-day follow-up date.",
      );
      return;
    }
    setSelectedDispute(dispute.id);
    setPortal(p);
    setPanel("Guided filing");
    setAnswer("");
    update(s =>
      s.filings.some(f => f.disputeId === dispute.id && f.portal === p)
        ? s
        : {
            ...s,
            filings: [
              ...s.filings,
              {
                id: id(),
                disputeId: dispute.id,
                portal: p,
                step: 0,
                reviewed: false,
                problem: dispute.reason,
                companyResponse: "",
                resolution: "",
                evidence: "",
                confirmation: "",
              },
            ],
          },
    );
  };
  const ai = (prompt: string, scope = caseId || undefined, persist = true) =>
    void run(async (commit, valid) => {
      if (!user || !state) return;
      const reply = await askAgent(user, prompt, state, scope, persist);
      if (!valid()) return;
      setAnswer(reply);
      if (persist)
        commit(s => ({
          ...s,
          chat: [
            ...s.chat,
            {
              id: id(),
              fromUser: true,
              text: redactText(prompt),
              caseScopeId: scope,
              createdAt: Date.now(),
            },
            {
              id: id(),
              fromUser: false,
              text: reply,
              caseScopeId: scope,
              createdAt: Date.now(),
            },
          ].slice(-200),
        }));
    });
  if (checking)
    return <main className="auth-shell">Checking secure session…</main>;
  return (
    <div className="credit-workspace">
      {message && (
        <div role="alert" className="notice error">
          <span>{message}</span>
          <button className="text-button" onClick={() => setMessage("")}>
            Dismiss
          </button>
        </div>
      )}
      {!user ? (
        <Login action={loginAction} busy={busy} />
      ) : !state ? (
        <main className="auth-shell">
          <div className="brand-mark">C</div>
          <h1>Unlock your private workspace</h1>
          <p>
            Signed in as {user.email}. Your vault passphrase is separate from
            your account password. Use the same passphrase when transferring
            your encrypted vault between devices. A lost passphrase cannot be
            recovered.
          </p>
          <form onSubmit={unlock}>
            <Field title="Vault passphrase (12+ characters)">
              <input
                type="password"
                autoComplete="off"
                required
                minLength={12}
                value={password}
                onChange={e => setPassword(e.target.value)}
              />
            </Field>
            <button disabled={busy}>Unlock or create vault</button>
          </form>
          <button className="secondary" onClick={signOut}>
            Sign out
          </button>
        </main>
      ) : (
        <>
          <header className="workspace-header">
            <div className="row">
              <div className="brand-mark small">C</div>
              <div>
                <strong>CreditRepairAI</strong>
                <span className="version">Shared case workspace · Web</span>
              </div>
            </div>
            <div className="row">
              <span className="muted session-email">{user.email}</span>
              <button className="secondary" onClick={signOut}>
                Lock & sign out
              </button>
            </div>
          </header>
          <div className="workspace-body">
            <nav aria-label="Workspace">
              {panels.map(p => (
                <button
                  key={p}
                  aria-current={panel === p ? "page" : undefined}
                  className={panel === p ? "active" : ""}
                  onClick={() => {
                    setPanel(p);
                    setAnswer("");
                  }}
                >
                  {p}
                </button>
              ))}
              <p className="legal">
                Your reports stay on your device. AI receives a redacted case
                only with your consent.
              </p>
            </nav>
            <main className="workspace-content">
              <div className="page-heading">
                <div>
                  <p className="eyebrow">YOUR NEXT STEP, CLEARER</p>
                  <h1>{panel}</h1>
                </div>
                <span className="save-status" role="status">
                  {saveStatus}
                </span>
              </div>
              {!user.emailVerified && (
                <div className="notice warning">
                  <p>Verify your email to use the AI guide.</p>
                  <button
                    disabled={busy}
                    onClick={() =>
                      void run(async () => {
                        await verifyEmail(user);
                        setMessage("Verification email sent.");
                      })
                    }
                  >
                    Resend verification
                  </button>
                  <button
                    className="secondary"
                    disabled={busy}
                    onClick={() =>
                      void run(async (_, valid) => {
                        const verified = await refreshUser(user);
                        if (valid())
                          setMessage(
                            verified
                              ? "Email verified. You can ask the AI guide."
                              : "Email is not verified yet. Open the link in your inbox.",
                          );
                      })
                    }
                  >
                    Refresh verification
                  </button>
                </div>
              )}
              {(!state.policyAcceptance ||
                state.policyAcceptance.version !== POLICY_VERSION ||
                state.policyAcceptance.digest !== POLICY_DIGEST) && (
                <Card title="Before you begin">
                  <p>
                    Read the current <Link href="/legal/terms.html">terms</Link>
                    , <Link href="/legal/privacy.html">privacy policy</Link> and{" "}
                    <Link href="/legal/disclaimer.html">disclosure</Link>.
                  </p>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={terms}
                      onChange={e => setTerms(e.target.checked)}
                    />
                    I have read and accept the current terms and disclosure.
                  </label>
                  <label className="check">
                    <input
                      type="checkbox"
                      checked={sharing}
                      onChange={e => setSharing(e.target.checked)}
                    />
                    I consent to sending a redacted case snapshot to the secure
                    AI service. Sender details and original PDFs are excluded.
                  </label>
                  <button
                    disabled={!terms || busy}
                    onClick={() =>
                      void run(async commit => {
                        await acceptPolicies(user, sharing);
                        commit(s => ({
                          ...s,
                          onboardingComplete: true,
                          policyAcceptance: {
                            version: POLICY_VERSION,
                            digest: POLICY_DIGEST,
                            aiSharing: sharing,
                            acceptedAt: Date.now(),
                          },
                        }));
                      })
                    }
                  >
                    Save preferences
                  </button>
                  <p className="muted">
                    You may keep AI sharing off and use local report review,
                    drafting, and tracking.
                  </p>
                </Card>
              )}
              {progress && (
                <div role="status" className="notice">
                  {progress}
                </div>
              )}
              {panel === "Overview" && (
                <>
                  <div className="stats">
                    {[
                      [state.reports.length, "Reports imported"],
                      [state.findings.length, "Items to verify"],
                      [state.disputes.length, "Disputes tracked"],
                    ].map(([value, title]) => (
                      <Card key={title} title={String(title)}>
                        <strong className="stat">{value}</strong>
                      </Card>
                    ))}
                  </div>
                  <Card title="Your plan">
                    <ol className="plan">
                      <li>
                        Obtain original reports from{" "}
                        <Link href="https://www.annualcreditreport.com/">
                          AnnualCreditReport.com
                        </Link>
                        . Save each bureau as a PDF.
                      </li>
                      <li>
                        Import and compare the extracted details with the
                        originals.
                      </li>
                      <li>
                        Review each flag and gather records before preparing a
                        supported dispute.
                      </li>
                      <li>
                        Use a bureau walkthrough or export a letter, then track
                        the actual receipt and response.
                      </li>
                    </ol>
                    <button onClick={() => setPanel("Reports")}>
                      Import your reports
                    </button>
                  </Card>
                  <Card title="Three-bureau coverage">
                    <div className="row wrap">
                      {BUREAUS.map(b => (
                        <span className="badge" key={b}>
                          {label(b)} ·{" "}
                          {state.reports.some(r => r.bureau === b)
                            ? "Imported"
                            : "Needed"}
                        </span>
                      ))}
                    </div>
                    <p>
                      Reports may not include a credit score. Differences
                      between bureaus are review flags and may reflect reporting
                      dates.
                    </p>
                  </Card>
                  <Card title="Check-ins">
                    {state.filings
                      .filter(f => f.submittedAt && !f.responseReceivedAt)
                      .map(f => (
                        <p key={f.id}>
                          {label(f.portal)} ·{" "}
                          {
                            state.disputes.find(d => d.id === f.disputeId)
                              ?.creditor
                          }
                          : check correspondence{" "}
                          {showDate(f.submittedAt! + 30 * DAY)} and{" "}
                          {showDate(f.submittedAt! + 45 * DAY)}.
                        </p>
                      ))}
                    {!state.filings.some(f => f.submittedAt) && (
                      <p>
                        Recorded submissions will appear here. These are
                        check-in dates, not promised results or universal legal
                        deadlines.
                      </p>
                    )}
                  </Card>
                </>
              )}
              {panel === "Reports" && (
                <>
                  <Card title="Import original PDF reports">
                    <p>
                      PDF text and scanned-page OCR run locally. No original
                      report is uploaded. Limits: 50 MB, 200 text pages, or 30
                      OCR pages per file.
                    </p>
                    <input
                      aria-label="Import credit report PDFs"
                      type="file"
                      accept="application/pdf,.pdf"
                      multiple
                      disabled={busy}
                      onChange={e => {
                        const files = Array.from(e.target.files || []);
                        e.target.value = "";
                        void run(async (commit, valid) => {
                          const parsed: Awaited<ReturnType<typeof parsePdf>>[] =
                            [];
                          for (const file of files) {
                            const result = await parsePdf(file, s => {
                              if (valid()) setProgress(s);
                            });
                            if (!valid()) return;
                            parsed.push(result);
                          }
                          commit(s => {
                            const reports = [
                                ...s.reports,
                                ...parsed.map(p => p.report),
                              ],
                              accounts = [
                                ...s.accounts,
                                ...parsed.flatMap(p => p.accounts),
                              ];
                            return {
                              ...s,
                              reports,
                              accounts,
                              findings: analyze(accounts, reports),
                            };
                          });
                        });
                      }}
                    />
                  </Card>
                  {state.reports.map(r => (
                    <Card key={r.id} title={r.fileName}>
                      <p>
                        {label(r.bureau)} · {r.accountCount} accounts ·{" "}
                        {r.score ?? "No score shown"} · {r.extractionMethod}
                      </p>
                      {r.bureau === "unknown" && (
                        <p className="warning">
                          The bureau was not detected. Select it below and
                          verify the original.
                        </p>
                      )}
                      <Field title="Reporting bureau">
                        <select
                          value={r.bureau}
                          onChange={e =>
                            update(s => {
                              const bureau = e.target.value as Bureau,
                                old = s.accounts.filter(
                                  a => a.bureau === r.bureau,
                                );
                              if (
                                s.reports.filter(x => x.bureau === r.bureau)
                                  .length > 1
                              ) {
                                setMessage(
                                  "Remove this report and reimport with the bureau in the filename to avoid changing other reports.",
                                );
                                return s;
                              }
                              const reports = s.reports.map(x =>
                                  x.id === r.id ? { ...x, bureau } : x,
                                ),
                                accounts = s.accounts.map(a =>
                                  old.includes(a) ? { ...a, bureau } : a,
                                );
                              return {
                                ...s,
                                reports,
                                accounts,
                                findings: analyze(accounts, reports),
                              };
                            })
                          }
                        >
                          <option value="unknown">Unknown</option>
                          {BUREAUS.map(b => (
                            <option key={b} value={b}>
                              {label(b)}
                            </option>
                          ))}
                        </select>
                      </Field>
                    </Card>
                  ))}
                  <Card title="Verify extracted accounts">
                    <p>
                      Edit any extraction errors against your original report
                      before drafting. Only the final four account characters
                      are retained.
                    </p>
                    <div className="table-wrap">
                      <table>
                        <thead>
                          <tr>
                            <th>Creditor</th>
                            <th>Bureau / suffix</th>
                            <th>Balance</th>
                            <th>Status</th>
                          </tr>
                        </thead>
                        <tbody>
                          {state.accounts.map(a => (
                            <tr key={a.id}>
                              <td>
                                <input
                                  aria-label="Creditor"
                                  maxLength={64}
                                  value={a.creditor}
                                  onChange={e =>
                                    update(s => {
                                      const accounts = s.accounts.map(x =>
                                        x.id === a.id
                                          ? { ...x, creditor: e.target.value }
                                          : x,
                                      );
                                      return {
                                        ...s,
                                        accounts,
                                        findings: analyze(accounts, s.reports),
                                      };
                                    })
                                  }
                                />
                              </td>
                              <td>
                                {label(a.bureau)} · ••••{a.accountSuffix}
                              </td>
                              <td>
                                <input
                                  aria-label="Balance"
                                  type="number"
                                  min={0}
                                  value={a.balance ?? ""}
                                  onChange={e =>
                                    update(s => {
                                      const accounts = s.accounts.map(x =>
                                        x.id === a.id
                                          ? {
                                              ...x,
                                              balance:
                                                e.target.value === ""
                                                  ? null
                                                  : Number(e.target.value),
                                            }
                                          : x,
                                      );
                                      return {
                                        ...s,
                                        accounts,
                                        findings: analyze(accounts, s.reports),
                                      };
                                    })
                                  }
                                />
                              </td>
                              <td>
                                <input
                                  aria-label="Account status"
                                  maxLength={80}
                                  value={a.status}
                                  onChange={e =>
                                    update(s => {
                                      const accounts = s.accounts.map(x =>
                                        x.id === a.id
                                          ? { ...x, status: e.target.value }
                                          : x,
                                      );
                                      return {
                                        ...s,
                                        accounts,
                                        findings: analyze(accounts, s.reports),
                                      };
                                    })
                                  }
                                />
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                    {!state.accounts.length && (
                      <p>No extracted accounts yet.</p>
                    )}
                  </Card>
                </>
              )}
              {panel === "Analysis" && (
                <>
                  {!state.findings.length && (
                    <Card title="No comparison flags">
                      <p>
                        Import original bureau reports and verify extraction. An
                        empty result does not establish that every reported
                        field is accurate.
                      </p>
                    </Card>
                  )}
                  {state.findings.map(f => (
                    <Card key={f.id} title={f.title}>
                      <span className="badge">
                        {f.severity} · {f.creditor}
                      </span>
                      <p>{f.detail}</p>
                      <p className="muted">{f.legalBasis.join(" · ")}</p>
                      <button
                        onClick={() => {
                          const reason = window.prompt(
                            "Describe the specific factual issue you verified against your records.",
                          );
                          if (!reason?.trim()) return;
                          update(s => ({
                            ...s,
                            disputes: [
                              ...s.disputes,
                              {
                                id: id(),
                                findingId: f.id,
                                creditor: f.creditor,
                                bureaus: f.bureaus,
                                reason: reason.trim(),
                                letter: "",
                                status: "DRAFT",
                                createdAt: Date.now(),
                              },
                            ],
                          }));
                          setPanel("Disputes");
                        }}
                      >
                        Prepare supported draft
                      </button>
                    </Card>
                  ))}
                </>
              )}
              {panel === "Disputes" && (
                <>
                  {!state.disputes.length && (
                    <Card title="No dispute drafts yet">
                      <p>
                        Review your analysis, verify a specific issue, then
                        prepare a draft.
                      </p>
                      <button onClick={() => setPanel("Analysis")}>
                        Review analysis
                      </button>
                    </Card>
                  )}
                  {state.disputes.map(dispute => (
                    <Card key={dispute.id} title={dispute.creditor}>
                      <Field title="Verified factual explanation">
                        <textarea
                          value={dispute.reason}
                          maxLength={10000}
                          onChange={e =>
                            update(s => ({
                              ...s,
                              disputes: s.disputes.map(x =>
                                x.id === dispute.id
                                  ? { ...x, reason: e.target.value }
                                  : x,
                              ),
                              filings: s.filings.map(f =>
                                f.disputeId === dispute.id
                                  ? { ...f, reviewed: false }
                                  : f,
                              ),
                            }))
                          }
                        />
                      </Field>
                      <div className="row wrap">
                        {dispute.bureaus
                          .filter(b => b !== "unknown")
                          .map(b => (
                            <button
                              key={b}
                              className="secondary"
                              onClick={() => chooseFiling(dispute, b)}
                            >
                              {label(b)} walkthrough
                            </button>
                          ))}
                        <button
                          className="secondary"
                          disabled={
                            !cfpbReady(dispute, state.filings, state.mailings)
                          }
                          onClick={() => chooseFiling(dispute, "cfpb")}
                        >
                          CFPB walkthrough
                        </button>
                        <button
                          className="secondary"
                          onClick={() => {
                            setSelectedDispute(dispute.id);
                            setLetterBureau(
                              dispute.bureaus.find(b => b !== "unknown") ||
                                "equifax",
                            );
                            setLetterFacts(dispute.reason);
                            setPanel("Guided filing");
                          }}
                        >
                          Prepare physical letter
                        </button>
                      </div>
                      <p className="muted">
                        {dispute.status} · sent {showDate(dispute.sentAt)}. CFPB
                        preparation unlocks after an actual response or the
                        recorded follow-up window.
                      </p>
                      <Field title="Case status">
                        <select
                          value={dispute.status}
                          onChange={e =>
                            update(s => ({
                              ...s,
                              disputes: s.disputes.map(x =>
                                x.id === dispute.id
                                  ? {
                                      ...x,
                                      status: e.target
                                        .value as Dispute["status"],
                                    }
                                  : x,
                              ),
                            }))
                          }
                        >
                          {[
                            "DRAFT",
                            "READY",
                            "SENT",
                            "INVESTIGATING",
                            "RESOLVED",
                          ].map(x => (
                            <option key={x}>{x}</option>
                          ))}
                        </select>
                      </Field>
                    </Card>
                  ))}
                </>
              )}
              {panel === "Guided filing" && (
                <>
                  <Card title="Select a saved dispute">
                    <select
                      aria-label="Saved dispute"
                      value={selectedDispute}
                      onChange={e => {
                        setSelectedDispute(e.target.value);
                        setAnswer("");
                      }}
                    >
                      <option value="">Choose a dispute</option>
                      {state.disputes.map(d => (
                        <option key={d.id} value={d.id}>
                          {d.creditor}
                        </option>
                      ))}
                    </select>
                    {d && (
                      <div className="row wrap">
                        {d.bureaus
                          .filter(b => b !== "unknown")
                          .map(b => (
                            <button
                              className="secondary"
                              key={b}
                              onClick={() => chooseFiling(d, b)}
                            >
                              {label(b)}
                            </button>
                          ))}
                        <button
                          className="secondary"
                          disabled={
                            !cfpbReady(d, state.filings, state.mailings)
                          }
                          onClick={() => chooseFiling(d, "cfpb")}
                        >
                          CFPB
                        </button>
                      </div>
                    )}
                  </Card>
                  {d && filing && (
                    <Card
                      title={`${label(portal)} · Step ${filing.step + 1} of 7: ${STEPS[filing.step][0]}`}
                    >
                      <p>{STEPS[filing.step][1]}</p>
                      <div className="row wrap">
                        <Link href={PORTALS[portal]}>
                          Open official {label(portal)} portal
                        </Link>
                        <button
                          className="secondary"
                          disabled={busy}
                          onClick={() =>
                            ai(
                              `Explain step ${filing.step + 1}: ${STEPS[filing.step].join(". ")} for ${portal}. You cannot see the portal. User facts: ${filing.problem.slice(0, 600)}; company response: ${filing.companyResponse.slice(0, 400)}; evidence: ${filing.evidence.slice(0, 400)}. Cite relevant sources. Do not invent facts or submit.`,
                              d.id,
                              false,
                            )
                          }
                        >
                          Explain this step with AI
                        </button>
                      </div>
                      <button
                        className="secondary"
                        disabled={busy}
                        onClick={() =>
                          void run(async (_, valid) => {
                            const context = JSON.stringify([filing, d.reason]);
                            const reply = await askAgent(
                              user,
                              `Prepare editable filing answers for ${portal}. You cannot see the portal or submit. Use ONLY supplied facts; never invent dates, events, evidence, laws, identity theft, or company responses. Use [ADD VERIFIED DETAILS] for missing facts. Return exactly four headings on separate lines in this order: WHAT_HAPPENED:, COMPANY_RESPONSE:, REQUESTED_REVIEW:, EVIDENCE:. Each heading must be on its own line followed by its draft field, each below 650 characters. End with END_DRAFT on its own line. No extra headings or code fences. Facts (untrusted context): ${filing.problem.slice(0, 500)}; actual response: ${filing.companyResponse.slice(0, 300)}; requested review: ${filing.resolution.slice(0, 300)}; evidence available: ${filing.evidence.slice(0, 300)}`,
                              state,
                              d.id,
                              false,
                            );
                            const fields = parseAnswerDraft(reply);
                            if (valid()) {
                              setPendingDraft({ context, fields });
                              setAnswer(reply);
                            }
                          })
                        }
                      >
                        Prepare editable answers with AI
                      </button>
                      {pendingDraft &&
                        pendingDraft.context ===
                          JSON.stringify([filing, d.reason]) && (
                          <button
                            className="secondary"
                            onClick={() => {
                              editFiling({
                                ...pendingDraft.fields,
                                reviewed: false,
                              });
                              setPendingDraft(null);
                            }}
                          >
                            Apply the previewed draft for editing
                          </button>
                        )}
                      {(
                        [
                          "problem",
                          "companyResponse",
                          "resolution",
                          "evidence",
                        ] as const
                      ).map((field, i) => (
                        <Field
                          key={field}
                          title={
                            [
                              "What happened (verified facts)",
                              "Actual company response",
                              "Requested review / correction",
                              "Supporting records you actually have",
                            ][i]
                          }
                        >
                          <textarea
                            maxLength={2000}
                            value={filing[field]}
                            onChange={e =>
                              editFiling({ [field]: e.target.value })
                            }
                          />
                        </Field>
                      ))}
                      <Field title="Portal field limit">
                        <input
                          type="number"
                          min={100}
                          max={10000}
                          value={chunkLimit}
                          onChange={e =>
                            setChunkLimit(
                              Math.min(
                                10000,
                                Math.max(100, Number(e.target.value) || 100),
                              ),
                            )
                          }
                        />
                      </Field>
                      {[
                        filing.problem,
                        filing.companyResponse,
                        filing.resolution,
                        filing.evidence,
                      ].flatMap((text, index) =>
                        chunks(text, chunkLimit).map((chunk, n) => (
                          <div className="copy-chunk" key={`${index}:${n}`}>
                            <span>
                              {
                                [
                                  "What happened",
                                  "Company response",
                                  "Requested review",
                                  "Evidence",
                                ][index]
                              }{" "}
                              {n + 1}
                            </span>
                            <p>{chunk}</p>
                            <button
                              className="secondary"
                              onClick={() =>
                                void navigator.clipboard
                                  .writeText(chunk)
                                  .catch(e => setMessage(err(e)))
                              }
                            >
                              Copy
                            </button>
                          </div>
                        )),
                      )}
                      <label className="check">
                        <input
                          type="checkbox"
                          checked={filing.reviewed}
                          disabled={!canReview(filing)}
                          onChange={e =>
                            editFiling({ reviewed: e.target.checked })
                          }
                        />
                        I verified every fact, evidence reference and requested
                        correction against my original records.
                      </label>
                      <div className="row">
                        <button
                          className="secondary"
                          disabled={!filing.step}
                          onClick={() =>
                            editFiling({
                              step: filing.step - 1,
                              reviewed: filing.reviewed,
                            })
                          }
                        >
                          Previous
                        </button>
                        <button
                          disabled={filing.step >= 6}
                          onClick={() =>
                            editFiling({
                              step: filing.step + 1,
                              reviewed: filing.reviewed,
                            })
                          }
                        >
                          Next step
                        </button>
                      </div>
                      {filing.step === 6 && (
                        <>
                          <Field title="Actual confirmation / receipt number">
                            <input
                              maxLength={120}
                              value={filing.confirmation}
                              onChange={e =>
                                editFiling({
                                  confirmation: e.target.value,
                                  reviewed: filing.reviewed,
                                })
                              }
                            />
                          </Field>
                          <button
                            disabled={
                              busy ||
                              !!filing.submittedAt ||
                              !filing.reviewed ||
                              !filing.confirmation.trim()
                            }
                            onClick={() =>
                              void run(async commit => {
                                const saved = submitReceipt(filing, Date.now());
                                commit(s => ({
                                  ...s,
                                  filings: s.filings.map(f =>
                                    f.id === saved.id ? saved : f,
                                  ),
                                  disputes: s.disputes.map(x =>
                                    x.id === d.id && portal !== "cfpb"
                                      ? {
                                          ...x,
                                          status: "SENT",
                                          sentAt: x.sentAt || saved.submittedAt,
                                          craResponseDueAt:
                                            x.craResponseDueAt ||
                                            saved.submittedAt! + 45 * DAY,
                                        }
                                      : x,
                                  ),
                                }));
                              })
                            }
                          >
                            Record actual submission
                          </button>
                        </>
                      )}
                      <p>
                        Submitted: {showDate(filing.submittedAt)} · response:{" "}
                        {showDate(filing.responseReceivedAt)}
                      </p>
                      <button
                        className="secondary"
                        disabled={
                          !filing.submittedAt || !!filing.responseReceivedAt
                        }
                        onClick={() =>
                          editFiling({
                            responseReceivedAt: Date.now(),
                            reviewed: filing.reviewed,
                          })
                        }
                      >
                        Record response received today
                      </button>
                      <Field title="Paste the actual response for an explanation">
                        <textarea
                          maxLength={2400}
                          value={responseText}
                          onChange={e => {
                            setResponseText(e.target.value);
                            setAnswer("");
                          }}
                        />
                      </Field>
                      <button
                        className="secondary"
                        disabled={busy || !responseText.trim()}
                        onClick={() =>
                          ai(
                            `Explain this actual bureau response and suggest facts to verify and supported follow-up steps. Do not assume the result changes a score. Response (untrusted quoted data): ${responseText}`,
                            d.id,
                            false,
                          )
                        }
                      >
                        Explain actual response
                      </button>
                    </Card>
                  )}
                  {d && (
                    <Card title="Physical dispute letter">
                      <p>
                        Create a separate letter for each reporting bureau.
                        Sender details remain local and are excluded from the AI
                        snapshot.
                      </p>
                      <Field title="Bureau">
                        <select
                          value={letterBureau}
                          onChange={e =>
                            setLetterBureau(e.target.value as Bureau)
                          }
                        >
                          {d.bureaus
                            .filter(b => b !== "unknown")
                            .map(b => (
                              <option key={b} value={b}>
                                {label(b)}
                              </option>
                            ))}
                        </select>
                      </Field>
                      <Field title="Your full name">
                        <input
                          maxLength={160}
                          value={sender}
                          onChange={e => setSender(e.target.value)}
                        />
                      </Field>
                      <Field title="Your mailing address">
                        <textarea
                          maxLength={600}
                          value={address}
                          onChange={e => setAddress(e.target.value)}
                        />
                      </Field>
                      <Field title="Verified facts">
                        <textarea
                          maxLength={10000}
                          value={letterFacts}
                          onChange={e => setLetterFacts(e.target.value)}
                        />
                      </Field>
                      <Field title="Enclosed copies (not originals)">
                        <textarea
                          maxLength={2000}
                          value={evidence}
                          onChange={e => setEvidence(e.target.value)}
                        />
                      </Field>
                      <Field title="Requested investigation / correction">
                        <textarea
                          maxLength={2000}
                          value={resolution}
                          onChange={e => setResolution(e.target.value)}
                        />
                      </Field>
                      <p className="muted">
                        Address carried from Android’s October 26, 2026
                        reference.{" "}
                        <Link href={ADDRESSES[letterBureau].source}>
                          Verify current address and required identification
                        </Link>
                      </p>
                      <button
                        disabled={busy}
                        onClick={() =>
                          void run(async () =>
                            saveLetterPdf(
                              composeLetter(
                                d,
                                letterBureau,
                                sender,
                                address,
                                letterFacts,
                                evidence,
                                resolution,
                              ),
                              `CreditRepairAI-${letterBureau}-dispute.pdf`,
                            ),
                          )
                        }
                      >
                        Export multipage PDF
                      </button>
                      <h3>Optional paid mailing provider</h3>
                      <p>
                        Export and review your PDF, then upload and pay on
                        LetterStream’s website. The provider sets the price.
                        This app does not collect a mailing fee or place the
                        order.
                      </p>
                      <Link href="https://www.letterstream.com/">
                        Open LetterStream
                      </Link>
                      <MailForm
                        onSave={record =>
                          update(s => ({
                            ...s,
                            mailings: [
                              ...s.mailings,
                              {
                                ...record,
                                disputeId: d.id,
                                bureau: letterBureau,
                              },
                            ],
                          }))
                        }
                      />
                      {state.mailings
                        .filter(m => m.disputeId === d.id)
                        .map((m, i) => (
                          <div className="tracking" key={i}>
                            <p>
                              {label(m.bureau)} · mailed {showDate(m.mailedAt)}{" "}
                              · {m.trackingNumber}
                              <br />
                              Delivered {showDate(m.deliveredAt)} · response{" "}
                              {showDate(m.responseReceivedAt)}
                            </p>
                            <button
                              className="secondary"
                              disabled={!!m.deliveredAt}
                              onClick={() =>
                                update(s => ({
                                  ...s,
                                  mailings: s.mailings.map(x =>
                                    x === m
                                      ? { ...x, deliveredAt: Date.now() }
                                      : x,
                                  ),
                                }))
                              }
                            >
                              Record delivery today
                            </button>
                            <button
                              className="secondary"
                              disabled={!!m.responseReceivedAt}
                              onClick={() =>
                                update(s => ({
                                  ...s,
                                  mailings: s.mailings.map(x =>
                                    x === m
                                      ? { ...x, responseReceivedAt: Date.now() }
                                      : x,
                                  ),
                                }))
                              }
                            >
                              Record response today
                            </button>
                          </div>
                        ))}
                    </Card>
                  )}
                </>
              )}
              {panel === "AI guide" && (
                <Card title="Ask your case guide">
                  <p>
                    Ask about a finding, evidence, a next step, your goals or an
                    actual bureau response. Answers use the shared authenticated
                    Android legal-source service.
                  </p>
                  <Field title="Case scope">
                    <select
                      value={caseId}
                      onChange={e => {
                        setCaseId(e.target.value);
                        setAnswer("");
                      }}
                    >
                      <option value="">Whole case</option>
                      {state.disputes.map(d => (
                        <option key={d.id} value={d.id}>
                          {d.creditor}
                        </option>
                      ))}
                    </select>
                  </Field>
                  <div className="conversation">
                    {state.chat
                      .filter(m => m.caseScopeId === (caseId || undefined))
                      .map(m => (
                        <article
                          className={
                            m.fromUser ? "user-message" : "guide-message"
                          }
                          key={m.id}
                        >
                          <strong>{m.fromUser ? "You" : "AI guide"}</strong>
                          <p>{m.text}</p>
                        </article>
                      ))}
                  </div>
                  <form
                    onSubmit={e => {
                      e.preventDefault();
                      ai(question);
                      setQuestion("");
                    }}
                  >
                    <Field title="Your question">
                      <textarea
                        required
                        minLength={2}
                        maxLength={1400}
                        value={question}
                        onChange={e => setQuestion(e.target.value)}
                        placeholder="What should I verify before disputing this balance?"
                      />
                    </Field>
                    <button
                      disabled={
                        busy ||
                        !user.emailVerified ||
                        !state.policyAcceptance?.aiSharing
                      }
                    >
                      Ask the guide
                    </button>
                  </form>
                  <div className="row wrap">
                    {[
                      "What should I verify first?",
                      "Help me prepare evidence for a supported dispute.",
                      "Explain my CFPB readiness.",
                    ].map(q => (
                      <button
                        key={q}
                        className="secondary"
                        disabled={busy}
                        onClick={() => ai(q)}
                      >
                        {q}
                      </button>
                    ))}
                  </div>
                </Card>
              )}
              {panel === "Goals & scores" && (
                <>
                  <Card title="Your goal">
                    <Field title="What are you working toward?">
                      <textarea
                        maxLength={1500}
                        value={state.goal.description}
                        onChange={e =>
                          update(s => ({
                            ...s,
                            goal: {
                              ...s.goal,
                              description: e.target.value,
                              aiPlan: "",
                            },
                          }))
                        }
                      />
                    </Field>
                    <Field title="Personal target (months, not a forecast)">
                      <input
                        type="number"
                        min={1}
                        max={120}
                        value={state.goal.targetMonths}
                        onChange={e =>
                          update(s => ({
                            ...s,
                            goal: {
                              ...s.goal,
                              targetMonths: Math.min(
                                120,
                                Math.max(1, Number(e.target.value)),
                              ),
                            },
                          }))
                        }
                      />
                    </Field>
                    <button
                      disabled={busy || !state.goal.description.trim()}
                      onClick={() =>
                        void run(async commit => {
                          const reply = await askAgent(
                            user,
                            `Build a prioritized educational plan for my goal: ${state.goal.description}. My target of ${state.goal.targetMonths} months is a preference, not a forecast. Use actual case facts and identify missing evidence. Do not predict score changes, approval, deletion or completion.`,
                            state,
                          );
                          commit(s => ({
                            ...s,
                            goal: { ...s.goal, aiPlan: reply },
                          }));
                        })
                      }
                    >
                      Create and save an AI plan
                    </button>
                    <p className="answer">{state.goal.aiPlan}</p>
                  </Card>
                  <ScoreForm
                    onSave={score =>
                      update(s => ({ ...s, scores: [...s.scores, score] }))
                    }
                  />
                  <Card title="Manual score history">
                    {state.scores.map((score, i) => (
                      <p key={i}>
                        {label(score.bureau)} · {score.score} ·{" "}
                        {showDate(score.recordedAt)}
                      </p>
                    ))}
                    <p className="muted">
                      Record scores from your source. A score change does not
                      establish that a dispute caused it.
                    </p>
                  </Card>
                  <Card title="Security freeze checklist">
                    {BUREAUS.map(b => (
                      <label key={b} className="check">
                        <input
                          type="checkbox"
                          checked={state.frozenBureaus.includes(b)}
                          onChange={e =>
                            update(s => ({
                              ...s,
                              frozenBureaus: e.target.checked
                                ? [...s.frozenBureaus, b]
                                : s.frozenBureaus.filter(x => x !== b),
                            }))
                          }
                        />
                        I completed my {label(b)} freeze myself.
                      </label>
                    ))}
                    <p>
                      <Link href="https://www.identitytheft.gov/">
                        Official identity theft guidance
                      </Link>
                    </p>
                    <p>
                      These are your records of actions, not confirmation from a
                      bureau.
                    </p>
                  </Card>
                </>
              )}
              {panel === "Account" && (
                <>
                  <Card title="Account & privacy">
                    <p>
                      {user.email} ·{" "}
                      {user.emailVerified
                        ? "Verified"
                        : "Verification required"}
                    </p>
                    <Field title="U.S. jurisdiction (state/DC code, optional)">
                      <input
                        maxLength={2}
                        pattern="[A-Z]{2}"
                        value={state.jurisdictionCode}
                        onChange={e =>
                          update(s => ({
                            ...s,
                            jurisdictionCode: e.target.value
                              .toUpperCase()
                              .replace(/[^A-Z]/g, ""),
                          }))
                        }
                      />
                    </Field>
                    <label className="check">
                      <input
                        type="checkbox"
                        checked={state.policyAcceptance?.aiSharing || false}
                        disabled={
                          busy ||
                          state.policyAcceptance?.version !== POLICY_VERSION ||
                          state.policyAcceptance?.digest !== POLICY_DIGEST
                        }
                        onChange={e => {
                          const aiSharing = e.target.checked;
                          void run(async commit => {
                            await acceptPolicies(user, aiSharing);
                            commit(s => ({
                              ...s,
                              policyAcceptance: {
                                version: POLICY_VERSION,
                                digest: POLICY_DIGEST,
                                aiSharing,
                                acceptedAt: Date.now(),
                              },
                            }));
                          });
                        }}
                      />
                      Allow redacted AI sharing
                    </label>
                    <p>
                      <Link href="/legal/privacy.html">Privacy</Link> ·{" "}
                      <Link href="/legal/terms.html">Terms</Link> ·{" "}
                      <Link href="/legal/delete-account.html">
                        Deletion details
                      </Link>
                    </p>
                  </Card>
                  <Card title="Transfer your encrypted case">
                    <p>
                      Export this vault and import it while signed into the same
                      Firebase account on another supported platform. Only
                      encrypted data leaves the device. This is a manual
                      transfer; it does not silently sync Android’s existing
                      private vault.
                    </p>
                    <button
                      disabled={busy}
                      onClick={() =>
                        void run(async () => {
                          if (!key.current || !salt.current) return;
                          const envelope = await seal(
                            state,
                            key.current,
                            salt.current,
                            user.uid,
                          );
                          download(
                            JSON.stringify(envelope),
                            "CreditRepairAI-encrypted-vault.json",
                          );
                        })
                      }
                    >
                      Export encrypted vault
                    </button>
                    <Field title="Import passphrase">
                      <input
                        type="password"
                        autoComplete="off"
                        minLength={12}
                        value={password}
                        onChange={e => setPassword(e.target.value)}
                      />
                    </Field>
                    <input
                      aria-label="Import encrypted vault"
                      type="file"
                      accept=".json,application/json"
                      disabled={busy || password.length < 12}
                      onChange={e => {
                        const file = e.target.files?.[0];
                        e.target.value = "";
                        if (!file) return;
                        void run(async (commit, valid) => {
                          if (file.size > 28000000)
                            throw Error("Vault exceeds the transfer limit.");
                          const result = await unseal(
                            JSON.parse(await file.text()),
                            password,
                            user.uid,
                          );
                          if (!valid()) return;
                          commit(() => result.state);
                          setPassword("");
                        });
                      }}
                    />
                  </Card>
                  <Card title="Shared subscription access">
                    <p>
                      Existing Android subscription access is checked by the
                      same account identity. New web and Apple checkout remain
                      disabled in this test candidate.
                    </p>
                    <button
                      disabled={busy}
                      onClick={() =>
                        void run(async (_, valid) => {
                          const result = await securePost(
                            user,
                            "/billing/status",
                            {},
                          );
                          if (valid())
                            setBilling(
                              result.active
                                ? "Verified subscription is active."
                                : "No active subscription was returned. Per-dispute access is checked when a scoped AI request is made.",
                            );
                        })
                      }
                    >
                      Check shared access
                    </button>
                    <p>{billing}</p>
                  </Card>
                  <Card title="Delete account">
                    <p>
                      Delete server billing and consent records, then the
                      Firebase login and this browser’s encrypted case.
                      Reauthenticate first if Firebase requires it. Export your
                      vault first if you need a copy.
                    </p>
                    <Field title="Type DELETE to confirm">
                      <input
                        value={deletion}
                        onChange={e => setDeletion(e.target.value)}
                      />
                    </Field>
                    <button
                      className="danger"
                      disabled={busy || deletion !== "DELETE"}
                      onClick={() =>
                        void run(async () => {
                          const uid = user.uid;
                          await deleteAccount(user);
                          localStorage.removeItem(vaultName(uid));
                          signOut();
                        })
                      }
                    >
                      Delete my account
                    </button>
                  </Card>
                </>
              )}
              {answer && (
                <Card title="AI explanation">
                  <p className="answer">{answer}</p>
                </Card>
              )}
              <footer>
                Educational information only — not legal or financial advice.
                Review original records and official sources. You personally
                submit all portal filings and mailing orders.
              </footer>
            </main>
          </div>
        </>
      )}
    </div>
  );
}
function MailForm({
  onSave,
}: {
  onSave: (record: { mailedAt: number; trackingNumber: string }) => void;
}) {
  const [date, setDate] = useState(""),
    [tracking, setTracking] = useState("");
  return (
    <form
      onSubmit={e => {
        e.preventDefault();
        const mailedAt = new Date(date + "T12:00:00").getTime();
        if (!Number.isFinite(mailedAt) || mailedAt > Date.now()) return;
        onSave({ mailedAt, trackingNumber: tracking.trim() });
        setDate("");
        setTracking("");
      }}
    >
      <h3>Record actual mailing</h3>
      <Field title="Date mailed">
        <input
          type="date"
          max={new Date().toLocaleDateString("en-CA")}
          required
          value={date}
          onChange={e => setDate(e.target.value)}
        />
      </Field>
      <Field title="Tracking / provider receipt">
        <input
          required
          maxLength={160}
          value={tracking}
          onChange={e => setTracking(e.target.value)}
        />
      </Field>
      <button className="secondary">Record mailing</button>
    </form>
  );
}
function ScoreForm({
  onSave,
}: {
  onSave: (score: {
    bureau: Bureau;
    score: number;
    recordedAt: number;
  }) => void;
}) {
  const [bureau, setBureau] = useState<Bureau>("equifax"),
    [score, setScore] = useState("");
  return (
    <Card title="Record a dated score">
      <form
        onSubmit={e => {
          e.preventDefault();
          if (+score >= 300 && +score <= 850) {
            onSave({ bureau, score: +score, recordedAt: Date.now() });
            setScore("");
          }
        }}
      >
        <Field title="Bureau">
          <select
            value={bureau}
            onChange={e => setBureau(e.target.value as Bureau)}
          >
            {BUREAUS.map(b => (
              <option key={b} value={b}>
                {label(b)}
              </option>
            ))}
          </select>
        </Field>
        <Field title="Score (300–850)">
          <input
            type="number"
            min={300}
            max={850}
            required
            value={score}
            onChange={e => setScore(e.target.value)}
          />
        </Field>
        <button>Save score</button>
      </form>
    </Card>
  );
}
