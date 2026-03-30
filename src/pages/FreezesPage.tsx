import { useState } from "react";
import { Shield, ExternalLink, Lock, Unlock, AlertCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";

const bureaus = [
  {
    name: "SageStream (LexisNexis)",
    url: "https://consumer.risk.lexisnexis.com/freeze",
    description: "Commonly used for credit card and auto loan approvals.",
    status: "unfrozen"
  },
  {
    name: "CoreLogic Credco",
    url: "https://www.corelogic.com/solutions/consumer-services/",
    description: "Used by mortgage lenders to verify credit history.",
    status: "unfrozen"
  },
  {
    name: "Invisas",
    url: "https://www.innovis.com/personal/securityFreeze",
    description: "The fourth major credit bureau, often overlooked.",
    status: "unfrozen"
  },
  {
    name: "ChexSystems",
    url: "https://www.chexsystems.com/web/chexsystems/consumerdebit/page/securityfreeze/placefreeze/",
    description: "Used by banks to track checking and savings account history.",
    status: "unfrozen"
  },
  {
    name: "ARS (Advanced Resolution Services)",
    url: "https://www.ars-consumerreports.com/freeze",
    description: "Used by major credit card issuers like Visa and Mastercard.",
    status: "unfrozen"
  }
];

export function FreezesPage() {
  const [freezeStatus, setFreezeStatus] = useState<Record<string, string>>(
    Object.fromEntries(bureaus.map(b => [b.name, b.status]))
  );

  const toggleFreeze = (name: string) => {
    const current = freezeStatus[name];
    const next = current === "frozen" ? "unfrozen" : "frozen";
    setFreezeStatus({ ...freezeStatus, [name]: next });
    toast.success(`${name} ${next === "frozen" ? "frozen" : "unfrozen"} successfully!`);
  };

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
          <Shield className="size-8 text-primary" />
          Secondary Bureau Freezes
        </h1>
        <p className="text-muted-foreground">Freeze secondary bureaus to prevent them from sharing data during the dispute process.</p>
      </div>

      <Card className="bg-amber-500/5 border-amber-500/20">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-medium flex items-center gap-2 text-amber-600">
            <AlertCircle className="size-4" />
            Why freeze secondary bureaus?
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            Secondary bureaus often provide the "verification" that the big three (TransUnion, Experian, Equifax) use to validate negative items. Freezing them makes it harder for bureaus to verify disputed information, increasing your chances of deletion.
          </p>
        </CardContent>
      </Card>

      <div className="grid gap-4">
        {bureaus.map((bureau) => (
          <Card key={bureau.name} className="group">
            <CardContent className="p-6 flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className={`size-12 rounded-lg flex items-center justify-center ${
                  freezeStatus[bureau.name] === "frozen" ? "bg-amber-500/10 text-amber-500" : "bg-primary/10 text-primary"
                }`}>
                  {freezeStatus[bureau.name] === "frozen" ? <Lock className="size-6" /> : <Unlock className="size-6" />}
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <h3 className="font-bold text-lg">{bureau.name}</h3>
                    <Badge variant="outline" className={
                      freezeStatus[bureau.name] === "frozen" 
                        ? "bg-amber-500/10 text-amber-500 border-amber-500/20" 
                        : "bg-emerald-500/10 text-emerald-500 border-emerald-500/20"
                    }>
                      {freezeStatus[bureau.name].toUpperCase()}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">{bureau.description}</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <Button variant="outline" size="sm" className="gap-2" asChild>
                  <a href={bureau.url} target="_blank" rel="noopener noreferrer">
                    <ExternalLink className="size-4" />
                    Visit Site
                  </a>
                </Button>
                <Button 
                  variant={freezeStatus[bureau.name] === "frozen" ? "outline" : "default"} 
                  size="sm" 
                  onClick={() => toggleFreeze(bureau.name)}
                >
                  {freezeStatus[bureau.name] === "frozen" ? "Unfreeze" : "Freeze"}
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
