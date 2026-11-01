import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";
const root = document.getElementById("root");
if (!root) throw new Error("Missing root element.");
if (import.meta.env.DEV && location.pathname === "/__qa__/parser") {
  void import("./workspace/PdfQA").then(({ default: PdfQA }) =>
    createRoot(root).render(<PdfQA />),
  );
} else
  createRoot(root).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
