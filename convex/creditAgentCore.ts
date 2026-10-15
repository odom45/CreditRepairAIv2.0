type JsonObject = Record<string, unknown>;
import { educationalAnswer, sanitizeSnapshot } from "./agentSafety";
import { CREDIT_AGENT_CONFIG } from "./creditAgentConfig";

export type CreditAgentRequest = {
  clientRequestId: string;
  question: string;
  caseScopeId?: string;
  caseSnapshot: JsonObject;
};

type ResponseOutput = {
  type?: string;
  status?: string;
  results?: Array<{ file_id?: string; text?: string }>;
  name?: string;
  arguments?: string;
  call_id?: string;
  content?: Array<{ type?: string; text?: string }>;
};

type OpenAIResponse = {
  id?: string;
  status?: string;
  output_text?: string;
  output?: ResponseOutput[];
};

const MAX_TOOL_ROUNDS = CREDIT_AGENT_CONFIG.maxToolRounds;
const US_JURISDICTION_CODES = new Set([
  "AL", "AK", "AZ", "AR", "CA", "CO", "CT", "DE", "DC", "FL", "GA", "HI", "ID",
  "IL", "IN", "IA", "KS", "KY", "LA", "ME", "MD", "MA", "MI", "MN", "MS", "MO",
  "MT", "NE", "NV", "NH", "NJ", "NM", "NY", "NC", "ND", "OH", "OK", "OR", "PA",
  "RI", "SC", "SD", "TN", "TX", "UT", "VT", "VA", "WA", "WV", "WI", "WY",
]);
const FEDERAL_CIRCUIT_BY_JURISDICTION: Record<string, string> = {
  ME: "FIRST_CIRCUIT", MA: "FIRST_CIRCUIT", NH: "FIRST_CIRCUIT", RI: "FIRST_CIRCUIT",
  CT: "SECOND_CIRCUIT", NY: "SECOND_CIRCUIT", VT: "SECOND_CIRCUIT",
  DE: "THIRD_CIRCUIT", NJ: "THIRD_CIRCUIT", PA: "THIRD_CIRCUIT",
  MD: "FOURTH_CIRCUIT", NC: "FOURTH_CIRCUIT", SC: "FOURTH_CIRCUIT", VA: "FOURTH_CIRCUIT", WV: "FOURTH_CIRCUIT",
  LA: "FIFTH_CIRCUIT", MS: "FIFTH_CIRCUIT", TX: "FIFTH_CIRCUIT",
  KY: "SIXTH_CIRCUIT", MI: "SIXTH_CIRCUIT", OH: "SIXTH_CIRCUIT", TN: "SIXTH_CIRCUIT",
  IL: "SEVENTH_CIRCUIT", IN: "SEVENTH_CIRCUIT", WI: "SEVENTH_CIRCUIT",
  AR: "EIGHTH_CIRCUIT", IA: "EIGHTH_CIRCUIT", MN: "EIGHTH_CIRCUIT", MO: "EIGHTH_CIRCUIT",
  NE: "EIGHTH_CIRCUIT", ND: "EIGHTH_CIRCUIT", SD: "EIGHTH_CIRCUIT",
  AK: "NINTH_CIRCUIT", AZ: "NINTH_CIRCUIT", CA: "NINTH_CIRCUIT", HI: "NINTH_CIRCUIT",
  ID: "NINTH_CIRCUIT", MT: "NINTH_CIRCUIT", NV: "NINTH_CIRCUIT", OR: "NINTH_CIRCUIT", WA: "NINTH_CIRCUIT",
  CO: "TENTH_CIRCUIT", KS: "TENTH_CIRCUIT", NM: "TENTH_CIRCUIT", OK: "TENTH_CIRCUIT",
  UT: "TENTH_CIRCUIT", WY: "TENTH_CIRCUIT",
  AL: "ELEVENTH_CIRCUIT", FL: "ELEVENTH_CIRCUIT", GA: "ELEVENTH_CIRCUIT",
  DC: "DC_CIRCUIT",
};

