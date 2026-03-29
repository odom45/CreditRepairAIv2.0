import { useState } from "react";
import { BarChart3, AlertTriangle, Plus, Loader2, Info, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useMutation } from "convex/react";
import { api } from "../../convex/_generated/api";
import { toast } from "sonner";

const sampleData = [
  {
    name: "OREGON COMMUNITY CU",
    number: "XXXX1234",
    transunion: null,
    experian: { status: "Charge-off", balance: "1,087" },
    equifax: { status: "Charge-off", balance: "1,087" },
    type: "missing",
    aiReason: "Account missing from TransUnion",
    uscCode: "15 U.S. Code § 1681b(a)2"
  },
  {
    name: "BOEING EMPLOYEES CU",
    number: "XXXX5678",
    transunion: { status: "Charge-off", balance: "0,466" },
    experian: { status: "Charge Off", balance: "0,466" },
    equifax: { status: "Charge-off", balance: "0,466" },
    type: "wording",
    aiReason: "Status wording inconsistent",
    uscCode: "15 U.S. Code § 1681(e)b"
  },
  {
    name: "CHIME/STRIDE BANK",
    number: "XXXX7890",
    transunion: { status: "Open", balance: "0" },
    experian: { status: "Open", balance: "0" },
    equifax: { status: "Open", balance: "0" },
    type: "clean",
    aiReason: "No discrepancies found",
    uscCode: ""
  },
  {
    name: "HARD INQUIRY - CHASE",
    number: "2026-01-15",
    transunion: { status: "Inquiry", balance: "0" },
    experian: null,
    equifax: null,
    type: "inquiry",
    aiReason: "Unauthorized hard inquiry",
    uscCode: "15 U.S. Code § 1681b"
  },
  {
    name: "SOFT INQUIRY - AMEX",
    number: "2026-02-10",
    transunion: { status: "Soft Inquiry", balance: "0" },
    experian: null,
    equifax: null,
    type: "inquiry",
    aiReason: "Unauthorized soft inquiry",
    uscCode: "15 U.S. Code § 1681b"
  }
];

