import { useState } from "react";
import { Globe, ExternalLink, Info, CheckCircle2, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";

const steps = [
  {
    title: "Visit CFPB.gov",
    description: "Go to the official Consumer Financial Protection Bureau website.",
    url: "https://www.consumerfinance.gov/complaint/",
    icon: Globe
  },
  {
    title: "Select 'Credit reporting'",
    description: "Choose the 'Credit reporting, credit repair services, or other personal consumer reports' option.",
    icon: Info
  },
  {
    title: "Enter Dispute Details",
    description: "Use the AI-generated dispute reasons and USC codes from your dashboard.",
    icon: CheckCircle2
  },
  {
    title: "Submit Complaint",
    description: "Review and submit your complaint to the CFPB for investigation.",
    icon: AlertCircle
  }
];

export function CfpbPage() {
  const [activeStep, setActiveStep] = useState(0);
  const [loading, setLoading] = useState(false);

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
    toast.success("CFPB website opened in a new tab!");
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight flex items-center gap-2">
          <Globe className="size-8 text-primary" />
          CFPB Dispute Assistant
        </h1>
        <p className="text-muted-foreground">Follow these steps to file a formal complaint with the CFPB for maximum results.</p>
      </div>

      <div className="grid gap-8 lg:grid-cols-3">
        <div className="lg:col-span-1 space-y-4">
          {steps.map((step, index) => (
            <Card 
              key={index} 
              className={`cursor-pointer transition-all ${
                activeStep === index ? "border-primary bg-primary/5" : "opacity-60 hover:opacity-100"
              }`}
              onClick={() => setActiveStep(index)}
            >
              <CardHeader className="p-4">
                <div className="flex items-center gap-3">
                  <div className={`size-8 rounded-full flex items-center justify-center text-sm font-bold ${
                    activeStep === index ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
                  }`}>
                    {index + 1}
                  </div>
                  <CardTitle className="text-base">{step.title}</CardTitle>
                </div>
              </CardHeader>
            </Card>
          ))}
        </div>

        <div className="lg:col-span-2">
          <Card className="h-full flex flex-col">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="space-y-1">
                  <CardTitle className="text-2xl">{steps[activeStep].title}</CardTitle>
                  <CardDescription>{steps[activeStep].description}</CardDescription>
                </div>
                <Badge variant="outline" className="bg-primary/10 text-primary border-primary/20">
                  Step {activeStep + 1} of {steps.length}
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="flex-1 flex flex-col items-center justify-center p-12 text-center space-y-8">
              <div className="size-24 rounded-full bg-primary/10 flex items-center justify-center">
                {(() => {
                  const Icon = steps[activeStep].icon;
                  return <Icon className="size-12 text-primary" />;
                })()}
              </div>
              
              <div className="max-w-md space-y-4">
                <h3 className="text-xl font-semibold">Ready to proceed?</h3>
                <p className="text-muted-foreground">
                  {activeStep === 0 
                    ? "Click the button below to open the CFPB complaint portal. We'll guide you through the rest of the process."
                    : "Follow the instructions on the CFPB website and use the data from your AI Analysis page."}
                </p>
                
                {activeStep === 0 && (
                  <Button size="lg" className="w-full gap-2" onClick={handleOpenCfpb} disabled={loading}>
                    {loading ? <Loader2 className="size-4 animate-spin" /> : <ExternalLink className="size-4" />}
                    Open CFPB.gov
                  </Button>
                )}
              </div>

              <div className="flex items-center gap-4 pt-8">
                <Button variant="outline" onClick={handlePrev} disabled={activeStep === 0}>
                  Previous Step
                </Button>
                <Button onClick={handleNext} disabled={activeStep === steps.length - 1}>
                  Next Step
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      </div>

      <Card className="bg-chart-2/5 border-chart-2/20">
        <CardHeader>
          <CardTitle className="text-lg flex items-center gap-2">
            <Info className="size-5 text-chart-2" />
            Pro Tip: Use USC Codes
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            When filing your complaint, always reference the specific U.S. Code (USC) sections provided in your AI Analysis. This forces the bureaus to respond to specific legal requirements rather than just "verifying" the account.
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
