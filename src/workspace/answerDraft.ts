export function parseAnswerDraft(answer: string) {
  const complete = answer.trimEnd();
  if (!complete.endsWith("\nEND_DRAFT"))
    throw Error("AI returned an incomplete draft. No facts were changed.");
  const body = complete.slice(0, -10),
    labels = [
      "WHAT_HAPPENED",
      "COMPANY_RESPONSE",
      "REQUESTED_REVIEW",
      "EVIDENCE",
    ];
  const matches = [
    ...body.matchAll(
      /^(WHAT_HAPPENED|COMPANY_RESPONSE|REQUESTED_REVIEW|EVIDENCE):[ \t]*$/gm,
    ),
  ];
  if (matches.length !== 4 || matches.some((m, i) => m[1] !== labels[i]))
    throw Error("Draft headings were incomplete. No facts were changed.");
  const values = matches.map((m, i) =>
    body
      .slice(m.index! + m[0].length, matches[i + 1]?.index ?? body.length)
      .trim(),
  );
  if (values.some(v => !v || v.length > 2000))
    throw Error("Draft fields were empty or too long.");
  return {
    problem: values[0],
    companyResponse: values[1],
    resolution: values[2],
    evidence: values[3],
  };
}
