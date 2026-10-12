import test from "node:test";
import assert from "node:assert/strict";
import { buildSync } from "esbuild";
import { createRequire } from "node:module";
import { runInNewContext } from "node:vm";

// Execute actual registered mutation handlers against a deterministic fake DB.
// No cloud account, consumer data, credentials or financial operation is used.
const bundled = buildSync({ entryPoints: ["convex/billingData.ts"], bundle: true, platform: "node", format: "cjs", packages: "external", write: false }).outputFiles[0].text;
const module = { exports: {} };
runInNewContext(bundled, { module, exports: module.exports, require: createRequire(import.meta.url), process, console });
const handlers = module.exports;
function database() {
  const tables = new Map(); let id = 0;
  const rows = table => { if (!tables.has(table)) tables.set(table, []); return tables.get(table); };
  return {
    tables,
    query(table) {
      const predicates = [];
      const query = {
        withIndex(_name, callback) { const builder = { eq(key, value) { predicates.push(row => row[key] === value); return builder; } }; callback(builder); return query; },
        async collect() { return rows(table).filter(row => predicates.every(p => p(row))); },
        async unique() { const result = await query.collect(); assert.ok(result.length <= 1); return result[0] || null; },
      }; return query;
    },
    async insert(table, value) { const key = table + ":" + ++id; rows(table).push({ ...value, _id: key }); return key; },
    async patch(key, value) { for (const data of tables.values()) { const row = data.find(row => row._id === key); if (row) { Object.assign(row, value); return; } } throw new Error("Missing row"); },
    async delete(key) { for (const data of tables.values()) { const index = data.findIndex(row => row._id === key); if (index >= 0) { data.splice(index, 1); return; } } },
  };
}
const record = (owner = "fictional-A", token = "fictional-token-A") => ({ owner, ownerHash: "hash-" + owner, token, productId: "creditrepairai_monthly", state: "SUBSCRIPTION_STATE_ACTIVE", expiresAt: 9999999999999, active: true });
test("in-flight billing completion cannot recreate deleted records", async () => {
  const db = database(); const ctx = { db }; const pending = record();
  await handlers.upsert._handler(ctx, pending);
  await handlers.deleteOwner._handler(ctx, { owner: pending.owner, ownerHash: pending.ownerHash, actor: "fictional-actor" });
  await assert.rejects(handlers.upsert._handler(ctx, pending), /deletion/);
  assert.equal((db.tables.get("playSubscriptions") || []).length, 0);
  assert.deepEqual(Object.keys(db.tables.get("billingDeletionMarkers")[0]).sort(), ["_id", "ownerHash"]);
  assert.equal(await handlers.allowAgentRequest._handler(ctx, { owner: pending.owner, ownerHash: pending.ownerHash }), false);
});
test("a purchase token cannot bind to another account", async () => {
  const ctx = { db: database() }; await handlers.upsert._handler(ctx, record());
  await assert.rejects(handlers.upsert._handler(ctx, record("fictional-B")), /different account/);
});
test("replaced tokens remain terminal under delayed refresh", async () => {
  const db = database(); const ctx = { db }; const old = record();
  await handlers.upsert._handler(ctx, old);
  await handlers.upsert._handler(ctx, { ...record("fictional-A", "fictional-new-token"), linkedToken: old.token });
  assert.equal(await handlers.upsert._handler(ctx, old), false);
  assert.equal(db.tables.get("playSubscriptions").find(row => row.token === old.token).active, false);
});
test("deletion does not block a different owner", async () => {
  const ctx = { db: database() }; await handlers.deleteOwner._handler(ctx, { owner: "fictional-A", ownerHash: "hash-fictional-A", actor: "fictional-actor" });
  assert.equal(await handlers.upsert._handler(ctx, record("fictional-B", "fictional-token-B")), true);
});
