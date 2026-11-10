import type { Bureau, Dispute } from "./model";
export const ADDRESSES: Record<Bureau, { address: string; source: string }> = {
  experian: {
    address: "Experian\nDispute by Mail\nP.O. Box 4500\nAllen, TX 75013",
    source: "https://www.experian.com/help/dispute-credit/",
  },
  equifax: {
    address:
      "Equifax Information Services LLC\nP.O. Box 740256\nAtlanta, GA 30374",
    source: "https://assets.equifax.com/assets/personal/Dispute.pdf",
  },
  transunion: {
    address:
      "TransUnion Consumer Solutions\nP.O. Box 2000\nChester, PA 19016-2000",
    source:
      "https://www.transunion.com/credit-disputes/dispute-your-credit/mail-or-phone",
  },
  unknown: { address: "", source: "" },
};
export function composeLetter(
  d: Dispute,
  bureau: Bureau,
  name: string,
  address: string,
  facts: string,
  evidence: string,
  resolution: string,
  date = new Date().toLocaleDateString(),
): string {
  if (bureau === "unknown" || !d.bureaus.includes(bureau))
    throw Error("Choose a reporting bureau.");
  for (const [v, max] of [
    [name, 160],
    [address, 600],
    [facts, 10000],
    [evidence, 2000],
    [resolution, 2000],
  ] as const)
    if (!v.trim() || v.length > max)
      throw Error("Complete every letter field within the length limits.");
  return `${name.trim()}\n${address.trim()}\n\n${date}\n\n${ADDRESSES[bureau].address}\n\nRe: Credit report dispute - ${d.creditor}\n\nTo the consumer dispute department:\n\nI am requesting an investigation of the following information in my ${bureau} consumer report.\n\n${facts.trim()}\n\nRequested review/correction:\n${resolution.trim()}\n\nEnclosed copies of supporting records:\n${evidence.trim()}\n\nPlease review the information and supporting records, correct any inaccurate or incomplete reporting you identify, and send me your written investigation results and an updated report when applicable.\n\nSincerely,\n\nSignature: ____________________________________\n${name.trim()}`;
}
