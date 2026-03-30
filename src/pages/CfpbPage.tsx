import { useState, useEffect } from "react";
import { Globe, ExternalLink, Info, CheckCircle2, AlertCircle, Loader2, Copy, Check, ShieldAlert, HelpCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { useQuery } from "convex/react";
import { api } from "../../convex/_generated/api";
import { toast } from "sonner";

const steps = [
  {
    title: "Start Complaint",
    description: "Go to the official CFPB portal and select 'Credit reporting'.",
    url: "https://www.consumerfinance.gov/complaint/",
    icon: Globe,
    tip: "Select 'Credit reporting, credit repair services, or other personal consumer reports' as the product."
  },
  {
    title: "Identify Problem",
    description: "Choose 'Incorrect information on your report' as the issue.",
    icon: ShieldAlert,
    tip: "Then select 'Information belongs to someone else' or 'Account status is incorrect' based on your AI analysis."
  },
  {
    title: "Paste AI Dispute",
    description: "Copy the auto-populated legal response from below and paste it into the 'What happened' box.",
    icon: Copy,
    tip: "This text includes the specific USC codes that force the bureau to respond legally."
  },
  {
    title: "Submit & Track",
    description: "Review your details and submit. The CFPB will forward this to the bureau.",
    icon: CheckCircle2,
    tip: "Bureaus usually respond within 15 days when a CFPB complaint is filed."
  }
];

export function CfpbPage() {
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);
  const [copiedId, setCopiedId] = useState<string | null>(null);
  const [showGuide, setShowGuide] = useState(true);
  
  const disputes = useQuery(api.disputes.list);

  const handleNext = () => {
    if (activeStep < steps.length - 1) {
      setActiveStep(activeStep + 1);
    }
  };

  const handlePrev = () => {
    if (activeStep > 0) {
      setActiveStep(activeStep - 1);
    }
  };

  const handleOpenCfpb = () => {
    setLoading(true);
    window.open("https://www.consumerfinance.gov/complaint/", "_blank");
    setTimeout(() => setLoading(false), 1000);
    toast.success("CFPB portal opened! Follow the guide on the left.");
  };

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedId(id);
    toast.success("Legal response copied to clipboard!");
    setTimeout(() => setCopiedId(null), 2000);
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-20">
      {/* Introduction Guide */}
      {showGuide && (
        <Card className="bg-blue-600 border-none text-white overflow-hidden relative">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <HelpCircle className="size-32" />
          </div>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle className="text-2xl font-black flex items-center gap-2">
                <Zap className="size-6 fill-white" />
                How to use the CFPB Assistant
              </CardTitle>
              <Button variant="ghost" size="sm" className="text-white hover:bg-white/10" onClick={() => setShowGuide(false)}>Dismiss</Button>
            </div>
            <CardDescription className="text-blue-100 text-lg max-w-3xl">
              The CFPB (Consumer Financial Protection Bureau) is a government agency that oversees credit bureaus. 
              Filing a complaint here is the **most effective** way to get errors removed because bureaus are legally 
              required to respond to the government within 15 days.
            </CardDescription>
          </CardHeader>
          <CardContent className="grid md:grid-cols-3 gap-6">
            <div className="bg-white/10 p-4 rounded-2xl backdrop-blur-sm">
              <p className="font-bold mb-1">1. Open Portal</p>
              <p className="text-sm text-blue-50">We'll open the official government site in a new tab for you.</p>
            </div>
            <div className="bg-white/10 p-4 rounded-2xl backdrop-blur-sm">
              <p className="font-bold mb-1">2. Copy Legal Text</p>
              <p className="text-sm text-blue-50">Use our auto-populated legal responses below. They contain the USC codes.</p>
            </div>
            <div className="bg-white/10 p-4 rounded-2xl backdrop-blur-sm">
              <p className="font-bold mb-1">3. Paste & Submit</p>
              <p className="text-sm text-blue-50">Paste the text into the CFPB form. It's "idiot-proof" and takes 5 minutes.</p>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-black tracking-tight flex items-center gap-2">
          <Globe className="size-8 text-blue-500" />
          CFPB Government Assistant
        </h1>
        <p className="text-muted-foreground font-medium">Follow this step-by-step guide to file your legal disputes directly with the government.</p>
      </div>

      <div className="grid gap-8 lg:grid-cols-12">
        {/* Left: Steps Guide */}
        <div className="lg:col-span-4 space-y-4">
          <h3 className="text-sm font-black text-slate-500 uppercase tracking-widest px-2">Interactive Guide</h3>
          {steps.map((step, index) => (
            <Card 
              key={index} 
              className={`cursor-pointer transition-all border-2 ${
                activeStep === index ? "border-blue-500 bg-blue-500/5 shadow-lg" : "border-slate-800 opacity-60 hover:opacity-100"
              }`}
              onClick={() => setActiveStep(index)}
            >
              <CardHeader className="p-4">
                <div className="flex items-start gap-4">
                  <div className={`size-10 rounded-xl flex items-center justify-center text-lg font-black shrink-0 ${
                    activeStep === index ? "bg-blue-500 text-white" : "bg-slate-800 text-slate-400"
                  }`}>
                    {index + 1}
                  </div>
                  <div className="space-y-1">
                    <CardTitle className="text-base font-bold">{step.title}</CardTitle>
                    <p className="text-xs text-muted-foreground leading-relaxed">{step.description}</p>
                  </div>
                </div>
              </CardHeader>
            </Card>
          ))}
          
          <Card className="bg-amber-500/10 border-amber-500/20">
            <CardHeader className="p-4">
              <CardTitle className="text-sm flex items-center gap-2 text-amber-500">
                <Info className="size-4" />
                Current Step Tip
              </CardTitle>
              <p className="text-xs text-amber-200/80 leading-relaxed mt-2">
                {steps[activeStep].tip}
              </p>
            </CardHeader>
          </Card>
        </div>

        {/* Right: Action Area */}
        <div className="lg:col-span-8 space-y-6">
          <Card className="border-slate-800 bg-slate-900/50 backdrop-blur-xl overflow-hidden">
            <div className="bg-blue-600 h-1.5 w-full">
              <div 
                className="bg-white h-full transition-all duration-500" 
                style={{ width: `${((activeStep + 1) / steps.length) * 100}%` }}
              />
            </div>
            <CardHeader className="pb-4">
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <CardTitle className="text-2xl font-black">{steps[activeStep].title}</CardTitle>
                  <CardDescription className="text-base">{steps[activeStep].description}</CardDescription>
                </div>
                <Badge variant="outline" className="bg-blue-500/10 text-blue-500 border-blue-500/20 font-black">
                  STEP {activeStep + 1} OF {steps.length}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="flex flex-col items-center justify-center p-12 text-center space-y-8">
              <div className="size-24 rounded-3xl bg-blue-500/10 flex items-center justify-center border border-blue-500/20">
                {(() => {
                  const Icon = steps[activeStep].icon;
                  return <Icon className="size-12 text-blue-500" />;
                })()}
              </div>
              
              <div className="max-w-md space-y-6">
                {activeStep === 0 ? (
                  <>
                    <div className="space-y-2">
                      <h3 className="text-xl font-black">Ready to open the portal?</h3>
                      <p className="text-muted-foreground">
                        Click the button below. We recommend keeping this app open in a split-screen or separate window so you can follow the steps.
                      </p>
                    </div>
                    <Button size="lg" className="w-full h-14 text-lg font-black bg-blue-600 hover:bg-blue-500 gap-3 rounded-2xl shadow-xl shadow-blue-600/20" onClick={handleOpenCfpb} disabled={loading}>
                      {loading ? <Loader2 className="size-5 animate-spin" /> : <ExternalLink className="size-5" />}
                      Open CFPB.gov Portal
                    </Button>
                  </>
                ) : activeStep === 2 ? (
                  <div className="space-y-4 w-full">
                    <h3 className="text-xl font-black">Your Auto-Populated Responses</h3>
                    <p className="text-sm text-muted-foreground">
                      Select a dispute below to copy its legal text. Then paste it into the CFPB "What happened" box.
                    </p>
                    <div className="space-y-3 text-left max-h-64 overflow-y-auto pr-2">
                      {disputes && disputes.length > 0 ? (
                        disputes.map((dispute) => (
                          <div 
                            key={dispute._id} 
                            className="p-4 rounded-2xl bg-slate-950 border border-slate-800 hover:border-blue-500/50 transition-all group cursor-pointer"
                            onClick={() => copyToClipboard(dispute.reason, dispute._id)}
                          >
                            <div className="flex items-center justify-between mb-2">
                              <span className="text-xs font-black text-blue-500 uppercase tracking-widest">{dispute.accountName}</span>
                              {copiedId === dispute._id ? <Check className="size-4 text-emerald-500" /> : <Copy className="size-4 text-slate-500 group-hover:text-blue-500" />}
                            </div>
                            <p className="text-xs text-slate-400 line-clamp-2 italic">"{dispute.reason}"</p>
                          </div>
                        ))
                      ) : (
                        <div className="p-8 rounded-2xl border-2 border-dashed border-slate-800 text-center">
                          <p className="text-sm text-slate-500">No disputes generated yet. Go to Analysis first.</p>
                          <Button variant="link" className="text-blue-500" asChild>
                            <a href="/analysis">Go to Analysis</a>
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="space-y-2">
                    <h3 className="text-xl font-black">Follow the Instructions</h3>
                    <p className="text-muted-foreground">
                      Look at the "Current Step Tip" on the left for exactly what to click on the government website.
                    </p>
                  </div>
                )}
              </div>

              <div className="flex items-center gap-4 pt-8 w-full max-w-md">
                <Button variant="outline" className="flex-1 h-12 rounded-xl border-slate-800" onClick={handlePrev} disabled={activeStep === 0}>
                  Previous
                </Button>
                <Button className="flex-1 h-12 rounded-xl bg-slate-800 hover:bg-slate-700 text-white" onClick={handleNext} disabled={activeStep === steps.length - 1}>
                  Next Step
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
