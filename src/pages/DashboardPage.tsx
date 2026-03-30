import { useQuery } from "convex/react";
import {
  BarChart3,
  Mail,
  Zap,
  ShieldCheck,
  Settings,
  Upload,
  ChevronRight,
} from "lucide-react";
import { Link } from "react-router-dom";
import { api } from "../../convex/_generated/api";

export function DashboardPage() {
  const user = useQuery(api.auth.currentUser);

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#020617', color: 'white', padding: '2rem', fontFamily: 'sans-serif' }}>
      {/* Header */}
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '3rem', maxWidth: '80rem', marginLeft: 'auto', marginRight: 'auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <div style={{ width: '2rem', height: '2rem', backgroundColor: 'rgba(52, 211, 153, 0.2)', borderRadius: '0.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <Zap style={{ width: '1.25rem', height: '1.25rem', color: '#34d399' }} />
          </div>
          <h1 style={{ fontSize: '1.875rem', fontWeight: 'bold', background: 'linear-gradient(to right, #60a5fa, #34d399)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
            CreditRepairAI v2.0
          </h1>
        </div>
        <Link 
          to="/dashboard"
          style={{ paddingLeft: '1.5rem', paddingRight: '1.5rem', paddingTop: '0.5rem', paddingBottom: '0.5rem', backgroundColor: '#2563eb', borderRadius: '9999px', color: 'white', fontWeight: 'bold', textDecoration: 'none', fontSize: '0.875rem' }}
        >
          Dashboard
        </Link>
      </header>

      {/* Visual Dashboard */}
      <section style={{ maxWidth: '80rem', marginLeft: 'auto', marginRight: 'auto', backgroundColor: 'rgba(15, 23, 42, 0.3)', border: '1px solid rgba(30, 41, 59, 0.5)', borderRadius: '3rem', padding: '2.5rem', position: 'relative', overflow: 'hidden' }}>
        <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
          <h3 style={{ fontSize: '1.5rem', fontWeight: 'bold', color: '#cbd5e1', marginBottom: '0.25rem' }}>
            {user?.name || "CreditRepairAI User"}
          </h3>
          <p style={{ fontSize: '0.75rem', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.2em' }}>AI Progress Meter</p>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '2rem', alignItems: 'center' }}>
          {/* Left Card */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ padding: '2rem', backgroundColor: 'rgba(30, 41, 59, 0.4)', border: '1px solid rgba(51, 65, 85, 0.5)', borderRadius: '2rem' }}>
              <h4 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '2rem' }}>How It Works</h4>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
                {[
                  { icon: Upload, title: "Upload", desc: "Upload your credit assets.", href: "/upload" },
                  { icon: BarChart3, title: "Analysis", desc: "Analyse your credit.", href: "/analysis" },
                  { icon: Mail, title: "Dispute", desc: "Start your dispute process.", href: "/disputes" }
                ].map((item, i) => (
                  <Link key={i} to={item.href} style={{ display: 'flex', gap: '1rem', textDecoration: 'none', color: 'inherit' }}>
                    <div style={{ width: '2.5rem', height: '2.5rem', borderRadius: '0.75rem', backgroundColor: 'rgba(51, 65, 85, 0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
                      <item.icon style={{ width: '1.25rem', height: '1.25rem', color: '#cbd5e1' }} />
                    </div>
                    <div>
                      <p style={{ fontWeight: 'bold', fontSize: '0.875rem', margin: 0 }}>{item.title}</p>
                      <p style={{ fontSize: '0.625rem', color: '#64748b', margin: 0 }}>{item.desc}</p>
                    </div>
                  </Link>
                ))}
              </div>
            </div>
            <Link to="/analysis" style={{ width: '100%', padding: '1.5rem', borderRadius: '1rem', background: 'linear-gradient(to right, #3b82f6, #34d399)', color: 'white', fontWeight: 'bold', textAlign: 'center', textDecoration: 'none', border: 'none' }}>
              Start Dispute Process
            </Link>
          </div>

          {/* Center Meter */}
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ width: '20rem', height: '20rem', borderRadius: '50%', border: '16px solid #1e293b', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', position: 'relative' }}>
              <div style={{ position: 'absolute', inset: '-16px', borderRadius: '50%', border: '16px solid transparent', borderTopColor: '#34d399', borderRightColor: '#34d399', transform: 'rotate(45deg)' }} />
              <Zap style={{ width: '2.5rem', height: '2.5rem', color: '#34d399', marginBottom: '1rem' }} />
              <p style={{ fontSize: '0.75rem', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.1em', margin: 0 }}>Credit Score</p>
              <p style={{ fontSize: '4.5rem', fontWeight: 900, letterSpacing: '-0.05em', margin: 0 }}>618</p>
              <p style={{ fontSize: '0.625rem', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.1em', marginTop: '0.5rem', margin: 0 }}>Equifax Analysis</p>
            </div>
            <Link to="/analysis" style={{ marginTop: '3rem', padding: '1.5rem 3rem', borderRadius: '1rem', background: 'linear-gradient(to right, #3b82f6, #34d399)', color: 'white', fontWeight: 'bold', textDecoration: 'none' }}>
              Start Dispute Process
            </Link>
          </div>

          {/* Right Cards */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {[
              { name: "TransUnion", score: "618", bureau: "76" },
              { name: "Experian", score: "603", bureau: "72" }
            ].map((bureau, i) => (
              <div key={i} style={{ padding: '1.5rem', backgroundColor: 'rgba(30, 41, 59, 0.4)', border: '1px solid rgba(51, 65, 85, 0.5)', borderRadius: '2rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                  <span style={{ fontWeight: 'bold', color: '#cbd5e1' }}>{bureau.name}</span>
                  <ChevronRight style={{ width: '1rem', height: '1rem', color: '#64748b' }} />
                </div>
                <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
                  <div>
                    <p style={{ fontSize: '1.875rem', fontWeight: 900, margin: 0 }}>{bureau.score}</p>
                    <p style={{ fontSize: '0.5rem', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.1em', margin: 0 }}>Credit Score</p>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ width: '3rem', height: '3rem', borderRadius: '50%', border: '4px solid #334155', borderTopColor: '#34d399', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '0.25rem' }}>
                      <span style={{ fontSize: '0.75rem', fontWeight: 'bold' }}>{bureau.bureau}</span>
                    </div>
                    <p style={{ fontSize: '0.5rem', fontWeight: 'bold', color: '#64748b', textTransform: 'uppercase', letterSpacing: '0.1em', margin: 0 }}>Bureau Score</p>
                  </div>
                </div>
              </div>
            ))}
            <Link to="/upload" style={{ width: '100%', padding: '1.5rem', borderRadius: '1rem', border: '1px solid #334155', backgroundColor: 'rgba(30, 41, 59, 0.5)', color: '#cbd5e1', fontWeight: 'bold', textAlign: 'center', textDecoration: 'none' }}>
              View Reports
            </Link>
          </div>
        </div>
      </section>

      {/* Quick Actions */}
      <div style={{ maxWidth: '80rem', marginLeft: 'auto', marginRight: 'auto', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1.5rem', marginTop: '2.5rem' }}>
        {[
          { label: "CFPB Assistant", href: "/cfpb", icon: Zap },
          { label: "Bureau Freezes", href: "/freezes", icon: ShieldCheck },
          { label: "AI Analysis", href: "/analysis", icon: BarChart3 },
          { label: "Settings", href: "/settings", icon: Settings }
        ].map((action, i) => (
          <Link
            key={i}
            to={action.href}
            style={{ display: 'flex', alignItems: 'center', gap: '1rem', padding: '1.5rem', borderRadius: '1rem', border: '1px solid #1e293b', backgroundColor: 'rgba(15, 23, 42, 0.5)', textDecoration: 'none', color: 'inherit' }}
          >
            <div style={{ borderRadius: '0.75rem', backgroundColor: '#1e293b', padding: '0.75rem' }}>
              <action.icon style={{ width: '1.25rem', height: '1.25rem', color: '#cbd5e1' }} />
            </div>
            <span style={{ fontWeight: 'bold', color: '#e2e8f0' }}>{action.label}</span>
          </Link>
        ))}
      </div>
    </div>
  );
}
