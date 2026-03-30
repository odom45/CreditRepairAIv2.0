import { useQuery } from "convex/react";
import { Zap } from "lucide-react";
import { Link } from "react-router-dom";
import { api } from "../../convex/_generated/api";

export function LandingPage() {
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
          to={user ? "/dashboard" : "/login"}
          style={{ paddingLeft: '1.5rem', paddingRight: '1.5rem', paddingTop: '0.5rem', paddingBottom: '0.5rem', backgroundColor: '#2563eb', borderRadius: '9999px', color: 'white', fontWeight: 'bold', textDecoration: 'none', fontSize: '0.875rem' }}
        >
          {user ? "Dashboard" : "Sign In"}
        </Link>
      </header>

      {/* Hero Section */}
      <section style={{ maxWidth: '80rem', marginLeft: 'auto', marginRight: 'auto', backgroundColor: 'rgba(15, 23, 42, 0.5)', border: '1px solid #1e293b', borderRadius: '2.5rem', padding: '4rem', marginBottom: '2.5rem', position: 'relative', overflow: 'hidden' }}>
        <div style={{ position: 'relative', zIndex: 10 }}>
          <h2 style={{ fontSize: '3.75rem', fontWeight: 800, marginBottom: '1.5rem', letterSpacing: '-0.025em' }}>
            Fix Your Credit with AI
          </h2>
          <p style={{ color: '#94a3b8', fontSize: '1.25rem', marginBottom: '2.5rem', maxWidth: '42rem', lineHeight: 1.625 }}>
            Upload your 3-bureau credit reports and let our AI engine detect errors, mismatches, and missing accounts automatically.
          </p>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
            <Link 
              to={user ? "/upload" : "/signup"}
              style={{ backgroundColor: 'white', color: 'black', paddingLeft: '2rem', paddingRight: '2rem', paddingTop: '1rem', paddingBottom: '1rem', borderRadius: '1rem', fontWeight: 'bold', fontSize: '1.125rem', textDecoration: 'none' }}
            >
              Upload Reports
            </Link>
            <button 
              style={{ border: '1px solid #334155', backgroundColor: 'transparent', color: 'white', paddingLeft: '2rem', paddingRight: '2rem', paddingTop: '1rem', paddingBottom: '1rem', borderRadius: '1rem', fontWeight: 'bold', fontSize: '1.125rem', cursor: 'pointer' }}
            >
              Learn More
            </button>
          </div>
        </div>
      </section>

      {/* Features Grid */}
      <div style={{ maxWidth: '80rem', marginLeft: 'auto', marginRight: 'auto', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
        {[
          {
            step: "1",
            title: "Upload Reports",
            desc: "Securely upload PDF reports from TransUnion, Experian, and Equifax."
          },
          {
            step: "2",
            title: "AI Analysis",
            desc: "Our AI engine scans for inconsistencies and maps them to USC legal codes."
          },
          {
            step: "3",
            title: "Generate Disputes",
            desc: "Create professional dispute letters backed by federal law in one click."
          }
        ].map((feature, i) => (
          <div 
            key={i} 
            style={{ padding: '2rem', backgroundColor: '#0f172a', border: '1px solid #1e293b', borderRadius: '1.5rem' }}
          >
            <div style={{ width: '3rem', height: '3rem', backgroundColor: 'rgba(59, 130, 246, 0.1)', borderRadius: '0.75rem', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem' }}>
              <span style={{ color: '#60a5fa', fontSize: '1.25rem', fontWeight: 'bold' }}>{feature.step}</span>
            </div>
            <h3 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '0.75rem' }}>{feature.title}</h3>
            <p style={{ color: '#94a3b8', fontSize: '0.875rem', lineHeight: 1.625 }}>{feature.desc}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