const instructions = CREDIT_AGENT_CONFIG.systemPrompt;

const noArgumentTool = (name: string, description: string) => ({
  type: "function",
  name,
  description,
  strict: true,
  parameters: {
    type: "object",
    properties: {},
    required: [],
    additionalProperties: false,
  },
});

export async function runCreditAgent(request: CreditAgentRequest): Promise<string> {
  const deadline = Date.now() + CREDIT_AGENT_CONFIG.requestDeadlineMs;
  validateRequest(request);
  request = { ...request, caseSnapshot: sanitizeSnapshot(request.caseSnapshot) };
  const apiKey = process.env.OPENAI_API_KEY;
  const vectorStoreId = process.env.CREDIT_LEGAL_VECTOR_STORE_ID;
  if (!apiKey || !vectorStoreId) {
    throw new PublicAgentError("The legal AI service is not fully configured.", 503);
  }

  const jurisdictionCode = normalizeJurisdiction(asObject(request.caseSnapshot).jurisdictionCode);
  const jurisdictionFilter = jurisdictionCode
    ? { type: "in", key: "jurisdiction", value: ["US", jurisdictionCode] }
    : { type: "eq", key: "jurisdiction", value: "US" };
  const circuit = jurisdictionCode ? FEDERAL_CIRCUIT_BY_JURISDICTION[jurisdictionCode] : null;
  const precedentScopes = ["NON_CASE", "US_SUPREME_COURT", ...(circuit ? [circuit] : [])];
  const retrievalFilter = {
    type: "and",
    filters: [
      jurisdictionFilter,
      { type: "in", key: "court_scope", value: precedentScopes },
    ],
  };

  const tools = [
    {
      type: "file_search",
      vector_store_ids: [vectorStoreId],
      max_num_results: 8,
      filters: retrievalFilter,
    },
    noArgumentTool("get_case_summary", "Return the redacted reports, tradelines, scores, and analysis flags for this case."),
    noArgumentTool("get_dispute_history", "Return redacted dispute statuses and timing for this case."),
    noArgumentTool("get_cfpb_readiness", "Check whether the recorded CFPB complaint prerequisites are met."),
    noArgumentTool("get_jurisdiction_coverage", "Return whether the selected state has verified official source text or registry-only coverage."),
    noArgumentTool("get_precedent_scope", "Return the Supreme Court and federal circuit precedent scopes allowed for the selected state."),
    noArgumentTool("build_follow_up_plan", "Return a deterministic now, 7-day, 30-to-45-day, and ongoing action framework."),
  ];

  const history: unknown[] = [{
      role: "user", content: [{ type: "input_text", text: `Question: ${request.question}\nSelected jurisdiction: ${jurisdictionCode || "not provided"}. Use the case tools for individualized context.` }],
  }];
  let searched = false;
  let summarized = false;
  let response = await callResponsesApi(apiKey, {
    model: process.env.CREDIT_AGENT_MODEL || CREDIT_AGENT_CONFIG.defaultModel,
    instructions,
    input: history,
    store: false,
    include: ["reasoning.encrypted_content", "file_search_call.results"],
    tools,
    tool_choice: { type: "function", name: "get_case_summary" },
    max_output_tokens: CREDIT_AGENT_CONFIG.maxOutputTokens,
  }, deadline);

  for (let round = 0; round < MAX_TOOL_ROUNDS; round += 1) {
    const calls = (response.output || []).filter((item) => item.type === "function_call");
    summarized ||= calls.some(item => item.name === "get_case_summary" && Boolean(item.call_id));
    for (const search of (response.output || []).filter(item => item.type === "file_search_call")) {
      if (search.status !== "completed" || !search.results?.some(result => result.file_id && result.text?.trim())) {
        throw new PublicAgentError("The legal library did not return usable sources. Try again later; verify any legal question with a qualified professional.", 503);
      }
      searched = true;
    }
    if (calls.length === 0 && summarized && searched) return educationalAnswer(extractAnswer(response));

    const outputs = calls.map((call) => ({
      type: "function_call_output",
      call_id: call.call_id,
      output: JSON.stringify(executeCaseTool(call.name || "", request.caseSnapshot)),
    }));
    history.push(...(response.output || []), ...outputs);
    response = await callResponsesApi(apiKey, {
      model: process.env.CREDIT_AGENT_MODEL || CREDIT_AGENT_CONFIG.defaultModel,
      instructions,
      input: history,
      store: false,
      include: ["reasoning.encrypted_content", "file_search_call.results"],
      tools,
      tool_choice: !summarized ? { type: "function", name: "get_case_summary" } : searched ? "auto" : { type: "file_search" },
      max_output_tokens: CREDIT_AGENT_CONFIG.maxOutputTokens,
    }, deadline);
  }

  throw new PublicAgentError("The AI service could not complete its case-tool checks.", 502);
}

