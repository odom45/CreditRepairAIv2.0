import { type ChildProcess, spawn } from "node:child_process";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { createPageHelper } from "./auth";

const __dirname = dirname(fileURLToPath(import.meta.url));
const projectRoot = resolve(__dirname, "..");

const PREVIEW_PORT = 4173;
const PREVIEW_URL = `http://localhost:${PREVIEW_PORT}`;
const MAX_WAIT_MS = 30000;
const POLL_INTERVAL_MS = 500;

async function waitForServer(url: string, maxWait: number): Promise<boolean> {
  const start = Date.now();
  while (Date.now() - start < maxWait) {
    try {
      const response = await fetch(url);
      if (response.ok || response.status === 304) {
        return true;
      }
    } catch {
      // Server not ready yet
    }
    await new Promise(r => setTimeout(r, POLL_INTERVAL_MS));
  }
  return false;
}

function startPreviewServer(): ChildProcess {
  const server = spawn("bun", ["run", "preview"], {
    cwd: projectRoot,
    stdio: ["ignore", "pipe", "pipe"],
    detached: false,
  });

  server.stdout?.on("data", () => {});
  server.stderr?.on("data", () => {});

  return server;
}

async function main() {
  console.log("🚀 Starting preview server...");
  const server = startPreviewServer();

  try {
    console.log(`⏳ Waiting for server at ${PREVIEW_URL}...`);
    const ready = await waitForServer(PREVIEW_URL, MAX_WAIT_MS);

    if (!ready) {
      console.error("❌ Server failed to start.");
      process.exit(1);
    }

    process.env.APP_URL = PREVIEW_URL;
    const helper = await createPageHelper();
    const { page } = helper;

    console.log("📸 Navigating to /analysis...");
    await helper.goto("/analysis");
    
    console.log("🖱️ Selecting accounts...");
    // Click the first two cards
    const cards = page.locator(".cursor-pointer.transition-all");
    await cards.nth(0).click();
    await cards.nth(1).click();
    
    // Wait for the selection bar to appear
    await page.waitForSelector("text=Accounts selected");
    
    console.log("📸 Taking screenshot...");
    await helper.screenshot("analysis_selected.png");

    // Now click "Add to Disputes"
    console.log("🖱️ Clicking Add to Disputes...");
    await page.click("text=Add to Disputes");
    
    // Wait for toast
    await page.waitForSelector("text=Added 2 accounts to disputes");
    await helper.screenshot("analysis_success_toast.png");

    // Go to disputes page
    console.log("📸 Navigating to /disputes...");
    await helper.goto("/disputes");
    await page.waitForSelector("text=OREGON COMMUNITY CU");
    await helper.screenshot("disputes_populated.png");

    await helper.close();
    console.log("\n✅ Done!");
  } finally {
    server.kill("SIGTERM");
  }
}

main().catch(err => {
  console.error("Failed:", err);
  process.exit(1);
});
