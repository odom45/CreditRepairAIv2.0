import { useQuery } from "convex/react";
import {
  ArrowUpRight,
  Settings,
  Sparkles,
  TrendingUp,
  Upload,
  BarChart3,
  Mail,
  CheckCircle2,
  AlertTriangle,
  Zap,
  ShieldCheck,
  Clock,
  ChevronRight,
} from "lucide-react";
import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { api } from "../../convex/_generated/api";

export function DashboardPage() {
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
          <Link to="/dashboard">Dashboard</Link>
        </Button>
      </header>

      {/* Visual Dashboard (Matching the Screenshot) */}
      <section className="max-w-7xl mx-auto p-10 bg-slate-900/30 border border-slate-800/50 rounded-[3rem] backdrop-blur-xl relative overflow-hidden">
        <div className="absolute -bottom-20 -left-20 w-80 h-80 bg-emerald-500/10 blur-[100px]" />
        
        <div className="text-center mb-12">
          <h3 className="text-2xl font-bold text-slate-300 mb-1">
            {user?.name || "Nano Banana"}
          </h3>
          <p className="text-xs font-bold text-slate-500 uppercase tracking-[0.2em]">AI Progress Meter</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-center">
          {/* Left Card: How It Works */}
          <div className="lg:col-span-3 space-y-4">
            <div className="p-8 bg-slate-800/40 border border-slate-700/50 rounded-[2rem] backdrop-blur-md">
              <h4 className="text-xl font-bold mb-8">How It Works</h4>
              <div className="space-y-6">
                {[
                  { icon: Upload, title: "Upload", desc: "Upload your from and from credit assets.", href: "/upload" },
                  { icon: BarChart3, title: "Analysis", desc: "Analyse and plant your credit analysis.", href: "/analysis" },
                  { icon: Mail, title: "Dispute", desc: "Dispute your credit your dispute process.", href: "/disputes" }
                ].map((item, i) => (
                  <Link key={i} to={item.href} className="flex gap-4 group">
                    <div className="size-10 rounded-xl bg-slate-700/50 flex items-center justify-center shrink-0 group-hover:bg-blue-500/20 transition-colors">
                      <item.icon className="size-5 text-slate-300 group-hover:text-blue-400 transition-colors" />
                    </div>
                    <div>
                      <p className="font-bold text-sm group-hover:text-white transition-colors">{item.title}</p>
                      <p className="text-[10px] text-slate-500 leading-tight">{item.desc}</p>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
            <Button 
              className="w-full py-6 rounded-2xl bg-gradient-to-r from-blue-500 to-emerald-400 text-white font-bold shadow-lg shadow-emerald-500/20 border-none"
              asChild
            >
              <Link to="/analysis">Start Dispute Process</Link>
            </Button>
          </div>

          {/* Center Meter: Credit Score */}
          <div className="lg:col-span-6 flex flex-col items-center justify-center relative">
            <div className="size-80 rounded-full border-[16px] border-slate-800 flex flex-col items-center justify-center relative">
              <div className="absolute inset-0 rounded-full border-[16px] border-t-emerald-400 border-r-emerald-400 border-b-transparent border-l-transparent rotate-45 shadow-[0_0_30px_rgba(52,211,153,0.3)]" />
              <Zap className="size-10 text-emerald-400 fill-emerald-400 mb-4" />
              <p className="text-xs font-bold text-slate-500 uppercase tracking-widest">Credit Score</p>
              <p className="text-7xl font-black tracking-tighter">618</p>
              <p className="text-[10px] font-bold text-slate-500 uppercase tracking-widest mt-2">Equifax Analysis</p>
            </div>
            <Button 
              className="mt-12 px-12 py-6 rounded-2xl bg-gradient-to-r from-blue-500 to-emerald-400 text-white font-bold shadow-lg shadow-emerald-500/20 border-none"
              asChild
            >
              <Link to="/analysis">Start Dispute Process</Link>
            </Button>
          </div>

          {/* Right Cards: Bureau Scores */}
          <div className="lg:col-span-3 space-y-4">
            {[
              { name: "TransUnion", score: "618", bureau: "76" },
              { name: "Experian", score: "603", bureau: "72" }
            ].map((bureau, i) => (
              <div key={i} className="p-6 bg-slate-800/40 border border-slate-700/50 rounded-[2rem] backdrop-blur-md relative group cursor-pointer hover:border-blue-500/50 transition-all">
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
            <Button 
              variant="outline" 
              className="w-full py-6 rounded-2xl border-slate-700 bg-slate-800/50 text-slate-300 font-bold hover:bg-slate-800"
              asChild
            >
              <Link to="/upload">View Reports</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Quick Actions Grid */}
      <div className="max-w-7xl mx-auto grid grid-cols-1 md:grid-cols-4 gap-6 mt-10">
        {[
          { label: "CFPB Assistant", href: "/cfpb", icon: Zap, color: "blue" },
          { label: "Bureau Freezes", href: "/freezes", icon: ShieldCheck, color: "emerald" },
          { label: "AI Analysis", href: "/analysis", icon: BarChart3, color: "indigo" },
          { label: "Settings", href: "/settings", icon: Settings, color: "slate" }
        ].map((action, i) => (
          <Button
            key={i}
            variant="outline"
            className="h-auto py-6 px-6 rounded-2xl border-slate-800 bg-slate-900/50 hover:bg-slate-800 hover:border-blue-500/50 group transition-all duration-300 justify-start"
            asChild
          >
            <Link to={action.href}>
              <div className="flex items-center gap-4">
                <div className="rounded-xl bg-slate-800 p-3 transition-colors group-hover:bg-blue-500/20">
                  <action.icon className="size-5 text-slate-300 transition-colors group-hover:text-blue-400" />
                </div>
                <span className="font-bold text-slate-200 group-hover:text-white">{action.label}</span>
              </div>
            </Link>
          </Button>
        ))}
      </div>
    </div>
  );
}
