// Development-only synthetic QA. No account bypass and no consumer data.
import { useState } from "react";
import { jsPDF } from "jspdf";
import { parsePdf } from "./pdf";
export default function PdfQA() {
  const [results, setResults] = useState<string[]>([]),
    [busy, setBusy] = useState(false),
    [progress, setProgress] = useState("");
  const run = async () => {
    setBusy(true);
    setResults([]);
    for (const bureau of ["Equifax", "Experian", "TransUnion"])
      for (const scan of [false, true]) {
        const lines = [
          `${bureau} SYNTHETIC CREDIT REPORT`,
          "Invented QA records, not a consumer report.",
          "Credit Score: 640",
          "Creditor: Fictional Cedar Bank",
          "Account Number: XXXX1234",
          "Status: Open",
          "Balance: 1280",
          "Date Opened: 01/2020",
          "Creditor: Fictional Birch Bank",
          "Account Number: XXXX5678",
          "Status: Open",
          "Balance: 0",
          "Date Opened: 01/2020",
        ];
        try {
          const pdf = new jsPDF();
          pdf.setFontSize(13);
          if (scan) {
            const canvas = document.createElement("canvas");
            canvas.width = 1400;
            canvas.height = 1800;
            const context = canvas.getContext("2d")!;
            context.fillStyle = "white";
            context.fillRect(0, 0, 1400, 1800);
            context.fillStyle = "black";
            context.font = "28px Arial";
            lines.forEach((line, i) =>
              context.fillText(line, 70, 100 + i * 65),
            );
            pdf.addImage(canvas.toDataURL("image/png"), "PNG", 0, 0, 210, 270);
          } else lines.forEach((line, i) => pdf.text(line, 15, 20 + i * 8));
          const result = await parsePdf(
            new File(
              [pdf.output("arraybuffer")],
              `${bureau}-${scan ? "scan" : "text"}.pdf`,
              { type: "application/pdf" },
            ),
            setProgress,
          );
          if (
            result.report.bureau !== bureau.toLowerCase() ||
            result.report.score !== 640 ||
            result.accounts.length !== 2 ||
            result.accounts[0].balance !== 1280 ||
            result.accounts[1].balance !== 0 ||
            result.accounts[0].accountSuffix !== "1234"
          )
            throw Error(JSON.stringify(result));
          setResults(r => [
            ...r,
            `PASS ${bureau} ${scan ? "OCR" : "embedded"}: 2 accounts, score 640, balances 1280 / 0, suffix 1234`,
          ]);
        } catch (e) {
          setResults(r => [
            ...r,
            `FAIL ${bureau} ${scan ? "OCR" : "embedded"}: ${String(e)}`,
          ]);
        }
      }
    for (const [name, bytes] of [
      ["not PDF", "not a report"],
      ["truncated", "%PDF-1.7\ninvalid"],
    ] as const) {
      try {
        await parsePdf(new File([bytes], "malformed.pdf"), setProgress);
        setResults(r => [...r, `FAIL ${name}: accepted`]);
      } catch {
        setResults(r => [...r, `PASS ${name}: safely rejected`]);
      }
    }
    setProgress("Completed");
    setBusy(false);
  };
  return (
    <main style={{ padding: 32, fontFamily: "system-ui" }}>
      <h1>Synthetic PDF / OCR verification</h1>
      <p>
        Development-only harness. All records invented. Uses the same importer
        as the signed-in app.
      </p>
      <button disabled={busy} onClick={() => void run()}>
        Run synthetic PDF checks
      </button>
      <p role="status">{progress}</p>
      <ol>
        {results.map((r, i) => (
          <li key={i}>{r}</li>
        ))}
      </ol>
    </main>
  );
}
