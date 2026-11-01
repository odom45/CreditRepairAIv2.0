import { getDocument, GlobalWorkerOptions } from "pdfjs-dist";
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";
import { createWorker, OEM } from "tesseract.js";
import { parseText, validateText } from "./parser.ts";
GlobalWorkerOptions.workerSrc = workerUrl;
export async function parsePdf(
  file: File,
  progress: (message: string) => void,
) {
  if (file.size > 50_000_000 || file.size < 5)
    throw Error("Choose a PDF smaller than 50 MB.");
  const bytes = new Uint8Array(await file.arrayBuffer());
  if (new TextDecoder().decode(bytes.slice(0, 5)) !== "%PDF-")
    throw Error("Selected file is not a PDF.");
  const task = getDocument({ data: bytes, useSystemFonts: true });
  const timer = setTimeout(() => {
    void task.destroy();
  }, 180000);
  let pdfDoc;
  try {
    pdfDoc = await task.promise;
    if (pdfDoc.numPages > 200) throw Error("PDF exceeds 200 pages.");
    let text = "";
    for (let index = 1; index <= pdfDoc.numPages; index++) {
      progress(`Reading page ${index} of ${pdfDoc.numPages} on this device`);
      const page = await pdfDoc.getPage(index);
      const content = await page.getTextContent();
      let previousY: number | null = null;
      for (const item of content.items) {
        if (!("str" in item)) continue;
        const y = item.transform[5];
        if (previousY !== null && Math.abs(previousY - y) > 3) text += "\n";
        text += item.str + (item.hasEOL ? "\n" : " ");
        previousY = y;
        if (text.length > 1_000_000)
          throw Error("PDF text exceeds 1,000,000 characters.");
      }
      text += "\n";
      page.cleanup();
    }
    let method = "Embedded PDF text";
    if (text.replace(/\s/g, "").length < 200) {
      if (pdfDoc.numPages > 30)
        throw Error(
          "Scanned PDF exceeds 30 pages. Import smaller sections; no pages were skipped.",
        );
      progress("Starting private on-device OCR");
      const ocr = await createWorker("eng", OEM.LSTM_ONLY, {
        workerPath: "/ocr/worker.min.js",
        corePath: "/ocr/core",
        langPath: "/ocr",
        workerBlobURL: false,
        gzip: true,
      });
      try {
        text = "";
        method = "On-device OCR";
        for (let index = 1; index <= pdfDoc.numPages; index++) {
          progress(`OCR page ${index} of ${pdfDoc.numPages} on this device`);
          const page = await pdfDoc.getPage(index);
          const base = page.getViewport({ scale: 1 });
          const viewport = page.getViewport({ scale: 2048 / base.width });
          if (viewport.height > 4096 || viewport.height <= 0)
            throw Error("Page dimensions exceed the safe OCR limit.");
          const canvas = document.createElement("canvas");
          canvas.width = Math.ceil(viewport.width);
          canvas.height = Math.ceil(viewport.height);
          const context = canvas.getContext("2d");
          if (!context) throw Error("Canvas is unavailable.");
          await page.render({ canvasContext: context, canvas, viewport })
            .promise;
          text += (await ocr.recognize(canvas)).data.text + "\n";
          canvas.width = 0;
          canvas.height = 0;
          page.cleanup();
          if (text.length > 1_000_000)
            throw Error("OCR text exceeds the safe limit.");
        }
      } finally {
        await ocr.terminate();
      }
    }
    if (text.replace(/\s/g, "").length < 80)
      throw Error(
        "Not enough readable text. Try an original downloaded report.",
      );
    validateText(text);
    return parseText(text, file.name, method);
  } finally {
    clearTimeout(timer);
    await task.destroy();
  }
}