function executeCaseTool(name: string, snapshot: JsonObject): unknown {
  const reports = safeArray(snapshot.reports);
  const accounts = safeArray(snapshot.accounts);
  const findings = safeArray(snapshot.findings);
  const disputes = safeArray(snapshot.disputes);
  const jurisdictionCode = normalizeJurisdiction(snapshot.jurisdictionCode);
  switch (name) {
    case "get_case_summary":
      return { reports, accounts, findings };
    case "get_dispute_history":
      return { disputes };
    case "get_cfpb_readiness":
      return disputes.map((raw) => {
        const dispute = asObject(raw);
        const status = String(dispute.status || "DRAFT");
        const due = typeof dispute.craResponseDueAt === "number" ? dispute.craResponseDueAt : null;
        const ready = typeof dispute.sentAt === "number" && (dispute.responseReceived === true || (due !== null && due <= Date.now()));
        return {
          id: dispute.id,
          creditor: dispute.creditor,
          status,
          ready,
          reason: ready ? "Response recorded or 45-day date reached." : "First dispute remains unsent or pending within the recorded window.",
        };
      });
    case "get_jurisdiction_coverage": {
      const verifiedCodes = new Set(
        (process.env.CREDIT_VERIFIED_STATE_CODES || "")
          .split(",")
          .map((value) => value.trim().toUpperCase())
          .filter((value) => /^[A-Z]{2}$/.test(value)),
      );
      return {
        jurisdictionCode,
        federalCoverage: "SOURCE_TEXT_VERIFIED",
        stateCoverage: jurisdictionCode && verifiedCodes.has(jurisdictionCode)
          ? "SOURCE_TEXT_VERIFIED"
          : jurisdictionCode ? "SOURCE_REGISTRY_ONLY" : "NOT_SELECTED",
        instruction: jurisdictionCode && verifiedCodes.has(jurisdictionCode)
          ? "Retrieve and cite the exact official state provision before making a state-law claim."
          : "Use verified federal sources and disclose that state-specific provision coverage is not yet verified.",
      };
    }
    case "get_precedent_scope": {
      const circuit = jurisdictionCode ? FEDERAL_CIRCUIT_BY_JURISDICTION[jurisdictionCode] : null;
      return {
        jurisdictionCode,
        nationwideScope: "US_SUPREME_COURT",
        selectedCircuitScope: circuit,
        treatmentStatus: "ATTORNEY_REVIEW_PENDING",
        permittedUse: circuit
          ? "Explain official Supreme Court holdings and published holdings from the selected federal circuit for educational case planning."
          : "Explain official Supreme Court holdings only until a state is selected.",
        prohibitedUse: "Do not predict claims, liability, damages, limitation periods, venue, or litigation outcomes. Do not represent this as a formal citator result.",
      };
    }
    case "build_follow_up_plan":
      return {
        now: "Verify each flagged fact against the original reports and gather supporting records.",
        next7Days: "Send only supported disputes, keep copies, and retain delivery confirmation.",
        days30To45: "Track responses and compare every result with all three current reports; escalate only unresolved, supported issues.",
        ongoing: "Pay on time, manage revolving utilization, monitor reports, and record score changes without promising causation.",
        findingCount: findings.length,
        disputeCount: disputes.length,
      };
    default:
      return { error: "Unknown case tool" };
  }
}

