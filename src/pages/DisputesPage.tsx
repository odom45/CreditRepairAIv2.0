import { useQuery, useMutation } from "convex/react";
import { Mail, Plus, ChevronRight, Trash2, Send, FileText, ShieldCheck, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { api } from "../../convex/_generated/api";
import { toast } from "sonner";
import { useState } from "react";

export function DisputesPage() {
  const disputes = useQuery(api.disputes.list);
  const removeDispute = useMutation(api.disputes.remove);
  const updateStatus = useMutation(api.disputes.updateStatus);
  const [sendingId, setSendingId] = useState<string | null>(null);

  const handleDelete = async (id: any) => {
    try {
      await removeDispute({ id });
      toast.success("Dispute deleted");
    } catch (error) {
      toast.error("Failed to delete dispute");
    }
  };

  const handleSend = async (id: any) => {
    setSendingId(id);
    try {
      await updateStatus({ id, status: "sent" });
      toast.success("Dispute marked as sent!");
    } catch (error) {
      toast.error("Failed to update status");
    } finally {
      setSendingId(null);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case "draft": return "bg-slate-500/10 text-slate-500 border-slate-500/20";
      case "sent": return "bg-blue-500/10 text-blue-500 border-blue-500/20";
      case "resolved": return "bg-emerald-500/10 text-emerald-500 border-emerald-500/20";
      default: return "bg-slate-500/10 text-slate-500 border-slate-500/20";
    }
  };

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div className="flex items-center justify-between">
        <div className="flex flex-col gap-2">
          <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
            <Mail className="size-8 text-primary" />
            Dispute Letters
          </h1>
          <p className="text-muted-foreground">Manage and track your credit dispute letters with legal USC codes.</p>
        </div>
        <Button className="gap-2 shadow-lg shadow-primary/20">
          <Plus className="size-4" />
          New Dispute
        </Button>
      </div>

      <div className="grid gap-6">
        {disputes?.length === 0 ? (
          <Card className="border-dashed py-12 bg-muted/30">
            <CardContent className="flex flex-col items-center justify-center text-center space-y-4">
              <div className="size-16 rounded-full bg-muted flex items-center justify-center">
                <FileText className="size-8 text-muted-foreground" />
              </div>
              <div className="space-y-1">
                <h3 className="text-xl font-semibold">No disputes yet</h3>
                <p className="text-muted-foreground max-w-xs">
                  Start by analyzing your credit reports and selecting accounts to dispute.
                </p>
              </div>
              <Button variant="outline" asChild>
                <a href="/analysis">Go to Analysis</a>
              </Button>
            </CardContent>
          </Card>
        ) : (
          disputes?.map((dispute) => (
            <Card key={dispute._id} className="group overflow-hidden border-l-4 border-l-primary/20 hover:border-l-primary transition-all">
              <CardContent className="p-6 flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="size-12 rounded-xl bg-primary/10 flex items-center justify-center text-primary shadow-inner">
                    <Mail className="size-6" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <h3 className="font-bold text-lg">{dispute.accountName}</h3>
                      <Badge variant="outline" className={getStatusColor(dispute.status)}>
                        {dispute.status.toUpperCase()}
                      </Badge>
                    </div>
                    <div className="flex items-center gap-3 text-sm text-muted-foreground mt-1">
                      <span className="font-bold text-primary uppercase tracking-widest text-[10px]">{dispute.bureau}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <ShieldCheck className="size-3 text-emerald-500" />
                        Legal Code Applied
                      </span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-all translate-x-2 group-hover:translate-x-0">
                  {dispute.status === "draft" && (
                    <Button 
                      variant="default" 
                      size="sm" 
                      className="gap-2 shadow-md" 
                      onClick={() => handleSend(dispute._id)}
                      disabled={sendingId === dispute._id}
                    >
                      {sendingId === dispute._id ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
                      Mark Sent
                    </Button>
                  )}
                  <Button variant="ghost" size="icon" onClick={() => handleDelete(dispute._id)}>
                    <Trash2 className="size-4 text-destructive" />
                  </Button>
                  <Button variant="ghost" size="icon">
                    <ChevronRight className="size-4" />
                  </Button>
                </div>
              </CardContent>
              <div className="px-6 pb-4 pt-0">
                <div className="p-3 rounded-lg bg-muted/50 border border-muted flex items-start gap-3">
                  <AlertCircle className="size-4 text-muted-foreground shrink-0 mt-0.5" />
                  <p className="text-xs text-muted-foreground italic leading-relaxed">
                    "{dispute.reason}"
                  </p>
                </div>
              </div>
            </Card>
          ))
        )}
      </div>
    </div>
  );
}
