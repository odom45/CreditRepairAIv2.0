import { useQuery } from "convex/react";
import { ArrowRight, BarChart3, CheckCircle2, Mail, Sparkles, Upload, ShieldCheck, Zap, ChevronRight } from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { api } from "../../convex/_generated/api";

export function LandingPage() {
  const user = useQuery(api.auth.currentUser);

  return (
    <div className="min-h-screen bg-slate-950 text-white font-sans selection:bg-blue-500/30 p-8">
      {/* Header */}
      <header className="flex justify-between items-center mb-12 max-w-7xl mx-auto">
        <div className="flex items-center gap-2">
          <div className="size-8 bg-emerald-400/20 rounded-lg flex items-center justify-center">
            <Zap className="size-5 text-emerald-400 fill-emerald-400" />
          </div>
          <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 to-emerald-400 bg-clip-text text-transparent">
            CreditRepairAI v2.0
          </h1>
        </div>
        <Button 
          className="px-6 py-2 bg-blue-600 hover:bg-blue-500 rounded-full transition-all shadow-lg shadow-blue-500/20 border-none h-auto text-sm font-bold"
          asChild
        >
          <Link to={user ? "/dashboard" : "/login"}>
            {user ? "Dashboard" : "Sign In"}
          </Link>
        </Button>
      </header>

      {/* Hero Section - Glassmorphism */}
      <section className="max-w-7xl mx-auto bg-slate-900/50 border border-slate-800 rounded-[2.5rem] p-10 md:p-16 mb-10 backdrop-blur-md relative overflow-hidden">
        <div className="absolute top-0 right-0 w-96 h-96 bg-blue-500/10 blur-[120px] -z-10" />
        
        <div className="relative z-10">
          <h2 className="text-5xl md:text-6xl font-extrabold mb-6 tracking-tight">
            Fix Your Credit with AI
          </h2>
          <p className="text-slate-400 text-xl mb-10 max-w-2xl leading-relaxed">
            Upload your 3-bureau credit reports and let our AI engine detect errors, mismatches, and missing accounts automatically.
          </p>
          <div className="flex flex-wrap gap-4">
            <Button 
              className="bg-white text-black px-8 py-4 rounded-2xl font-bold hover:bg-slate-200 transition-all text-lg h-auto"
              asChild
            >
              <Link to={user ? "/upload" : "/signup"}>
                Upload Reports
              </Link>
            </Button>
            <Button 
              variant="outline" 
              className="border-slate-700 bg-transparent px-8 py-4 rounded-2xl font-bold hover:bg-slate-800 transition-all text-lg text-white h-auto"
            >
              Learn More
            </Button>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-6">
        {[
          {
            step: "1",
            title: "Upload Reports",
            desc: "Securely upload PDF reports from TransUnion, Experian, and Equifax.",
            color: "blue"
          },
          {
            step: "2",
            title: "AI Analysis",
            desc: "Our AI engine scans for inconsistencies and maps them to USC legal codes.",
            color: "emerald"
          },
          {
            step: "3",
            title: "Generate Disputes",
            desc: "Create professional dispute letters backed by federal law in one click.",
            color: "indigo"
          }
        ].map((feature, i) => (
          <div 
            key={i} 
            className="p-8 bg-slate-900 border border-slate-800 rounded-3xl hover:border-blue-500/50 transition-all duration-300 group"
          >
            <div className="w-12 h-12 bg-blue-500/10 rounded-xl flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <span className="text-blue-400 text-xl font-bold">{feature.step}</span>
            </div>
            <h3 className="text-xl font-bold mb-3">{feature.title}</h3>
            <p className="text-slate-400 text-sm leading-relaxed">{feature.desc}</p>
          </div>
        ))}
      </div>

      {/* Visual Dashboard Preview (Matching the Screenshot) */}
      <section className="max-w-7xl mx-auto mt-20 p-10 bg-slate-900/30 border border-slate-800/50 rounded-[3rem] backdrop-blur-xl relative overflow-hidden">
        <div className="absolute -bottom-20 -left-20 w-80 h-80 bg-emerald-500/10 blur-[100px]" />
        
        <div className="text-center mb-12">
          <h3 className="text-2xl font-bold text-slate-300 mb-1">Nano Banana</h3>
          <p className="text-xs font-bold text-slate-500 uppercase tracking-[0.2em]">AI Progress Meter</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Left Card */}
          <div className="lg:col-span-3 space-y-4">
            <div className="p-8 bg-slate-800/40 border border-slate-700/50 rounded-[2rem] backdrop-blur-md">
              <h4 className="text-xl font-bold mb-8">How It Works</h4>
              <div className="space-y-6">
                {[
                  { icon: Upload, title: "Upload", desc: "Upload your from and from credit assets." },
                  { icon: BarChart3, title: "Analysis", desc: "Analyse and plant your credit analysis." },
                  { icon: Mail, title: "Dispute", desc: "Dispute your credit your dispute process." }
                ].map((item, i) => (
                  <div key={i} className="flex gap-4">
                    <div className="size-10 rounded-xl bg-slate-700/50 flex items-center justify-center shrink-0">
                      <item.icon className="size-5 text-slate-300" />
                    </div>
                    <div>
                      <p className="font-bold text-sm">{item.title}</p>
                      <p className="text-[10px] text-slate-500 leading-tight">{item.desc}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
            <Button className="w-full py-6 rounded-2xl bg-gradient-to-r from-blue-500 to-emerald-400 text-white font-bold shadow-lg shadow-emerald-500/20 border-none">
              Start Dispute Process
            </Button>
          </div>

          {/* Center Meter */}
          <div className="lg:col-span-6 flex flex-col items-center justify-center relative">
            <div className="size-80 rounded-full border-[16px] border-slate-800 flex flex-col items-center justify-center relative">
              <div className="absolute inset-0 rounded-full border-[16px] border-t-emerald-400 border-r-emerald-400 border-b-transparent border-l-transparent rotate-45 shadow-[0_0_30px_rgba(52,211,153,0.3)]" />
              <Zap className="size-10 text-emerald-400 fill-emerald-400 mb-4" />
              <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">Credit Score</p>
              <p className="text-7xl font-black tracking-tighter">618</p>
              <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mt-2">Equifax Analysis</p>
            </div>
            <Button className="mt-12 px-12 py-6 rounded-2xl bg-gradient-to-r from-blue-500 to-emerald-400 text-white font-bold shadow-lg shadow-emerald-500/20 border-none">
              Start Dispute Process
            </Button>
          </div>

          {/* Right Cards */}
          <div className="lg:col-span-3 space-y-4">
            {[
              { name: "TransUnion", score: "618", bureau: "76" },
              { name: "Experian", score: "603", bureau: "72" }
            ].map((bureau, i) => (
              <div key={i} className="p-6 bg-slate-800/40 border border-slate-700/50 rounded-[2rem] backdrop-blur-md relative group cursor-pointer">
                <div className="flex justify-between items-center mb-4">
                  <span className="font-bold text-slate-300">{bureau.name}</span>
                  <ChevronRight className="size-4 text-slate-500 group-hover:text-white transition-colors" />
                </div>
                <div className="flex items-end justify-between">
                  <div>
                    <p className="text-3xl font-black">{bureau.score}</p>
                    <p className="text-[8px] font-bold text-slate-500 uppercase tracking-widest">Credit Score</p>
                  </div>
                  <div className="text-right">
                    <div className="size-12 rounded-full border-4 border-slate-700 border-t-emerald-400 flex items-center justify-center mb-1">
                      <span className="text-xs font-bold">{bureau.bureau}</span>
                    </div>
                    <p className="text-[8px] font-bold text-slate-500 uppercase tracking-widest">Bureau Score</p>
                  </div>
                </div>
              </div>
            ))}
            <Button variant="outline" className="w-full py-6 rounded-2xl border-slate-700 bg-slate-800/50 text-slate-300 font-bold hover:bg-slate-800">
              View Reports
            </Button>
          </div>
        </div>
      </section>
    </div>
  );
}
