import { useState } from "react";
import { useAction, useQuery } from "convex/react";
import { api } from "../../convex/_generated/api";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { toast } from "sonner";
import type { Id } from "../../convex/_generated/dataModel";

export function AdminPage() {
  const identity = useQuery(api.billingData.adminIdentity);
  const rows = useQuery(api.billingData.adminList);
  const manage = useAction(api.playBilling.adminManage);
  const [busy, setBusy] = useState(false);
  const [pending, setPending] = useState<{ id: Id<"playSubscriptions">; operation: "cancel" | "refundAndRevoke" } | null>(null);
  async function perform(id: Id<"playSubscriptions">, operation: "refresh" | "cancel" | "refundAndRevoke") {
    setBusy(true);
    try {
      await manage({ id, operation, confirmed: true });
      toast.success(operation === "refresh" ? "Play status refreshed." : "Google Play action completed and status refreshed.");
      setPending(null);
    } catch {
      toast.error("Action failed or status is pending. Refresh and check Play Console before retrying a financial action.");
    } finally { setBusy(false); }
  }
  if (identity === undefined) return <p role="status">Checking administrator access…</p>;
  if (!identity) return <p>Sign in with the owner’s administrator account.</p>;
  if (!identity.isAdmin) return <div className="space-y-4"><h1 className="text-2xl font-bold">Administrator setup required</h1><p>Server authorization is required; an email or client-side check cannot grant access. Ask the owner to add this account’s identity to the protected BILLING_ADMIN_IDENTITIES environment setting.</p><p className="break-all">Your authenticated identity: {identity.identity}</p></div>;
  return <div className="space-y-6 pb-16">
    <h1 className="text-3xl font-bold">Play subscription management</h1>
    <p>Live, server-verified monthly software access. No simulated refunds, invented user totals, or outcome-based fees. Purchase tokens are not exposed to this screen.</p>
    {rows === undefined ? <p role="status">Loading subscriptions…</p> : rows.length === 0 ? <p>No verified Play subscriptions recorded yet.</p> : rows.map(row => <Card key={row._id}>
      <CardHeader><CardTitle className="break-all">Account: {row.owner}</CardTitle></CardHeader>
      <CardContent className="space-y-3">
        <p>{row.productId} · {row.state} · {row.active && row.expiresAt > Date.now() ? "Access eligible" : "No active access"}</p>
        <p>Play expiry: {new Date(row.expiresAt).toLocaleString()} · Last checked: {new Date(row.checkedAt).toLocaleString()}</p>
        <div className="flex flex-wrap gap-3">
          <Button disabled={busy} variant="outline" onClick={() => void perform(row._id, "refresh")}>Refresh from Play</Button>
          <Button disabled={busy} variant="outline" onClick={() => setPending({ id: row._id, operation: "cancel" })}>Stop future renewals</Button>
          <Button disabled={busy} variant="destructive" onClick={() => setPending({ id: row._id, operation: "refundAndRevoke" })}>Refund and revoke access</Button>
        </div>
      </CardContent>
    </Card>)}
    {pending && <Card role="dialog" aria-label="Confirm Google Play account action"><CardContent className="pt-6 space-y-4">
      <p>{pending.operation === "refundAndRevoke" ? "This issues a full refund for the current subscription purchase and revokes access immediately via Google Play. Verify the account and transaction in Play Console first." : "This cancels future subscription renewals through Google Play. Existing paid access can remain until its expiry."}</p>
      <p>Target subscription: {pending.id}</p>
      <Button disabled={busy} variant="destructive" onClick={() => void perform(pending.id, pending.operation)}>Confirm actual Play action</Button>{" "}
      <Button disabled={busy} variant="outline" onClick={() => setPending(null)}>Cancel</Button>
    </CardContent></Card>}
  </div>;
}
