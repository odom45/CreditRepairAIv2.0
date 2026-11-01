import { jsPDF } from "jspdf";
export { ADDRESSES, composeLetter } from "./letterText";
export function saveLetterPdf(text: string, name: string) {
  const pdf = new jsPDF({ unit: "pt", format: "letter" });
  pdf.setFont("helvetica");
  pdf.setFontSize(11);
  const lines = pdf.splitTextToSize(text, 504) as string[];
  let y = 54;
  for (const line of lines) {
    if (y > 738) {
      pdf.addPage();
      y = 54;
    }
    pdf.text(line, 54, y);
    y += 15;
  }
  pdf.save(name);
}
export function download(
  data: string,
  fileName: string,
  type = "application/json",
) {
  const url = URL.createObjectURL(new Blob([data], { type }));
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 30000);
}
