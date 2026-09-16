type JsonObject = Record<string, unknown>;

export type CreditAgentRequest = {
  clientRequestId: string;
  question: string;
  caseSnapshot: JsonObject;
};

type ResponseOutput = {
  type?: string;
  name?: string;
  arguments?: string;
  call_id?: string;
  content?: Array<{ type?: string; text?: string }>;
};

type OpenAIResponse = {
  id?: string;
  output_text?: string;
  output?: ResponseOutput[];
};

const MAX_TOOL_ROUNDS = 6;

const instructions = `
You are CreditRepairAI's U.S. consumer-credit education and case-planning agent.

Mandatory process:
1. Call get_case_summary before giving case-specific guidance. Call the other case tools when relevant.
2. Use file_search for every legal or regulatory claim. Cite the retrieved official source title, section, and URL near the claim.
3. Separate: (a) facts shown in the case snapshot, (b) possible inconsistencies requiring user verification, (c) governing authority, and (d) recommended next steps.
4. Never promise deletion, a score increase, a filing result, or a timeline. Accurate current negative information generally cannot be removed merely because it is harmful.
5. Never invent facts, dispute accurate information, suggest identity theft without evidence, or use generic "609/UCC/affidavit" tactics.
6. Before suggesting a CFPB credit-reporting complaint, verify that the user first disputed with the reporting company and that the dispute is no longer pending or 45 days have passed.
7. Ask a focused question when evidence or facts are missing. Give a plain-language checklist usable by someone with no legal background.
8. Do not request or repeat an SSN, full account number, date of birth, password, email address, phone number, or street address.
9. State that the answer is educational information, not individualized legal advice, when legal judgment or litigation is implicated.

Prefer concise answers with: What the records show; What to verify; Best next action; Evidence; Timing/follow-up; Sources.
`;

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
  validateRequest(request);
  const apiKey = process.env.OPENAI_API_KEY;
  const vectorStoreId = process.env.CREDIT_LEGAL_VECTOR_STORE_ID;
  if (!apiKey || !vectorStoreId) {
    throw new PublicAgentError("The legal AI service is not fully configured.", 503);
  }

  const tools = [
    {
      type: "file_search",
      vector_store_ids: [vectorStoreId],
      max_num_results: 8,
    },
    noArgumentTool("get_case_summary", "Return the redacted reports, tradelines, scores, and analysis flags for this case."),
    noArgumentTool("get_dispute_history", "Return redacted dispute statuses and timing for this case."),
    noArgumentTool("get_cfpb_readiness", "Check whether the recorded CFPB complaint prerequisites are met."),
    noArgumentTool("build_follow_up_plan", "Return a deterministic now, 7-day, 30-to-45-day, and ongoing action framework."),
  ];

  let response = await callResponsesApi(apiKey, {
    model: process.env.CREDIT_AGENT_MODEL || "gpt-5.6-terra",
    instructions,
    input: [{
      role: "user",
      content: [{
        type: "input_text",
        text: `Question: ${request.question}\nUse the case tools for individualized context. Request ID: ${request.clientRequestId}`,
      }],
    }],
    tools,
    tool_choice: "auto",
    max_output_tokens: 1800,
  });

  for (let round = 0; round < MAX_TOOL_ROUNDS; round += 1) {
    const calls = (response.output || []).filter((item) => item.type === "function_call");
    if (calls.length === 0) return extractAnswer(response);

    const outputs = calls.map((call) => ({
      type: "function_call_output",
      call_id: call.call_id,
      output: JSON.stringify(executeCaseTool(call.name || "", request.caseSnapshot)),
    }));
    response = await callResponsesApi(apiKey, {
      model: process.env.CREDIT_AGENT_MODEL || "gpt-5.6-terra",
      instructions,
      previous_response_id: response.id,
      input: outputs,
      tools,
      max_output_tokens: 1800,
    });
  }

  throw new PublicAgentError("The AI service could not complete its case-tool checks.", 502);
}

function executeCaseTool(name: string, snapshot: JsonObject): unknown {
  const reports = safeArray(snapshot.reports);
  const accounts = safeArray(snapshot.accounts);
  const findings = safeArray(snapshot.findings);
  const disputes = safeArray(snapshot.disputes);
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
        const ready = status === "RESOLVED" || (due !== null && due <= Date.now());
        return {
          id: dispute.id,
          creditor: dispute.creditor,
          status,
          ready,
          reason: ready ? "Response recorded or 45-day date reached." : "First dispute remains unsent or pending within the recorded window.",
        };
      });
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

async function callResponsesApi(apiKey: string, body: JsonObject): Promise<OpenAIResponse> {
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${apiKey}`,
      "Content-Type": "application/json",
      "X-Client-Request-Id": crypto.randomUUID(),
    },
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new PublicAgentError("The AI service is temporarily unavailable.", 502);
  }
  return (await response.json()) as OpenAIResponse;
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

export class PublicAgentError extends Error {
  constructor(message: string, readonly status: number) {
    super(message);
  }
}
