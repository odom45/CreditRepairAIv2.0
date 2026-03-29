import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation } from "convex/react";
import { Upload, FileText, X, CheckCircle2, AlertCircle, Loader2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { api } from "../../convex/_generated/api";
import { toast } from "sonner";

interface FileItem {
  name: string;
  size: number;
  bureau: string;
}

export function UploadPage() {
  const [files, setFiles] = useState<FileItem[]>([]);
  const [analyzing, setAnalyzing] = useState(false);
  const navigate = useNavigate();
  const saveReport = useMutation(api.reports.saveReport);

  const detectBureau = (filename: string) => {
    const name = filename.toLowerCase();
    if (name.includes("transunion")) return "TransUnion";
    if (name.includes("experian")) return "Experian";
    if (name.includes("equifax")) return "Equifax";
    return "Unknown";
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const newFiles = Array.from(e.target.files).map(file => ({
        name: file.name,
        size: file.size,
        bureau: detectBureau(file.name)
      }));
      setFiles([...files, ...newFiles]);
    }
  };

  const removeFile = (index: number) => {
    const newFiles = [...files];
    newFiles.splice(index, 1);
    setFiles(newFiles);
  };

  const startAnalysis = async () => {
    setAnalyzing(true);
    try {
      // Simulate saving reports to Convex
      for (const file of files) {
        await saveReport({
          bureau: file.bureau.toLowerCase() as any,
          accounts: [
            {
              accountName: "SAMPLE CREDITOR",
              accountNumber: "XXXX1234",
              balance: 100,
              status: "Open",
              bureau: file.bureau.toLowerCase()
            }
          ]
        });
      }
      
      toast.success("Reports uploaded and saved!");
      setTimeout(() => {
        setAnalyzing(false);
        navigate("/analysis");
      }, 2000);
    } catch (error) {
      console.error(error);
      toast.error("Failed to save reports");
      setAnalyzing(false);
    }
  };

  const getBureauColor = (bureau: string) => {
    if (bureau === "TransUnion") return "bg-blue-500/10 text-blue-500 border-blue-500/20";
    if (bureau === "Experian") return "bg-purple-500/10 text-purple-500 border-purple-500/20";
    if (bureau === "Equifax") return "bg-red-500/10 text-red-500 border-red-500/20";
    return "bg-slate-500/10 text-slate-500 border-slate-500/20";
  };

  return (
    <div className="max-w-4xl mx-auto space-y-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">🤖 CreditRepairAI v2.0</h1>
        <p className="text-muted-foreground">AI-powered 3-bureau credit report analysis</p>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <Card className="bg-primary/5 border-primary/20">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <CheckCircle2 className="size-5 text-primary" />
              Quick Start
            </CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-2 text-muted-foreground">
            <p>• Download free reports from AnnualCreditReport.com</p>
            <p>• Upload all 3 bureaus (TransUnion, Experian, Equifax)</p>
            <p>• AI automatically detects errors & mismatches</p>
          </CardContent>
        </Card>

        <Card className="bg-chart-3/5 border-chart-3/20">
          <CardHeader>
            <CardTitle className="text-lg flex items-center gap-2">
              <AlertCircle className="size-5 text-chart-3" />
              AI Analysis
            </CardTitle>
          </CardHeader>
          <CardContent className="text-sm space-y-2 text-muted-foreground">
            <p>Our AI engine scans for inconsistent balances, status mismatches, and missing accounts across all three bureaus.</p>
          </CardContent>
        </Card>
      </div>

      <div 
        className="border-2 border-dashed border-muted-foreground/25 rounded-xl p-12 flex flex-col items-center justify-center bg-muted/30 hover:bg-muted/50 transition-colors cursor-pointer relative"
        onClick={() => document.getElementById("file-upload")?.click()}
      >
        <input 
          id="file-upload" 
          type="file" 
          multiple 
          accept=".pdf" 
          className="hidden" 
          onChange={handleFileChange}
        />
        <div className="size-16 rounded-full bg-primary/10 flex items-center justify-center mb-4">
          <Upload className="size-8 text-primary" />
        </div>
        <h3 className="text-xl font-semibold">Upload Credit Reports</h3>
        <p className="text-muted-foreground mt-1">Click to select PDF files</p>
      </div>

      {files.length > 0 && (
        <div className="space-y-4">
          <h3 className="font-semibold flex items-center gap-2">
            <FileText className="size-4" />
            Selected Files ({files.length})
          </h3>
          <div className="grid gap-3">
            {files.map((file, index) => (
              <div key={index} className="flex items-center justify-between p-4 rounded-lg border bg-card">
                <div className="flex items-center gap-4">
                  <div className="size-10 rounded bg-muted flex items-center justify-center font-bold text-primary">
                    {file.bureau[0]}
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="font-medium">{file.name}</span>
                      <Badge variant="outline" className={getBureauColor(file.bureau)}>
                        {file.bureau}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                  </div>
                </div>
                <Button variant="ghost" size="icon" onClick={(e) => { e.stopPropagation(); removeFile(index); }}>
                  <X className="size-4 text-destructive" />
                </Button>
              </div>
            ))}
          </div>
        </div>
      )}

      {files.length >= 1 && (
        <Button 
          className="w-full h-12 text-lg font-semibold" 
          onClick={startAnalysis} 
          disabled={analyzing}
        >
          {analyzing ? (
            <>
              <Loader2 className="mr-2 h-5 w-5 animate-spin" />
              AI is analyzing your reports...
            </>
          ) : (
            "🔍 Start AI Analysis"
          )}
        </Button>
      )}
    </div>
  );
}
