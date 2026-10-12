import { buildSync } from 'esbuild';
import { mkdtempSync, writeFileSync, rmSync, mkdirSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { pathToFileURL } from 'node:url';
const folder = mkdtempSync(join(tmpdir(), 'credit-schema-export-'));
try {
  const modulePath = join(folder, 'schema.mjs');
  const source = buildSync({ entryPoints: ['convex/schema.ts'], bundle: true, platform: 'node', format: 'esm', write: false }).outputFiles[0].text;
  writeFileSync(modulePath, source);
  const { default: schema } = await import(pathToFileURL(modulePath).href);
  const exported = JSON.parse(schema.export());
  mkdirSync('docs/handoff', { recursive: true });
  writeFileSync('docs/handoff/convex-schema.json', JSON.stringify(exported, null, 2) + '\n');
  console.log(`Exported ${exported.tables.length} schema tables and their actual indexes; no database records.`);
} finally { rmSync(folder, { recursive: true, force: true }); }