async function callResponsesApi(apiKey: string, body: JsonObject, deadline: number): Promise<OpenAIResponse> {
  const remaining = deadline - Date.now();
  if (remaining <= 0) throw new PublicAgentError("The AI request timed out. Please try again.", 504);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), remaining);
  try {
    const response = await fetch("https://api.openai.com/v1/responses", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
        "X-Client-Request-Id": crypto.randomUUID(),
      },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!response.ok) throw new PublicAgentError("The AI service is temporarily unavailable.", 502);
    const result = (await response.json()) as OpenAIResponse;
    if (result.status !== "completed") throw new PublicAgentError("The AI service did not complete its response. Please try again.", 502);
    return result;
  } catch (error) {
    if (error instanceof PublicAgentError) throw error;
    throw new PublicAgentError("The AI service is temporarily unavailable or timed out.", 502);
  } finally {
    clearTimeout(timer);
  }
}

function extractAnswer(response: OpenAIResponse): string {
  if (response.output_text?.trim()) return response.output_text.trim();
  const text = (response.output || [])
    .flatMap((item) => item.content || [])
    .filter((item) => item.type === "output_text" && item.text)
    .map((item) => item.text)
    .join("\n")
    .trim();
  if (!text) throw new PublicAgentError("The AI service returned an empty answer.", 502);
  return text;
}

function validateRequest(request: CreditAgentRequest) {
  if (!request || typeof request !== "object") throw new PublicAgentError("Invalid request.", 400);
  if (!/^[0-9a-f-]{36}$/i.test(request.clientRequestId || "")) throw new PublicAgentError("Invalid request ID.", 400);
  if (typeof request.question !== "string" || request.question.trim().length < 2 || request.question.length > 4000) {
    throw new PublicAgentError("Question must be between 2 and 4,000 characters.", 400);
  }
  if (containsHighRiskIdentifier(request.question)) {
    throw new PublicAgentError("Remove sensitive identifiers before asking the agent.", 400);
  }
  const snapshot = asObject(request.caseSnapshot);
  if (typeof snapshot.jurisdictionCode === "string" && snapshot.jurisdictionCode.trim() && !normalizeJurisdiction(snapshot.jurisdictionCode)) {
    throw new PublicAgentError("Jurisdiction must be a valid U.S. state or DC code.", 400);
  }
  if (safeArray(snapshot.accounts).length > 200 || safeArray(snapshot.findings).length > 100 || safeArray(snapshot.disputes).length > 100) {
    throw new PublicAgentError("Case snapshot exceeds the allowed size.", 400);
  }
  for (const raw of safeArray(snapshot.accounts)) {
    const suffix = String(asObject(raw).accountSuffix || "");
    if (suffix.length > 4) throw new PublicAgentError("Full account numbers are not accepted.", 400);
  }
}

function containsHighRiskIdentifier(value: string): boolean {
  return /\b\d{3}[- ]?\d{2}[- ]?\d{4}\b/.test(value) ||
    /\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}\b/i.test(value) ||
    /\b(?:dob|date of birth)\s*[:#-]?\s*(?:\d{1,2}[/-]){2}\d{2,4}\b/i.test(value) ||
    /(?<!\d)\d{9,19}(?!\d)/.test(value);
}

function asObject(value: unknown): JsonObject {
  return value && typeof value === "object" && !Array.isArray(value) ? value as JsonObject : {};
}

function safeArray(value: unknown): unknown[] {
  return Array.isArray(value) ? value : [];
}

function normalizeJurisdiction(value: unknown): string | null {
  const code = typeof value === "string" ? value.trim().toUpperCase() : "";
  return US_JURISDICTION_CODES.has(code) ? code : null;
}

export class PublicAgentError extends Error {
  readonly status: number;
  constructor(message: string, status: number) {
    super(message);
    this.status = status;
  }
}
