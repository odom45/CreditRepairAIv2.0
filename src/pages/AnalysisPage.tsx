import { useState } from "react";
import { BarChart3, AlertTriangle, Plus, Loader2, ShieldCheck, CheckCircle2, Circle, Search } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { useMutation, useQuery } from "convex/react";
import { api } from "../../convex/_generated/api";
import { toast } from "sonner";

const sampleData = [
  {
    name: "OREGON COMMUNITY CU",
    number: "XXXX1234",
    transunion: null,
    experian: { status: "Charge-off", balance: "1,087" },
    equifax: { status: "Charge-off", balance: "1,087" },
    type: "discrepancy",
    aiReason: "Account missing from TransUnion",
    uscCode: "15 U.S. Code § 1681b(a)2"
  },
  {
    name: "BOEING EMPLOYEES CU",
    number: "XXXX5678",
    transunion: { status: "Charge-off", balance: "0,466" },
    experian: { status: "Charge Off", balance: "0,466" },
    equifax: { status: "Charge-off", balance: "0,466" },
    type: "discrepancy",
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
    uscCode: "15 U.S. Code § 1681i"
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
  },
  {
    name: "CAPITAL ONE",
    number: "XXXX4321",
    transunion: { status: "Open", balance: "450" },
    experian: { status: "Open", balance: "450" },
    equifax: { status: "Open", balance: "450" },
    type: "clean",
    aiReason: "No discrepancies found",
    uscCode: "15 U.S. Code § 1681i"
  }
];

