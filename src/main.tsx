import { ConvexAuthProvider } from "@convex-dev/auth/react";
import { ConvexReactClient } from "convex/react";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import App from "./App";
import "./index.css";

const convexUrl = import.meta.env.VITE_CONVEX_URL;

if (!convexUrl) {
  throw new Error(
    "Missing VITE_CONVEX_URL. Copy .env.example to .env.local and set your Convex deployment URL.",
  );
}

const convex = new ConvexReactClient(convexUrl);
const root = document.getElementById("root");

if (!root) {
  throw new Error('Missing root element. Expected <div id="root"></div> in index.html.');
}

createRoot(root).render(
  <StrictMode>
    <ConvexAuthProvider client={convex}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </ConvexAuthProvider>
  </StrictMode>,
);
