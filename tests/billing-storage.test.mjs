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
const policyBundle = buildSync({ entryPoints: ["convex/policyAcceptance.ts"], bundle: true, platform: "node", format: "cjs", packages: "external", write: false }).outputFiles[0].text;
const policyModule = { exports: {} };
runInNewContext(policyBundle, { module: policyModule, exports: policyModule.exports, require: createRequire(import.meta.url), process, console });
const policyHandlers = policyModule.exports;
const { POLICY_VERSION, POLICY_DIGEST } = await import("../convex/policyDocuments.ts");
const agreement = (aiSharing = false, choiceAt = Date.now()) => ({ ownerHash: "hash-fictional-A", version: POLICY_VERSION, digest: POLICY_DIGEST, termsAccepted: true, aiSharing, choiceAt });
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
test("AI sharing requires current affirmative agreement", async () => {
  const ctx = { db: database() };
  assert.equal((await policyHandlers.current._handler(ctx, { ownerHash: "hash-fictional-A" })).accepted, false);
  await assert.rejects(policyHandlers.accept._handler(ctx, { ...agreement(), termsAccepted: false }), /affirmative/);
  await assert.rejects(policyHandlers.accept._handler(ctx, { ...agreement(), digest: "changed" }), /affirmative/);
  await policyHandlers.accept._handler(ctx, agreement());
  assert.equal((await policyHandlers.current._handler(ctx, { ownerHash: "hash-fictional-A" })).aiSharing, false);
});
test("delayed grant cannot undo a newer AI revocation", async () => {
  const ctx = { db: database() }; const time = Date.now();
  const granted = agreement(true, time - 3);
  await policyHandlers.accept._handler(ctx, granted);
  await policyHandlers.accept._handler(ctx, agreement(false, time - 2));
  await policyHandlers.accept._handler(ctx, granted);
  assert.equal((await policyHandlers.current._handler(ctx, { ownerHash: "hash-fictional-A" })).aiSharing, false);
});
test("deletion removes acceptance and rejects delayed preference writes", async () => {
  const ctx = { db: database() }; const pending = agreement(true);
  await policyHandlers.accept._handler(ctx, pending);
  await handlers.deleteOwner._handler(ctx, { owner: "fictional-A", ownerHash: pending.ownerHash, actor: "fictional-actor" });
  assert.equal((ctx.db.tables.get("policyAcceptances") || []).length, 0);
  await assert.rejects(policyHandlers.accept._handler(ctx, pending), /deletion/);
});
test("acceptance cannot be backdated by the server timestamp or pinned far into the future", async () => {
  const ctx = { db: database() };
  await assert.rejects(policyHandlers.accept._handler(ctx, agreement(true, Date.now() + 3600000)), /date/);
  const before = Date.now(); await policyHandlers.accept._handler(ctx, agreement(false, before - 1000));
  assert.ok(ctx.db.tables.get("policyAcceptances")[0].acceptedAt >= before);
});