export function AnalysisPage() {
  const [selected, setSelected] = useState<number[]>([]);
  const [isAdding, setIsAdding] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [filter, setFilter] = useState<"all" | "discrepancy" | "inquiry" | "clean">("all");
  
  const user = useQuery(api.auth.currentUser) as any;
  const createDispute = useMutation(api.disputes.create);

  const isSubscribed = user?.plan === "unlimited" || user?.plan === "pro";

  const filteredData = sampleData.filter(item => {
    const matchesSearch = item.name.toLowerCase().includes(searchQuery.toLowerCase()) || 
                         item.number.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesFilter = filter === "all" || item.type === filter;
    return matchesSearch && matchesFilter;
  });

  const toggleSelection = (index: number) => {
    if (selected.includes(index)) {
      setSelected(selected.filter(i => i !== index));
      return;
    }

    // Subscription logic
    if (!isSubscribed) {
      const currentSelectedCount = selected.length;
      if (currentSelectedCount >= 1) {
        toast.error("Free users can only dispute 1 item per month. Upgrade for unlimited disputes!", {
          action: {
            label: "Upgrade",
            onClick: () => window.location.href = "/settings"
          }
        });
        return;
      }
    }

    setSelected([...selected, index]);
  };

  const handleAddToDisputes = async () => {
    setIsAdding(true);
    try {
      for (const index of selected) {
        const account = sampleData[index];
        await createDispute({
          accountName: account.name,
          bureau: account.aiReason.includes("TransUnion") ? "TransUnion" : "All 3 Bureaus",
          reason: `${account.aiReason} - Leveraging Privacy Laws & Fraud Prevention (Violation of ${account.uscCode})`,
        });
      }
      toast.success(`Successfully generated ${selected.length} legal disputes!`);
      setSelected([]);
    } catch (error) {
      console.error(error);
      toast.error("Failed to generate disputes");
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
    <div className="max-w-6xl mx-auto space-y-8 pb-32">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
          <BarChart3 className="size-8 text-primary" />
          Full Report Analysis
        </h1>
        <p className="text-muted-foreground">Review every account and inquiry. Select any item to generate a legal dispute using USC privacy laws.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card className="bg-card/50 backdrop-blur border-slate-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-slate-400 uppercase tracking-wider">Total Items</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-black text-white">{sampleData.length}</div>
          </CardContent>
        </Card>
        <Card className="bg-card/50 backdrop-blur border-slate-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-slate-400 uppercase tracking-wider">AI Flagged</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-black text-rose-500">2</div>
          </CardContent>
        </Card>
        <Card className="bg-card/50 backdrop-blur border-slate-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-slate-400 uppercase tracking-wider">Inquiries</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-black text-amber-500">2</div>
          </CardContent>
        </Card>
        <Card className="bg-card/50 backdrop-blur border-slate-800">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-slate-400 uppercase tracking-wider">Dispute Limit</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-black text-emerald-500">{isSubscribed ? "UNLIMITED" : "1 / MO"}</div>
          </CardContent>
        </Card>
      </div>

      <div className="flex flex-col md:flex-row gap-4 items-center justify-between bg-slate-900/50 p-4 rounded-2xl border border-slate-800">
        <div className="relative w-full md:w-96">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 size-4 text-slate-500" />
          <Input 
            placeholder="Search accounts or inquiries..." 
            className="pl-10 bg-slate-950 border-slate-800"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
          />
        </div>
        <div className="flex gap-2 w-full md:w-auto overflow-x-auto pb-2 md:pb-0">
          <Button 
            variant={filter === "all" ? "default" : "outline"} 
            size="sm" 
            onClick={() => setFilter("all")}
            className="rounded-full"
          >
            All Items
          </Button>
          <Button 
            variant={filter === "discrepancy" ? "default" : "outline"} 
            size="sm" 
            onClick={() => setFilter("discrepancy")}
            className="rounded-full gap-2"
          >
            <AlertTriangle className="size-3" />
            Flagged
          </Button>
          <Button 
            variant={filter === "inquiry" ? "default" : "outline"} 
            size="sm" 
            onClick={() => setFilter("inquiry")}
            className="rounded-full gap-2"
          >
            <Search className="size-3" />
            Inquiries
          </Button>
          <Button 
            variant={filter === "clean" ? "default" : "outline"} 
            size="sm" 
            onClick={() => setFilter("clean")}
            className="rounded-full gap-2"
          >
            <CheckCircle2 className="size-3" />
            Clean
          </Button>
        </div>
      </div>

      <div className="space-y-4">
        {filteredData.map((account, idx) => {
          const originalIndex = sampleData.findIndex(item => item.name === account.name && item.number === account.number);
          const isItemSelected = selected.includes(originalIndex);
          
          return (
            <Card 
              key={idx} 
              className={`cursor-pointer transition-all hover:border-blue-500/50 overflow-hidden border-slate-800 ${
                account.type === "discrepancy" ? "border-l-4 border-l-rose-500" : 
                account.type === "inquiry" ? "border-l-4 border-l-amber-500" : ""
              } ${isItemSelected ? "bg-blue-600/10 border-blue-500 shadow-lg shadow-blue-500/10" : "bg-slate-950/50"}`}
              onClick={() => toggleSelection(originalIndex)}
            >
              <CardHeader className="pb-4">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-4">
                    <div className={`size-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                      isItemSelected ? "bg-blue-600 border-blue-600" : "border-slate-700"
                    }`}>
                      {isItemSelected ? <CheckCircle2 className="size-4 text-white" /> : <Circle className="size-4 text-transparent" />}
                    </div>
                    <div className="space-y-1">
                      <CardTitle className="text-lg font-black text-white flex items-center gap-2">
                        {account.name}
                      </CardTitle>
                      <CardDescription className="font-mono text-xs">{account.number}</CardDescription>
                    </div>
                  </div>
                  <div className="flex flex-col items-end gap-2">
                    {account.type === "discrepancy" && (
                      <Badge variant="destructive" className="bg-rose-500/10 text-rose-500 border-rose-500/20 font-bold">
                        AI FLAGGED: {account.aiReason}
                      </Badge>
                    )}
                    {account.type === "inquiry" && (
                      <Badge variant="outline" className="bg-amber-500/10 text-amber-500 border-amber-500/20 font-bold">
                        INQUIRY: {account.aiReason}
                      </Badge>
                    )}
                    {account.type === "clean" && (
                      <Badge variant="outline" className="bg-emerald-500/10 text-emerald-500 border-emerald-500/20 font-bold">
                        CLEAN ACCOUNT
                      </Badge>
                    )}
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-3 gap-4">
                  <div className="space-y-2 text-center p-3 rounded-xl bg-slate-900/50 border border-slate-800">
                    <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">TransUnion</p>
                    {account.transunion ? (
                      <>
                        <Badge variant="outline" className={getStatusStyle(account.transunion.status)}>
                          {account.transunion.status}
                        </Badge>
                        <p className="text-sm font-black text-white mt-1">${account.transunion.balance}</p>
                      </>
                    ) : (
                      <p className="text-xs font-bold text-rose-500/50 italic">NOT REPORTED</p>
                    )}
                  </div>

                  <div className="space-y-2 text-center p-3 rounded-xl bg-slate-900/50 border border-slate-800">
                    <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Experian</p>
                    {account.experian ? (
                      <>
                        <Badge variant="outline" className={getStatusStyle(account.experian.status)}>
                          {account.experian.status}
                        </Badge>
                        <p className="text-sm font-black text-white mt-1">${account.experian.balance}</p>
                      </>
                    ) : (
                      <p className="text-xs font-bold text-rose-500/50 italic">NOT REPORTED</p>
                    )}
                  </div>

                  <div className="space-y-2 text-center p-3 rounded-xl bg-slate-900/50 border border-slate-800">
                    <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest">Equifax</p>
                    {account.equifax ? (
                      <>
                        <Badge variant="outline" className={getStatusStyle(account.equifax.status)}>
                          {account.equifax.status}
                        </Badge>
                        <p className="text-sm font-black text-white mt-1">${account.equifax.balance}</p>
                      </>
                    ) : (
                      <p className="text-xs font-bold text-rose-500/50 italic">NOT REPORTED</p>
                    )}
                  </div>
                </div>
                
                {isItemSelected && (
                  <div className="mt-4 p-4 rounded-xl bg-blue-600/10 border border-blue-500/20 flex items-start gap-3 animate-in fade-in slide-in-from-top-2">
                    <ShieldCheck className="size-5 text-blue-400 shrink-0 mt-0.5" />
                    <div className="space-y-1">
                      <p className="text-xs text-blue-100 leading-relaxed">
                        <span className="font-black text-blue-400 uppercase tracking-wider mr-2">Legal Strategy:</span> 
                        Leveraging <span className="font-mono font-bold text-white">{account.uscCode}</span>. 
                        This dispute will challenge the bureau to provide strict proof of authorization and original documentation under federal privacy laws.
                      </p>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          );
        })}
      </div>

      {selected.length > 0 && (
        <div className="fixed bottom-8 left-1/2 -translate-x-1/2 w-full max-w-md px-4 z-50">
          <Card className="shadow-2xl border-blue-500/20 bg-slate-900/95 backdrop-blur-xl ring-1 ring-blue-500/20 rounded-3xl overflow-hidden">
            <CardContent className="p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="size-12 rounded-2xl bg-blue-600 flex items-center justify-center text-white font-black shadow-lg shadow-blue-600/20">
                  {selected.length}
                </div>
                <div>
                  <p className="font-black text-white text-sm">Items Selected</p>
                  <p className="text-[10px] text-slate-400 uppercase font-bold tracking-widest">Legal codes applied</p>
                </div>
              </div>
              <Button 
                className="h-12 px-6 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-black gap-2 shadow-lg shadow-blue-600/20 transition-all active:scale-95" 
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