export function AnalysisPage() {
  const [selected, setSelected] = useState<number[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const createDispute = useMutation(api.disputes.create);

  const toggleSelection = (index: number) => {
    if (selected.includes(index)) {
      setSelected(selected.filter(i => i !== index));
    } else {
      setSelected([...selected, index]);
    }
  };

  const handleAddToDisputes = async () => {
    setIsAdding(true);
    try {
      for (const index of selected) {
        const account = sampleData[index];
        await createDispute({
          accountName: account.name,
          bureau: account.aiReason.includes("TransUnion") ? "TransUnion" : "All 3 Bureaus",
          reason: `${account.aiReason} - Violation of ${account.uscCode}`,
        });
      }
      toast.success(`Added ${selected.length} accounts to disputes`);
      setSelected([]);
    } catch (error) {
      console.error(error);
      toast.error("Failed to add to disputes");
    } finally {
      setIsAdding(false);
    }
  };

  const getStatusStyle = (status: string) => {
    const statusLower = status.toLowerCase();
    if (statusLower.includes("charge") || statusLower.includes("collection") || statusLower.includes("inquiry")) {
      return "bg-red-500/10 text-red-500 border-red-500/20";
    }
    if (statusLower.includes("paid") || statusLower.includes("closed")) {
      return "bg-emerald-500/10 text-emerald-500 border-emerald-500/20";
    }
    return "bg-amber-500/10 text-amber-500 border-amber-500/20";
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
          <BarChart3 className="size-8 text-primary" />
          AI Analysis & USC Violations
        </h1>
        <p className="text-muted-foreground">AI-detected mismatches and legal code violations across all three bureaus</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="bg-card/50 backdrop-blur">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">ACCOUNTS</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">14</div>
          </CardContent>
        </Card>
        <Card className="bg-card/50 backdrop-blur">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">VIOLATIONS</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-destructive">5</div>
          </CardContent>
        </Card>
        <Card className="bg-card/50 backdrop-blur">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">INQUIRIES</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-amber-500">3</div>
          </CardContent>
        </Card>
        <Card className="bg-card/50 backdrop-blur">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">POTENTIAL BOOST</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-emerald-500">+55 pts</div>
          </CardContent>
        </Card>
      </div>

      <div className="space-y-4">
        {sampleData.map((account, index) => (
          <Card 
            key={index} 
            className={`cursor-pointer transition-all hover:border-primary/50 overflow-hidden ${
              account.type !== "clean" ? "border-l-4 border-l-destructive" : ""
            } ${selected.includes(index) ? "bg-primary/5 border-primary shadow-lg" : "bg-card/50"}`}
            onClick={() => toggleSelection(index)}
          >
            <CardHeader className="pb-4">
              <div className="flex items-start justify-between">
                <div className="space-y-1">
                  <CardTitle className="text-lg flex items-center gap-2">
                    {account.name}
                    {selected.includes(index) && <ShieldCheck className="size-5 text-primary" />}
                  </CardTitle>
                  <CardDescription>{account.number}</CardDescription>
                </div>
                {account.type !== "clean" && (
                  <div className="flex flex-col items-end gap-2">
                    <Badge variant="destructive" className="flex items-center gap-1">
                      <AlertTriangle className="size-3" />
                      {account.aiReason}
                    </Badge>
                    <Badge variant="outline" className="bg-primary/10 text-primary border-primary/20 font-mono text-[10px]">
                      {account.uscCode}
                    </Badge>
                  </div>
                )}
              </div>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-4">
                <div className="space-y-2 text-center p-3 rounded-xl bg-muted/30 border border-transparent transition-colors group-hover:border-primary/10">
                  <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">TransUnion</p>
                  {account.transunion ? (
                    <>
                      <Badge variant="outline" className={getStatusStyle(account.transunion.status)}>
                        {account.transunion.status}
                      </Badge>
                      <p className="text-sm font-bold mt-1">${account.transunion.balance}</p>
                    </>
                  ) : (
                    <p className="text-sm font-bold text-destructive/50 italic">MISSING</p>
                  )}
                </div>

                <div className="space-y-2 text-center p-3 rounded-xl bg-muted/30 border border-transparent transition-colors group-hover:border-primary/10">
                  <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Experian</p>
                  {account.experian ? (
                    <>
                      <Badge variant="outline" className={getStatusStyle(account.experian.status)}>
                        {account.experian.status}
                      </Badge>
                      <p className="text-sm font-bold mt-1">${account.experian.balance}</p>
                    </>
                  ) : (
                    <p className="text-sm font-bold text-destructive/50 italic">MISSING</p>
                  )}
                </div>

                <div className="space-y-2 text-center p-3 rounded-xl bg-muted/30 border border-transparent transition-colors group-hover:border-primary/10">
                  <p className="text-[10px] font-bold text-muted-foreground uppercase tracking-widest">Equifax</p>
                  {account.equifax ? (
                    <>
                      <Badge variant="outline" className={getStatusStyle(account.equifax.status)}>
                        {account.equifax.status}
                      </Badge>
                      <p className="text-sm font-bold mt-1">${account.equifax.balance}</p>
                    </>
                  ) : (
                    <p className="text-sm font-bold text-destructive/50 italic">MISSING</p>
                  )}
                </div>
              </div>
              
              {selected.includes(index) && account.uscCode && (
                <div className="mt-4 p-3 rounded-lg bg-primary/10 border border-primary/20 flex items-start gap-3 animate-in fade-in slide-in-from-top-2">
                  <Info className="size-5 text-primary shrink-0 mt-0.5" />
                  <p className="text-xs text-primary-foreground/80 leading-relaxed">
                    <span className="font-bold">Legal Strategy:</span> This dispute will leverage <span className="font-mono">{account.uscCode}</span> to challenge the accuracy of this reporting. This specific code requires the bureau to provide proof of the original contract or delete the item within 30 days.
                  </p>
                </div>
              )}
            </CardContent>
          </Card>
        ))}
      </div>

      {selected.length > 0 && (
        <div className="fixed bottom-8 left-1/2 -translate-x-1/2 w-full max-w-md px-4 z-50">
          <Card className="shadow-2xl border-primary/20 bg-card/95 backdrop-blur-xl ring-1 ring-primary/20">
            <CardContent className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="size-10 rounded-full bg-primary flex items-center justify-center text-primary-foreground font-bold shadow-lg shadow-primary/20">
                  {selected.length}
                </div>
                <div>
                  <p className="font-bold text-sm">Ready to Dispute</p>
                  <p className="text-[10px] text-muted-foreground uppercase tracking-wider">Legal codes applied</p>
                </div>
              </div>
              <Button 
                className="gap-2 shadow-lg shadow-primary/20" 
                onClick={handleAddToDisputes}
                disabled={isAdding}
              >
                {isAdding ? (
                  <Loader2 className="size-4 animate-spin" />
                ) : (
                  <Plus className="size-4" />
                )}
                Generate Disputes
              </Button>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
