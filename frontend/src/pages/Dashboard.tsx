import { Leaf, Zap, Flame, ShoppingBag, PlaneTakeoff, UtensilsCrossed, Activity } from 'lucide-react'
import { ActivityCategory, ActivitySource } from '@/types/activity'

/**
 * Placeholder Dashboard page.
 *
 * This is an architectural shell — no real data is fetched yet.
 * Data integration will be added when the backend pipeline is complete.
 */
export default function Dashboard() {
  return (
    <div className="min-h-screen bg-[#0f172a] text-white">

      {/* ── Navigation ─────────────────────────────────────────────────── */}
      <nav className="border-b border-slate-800 px-6 py-4 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-brand-600 flex items-center justify-center">
            <Leaf className="w-5 h-5 text-white" />
          </div>
          <span className="font-semibold text-lg tracking-tight">Carbon Intelligence</span>
        </div>
        <div className="flex items-center gap-4">
          <span className="text-sm text-slate-400">Phase 1 — Architecture Ready</span>
          <div className="w-8 h-8 rounded-full bg-slate-700 flex items-center justify-center">
            <span className="text-xs font-medium">U</span>
          </div>
        </div>
      </nav>

      <main className="max-w-7xl mx-auto px-6 py-8 space-y-8">

        {/* ── Header ───────────────────────────────────────────────────── */}
        <div className="animate-fade-in">
          <h1 className="text-3xl font-bold tracking-tight">Carbon Dashboard</h1>
          <p className="text-slate-400 mt-1">
            AI-powered insights from your receipts, emails, and transactions.
          </p>
        </div>

        {/* ── Summary Cards ────────────────────────────────────────────── */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 animate-slide-up">
          <StatCard
            label="Total CO₂ this month"
            value="—"
            unit="kg CO₂e"
            icon={<Activity className="w-5 h-5" />}
            color="text-brand-400"
            bg="bg-brand-900/30"
          />
          <StatCard
            label="Activities tracked"
            value="—"
            unit="activities"
            icon={<Zap className="w-5 h-5" />}
            color="text-yellow-400"
            bg="bg-yellow-900/20"
          />
          <StatCard
            label="Top category"
            value="—"
            unit=""
            icon={<Flame className="w-5 h-5" />}
            color="text-orange-400"
            bg="bg-orange-900/20"
          />
          <StatCard
            label="Data sources"
            value="3"
            unit="configured"
            icon={<ShoppingBag className="w-5 h-5" />}
            color="text-purple-400"
            bg="bg-purple-900/20"
          />
        </div>

        {/* ── Pipeline Status ───────────────────────────────────────────── */}
        <section className="card animate-slide-up">
          <h2 className="text-lg font-semibold mb-4">Ingestion Pipeline Status</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <PipelineCard
              source={ActivitySource.RECEIPT}
              label="Receipt OCR"
              description="Upload electricity bills, fuel receipts, flight tickets"
              phase="Phase 1"
              status="pending"
            />
            <PipelineCard
              source={ActivitySource.GMAIL}
              label="Gmail Sync"
              description="Auto-detect bills and receipts from your inbox"
              phase="Phase 2"
              status="future"
            />
            <PipelineCard
              source={ActivitySource.SMS}
              label="SMS (Android)"
              description="Read transactions from SMS via companion app"
              phase="Future"
              status="future"
            />
          </div>
        </section>

        {/* ── Category Breakdown ────────────────────────────────────────── */}
        <section className="card animate-slide-up">
          <h2 className="text-lg font-semibold mb-4">Category Breakdown</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-3">
            {[
              { cat: ActivityCategory.ELECTRICITY, icon: <Zap className="w-4 h-4" />, color: 'text-yellow-400' },
              { cat: ActivityCategory.FUEL,        icon: <Flame className="w-4 h-4" />, color: 'text-orange-400' },
              { cat: ActivityCategory.FLIGHT,      icon: <PlaneTakeoff className="w-4 h-4" />, color: 'text-blue-400' },
              { cat: ActivityCategory.FOOD,        icon: <UtensilsCrossed className="w-4 h-4" />, color: 'text-green-400' },
              { cat: ActivityCategory.SHOPPING,    icon: <ShoppingBag className="w-4 h-4" />, color: 'text-purple-400' },
            ].map(({ cat, icon, color }) => (
              <div key={cat}
                className="bg-slate-800/50 border border-slate-700 rounded-xl p-4 flex flex-col gap-2
                           hover:border-slate-600 transition-colors duration-200">
                <div className={`${color}`}>{icon}</div>
                <div className="text-xs font-medium text-slate-300 capitalize">
                  {cat.toLowerCase()}
                </div>
                <div className="text-xl font-bold">—</div>
                <div className="text-xs text-slate-500">kg CO₂e</div>
              </div>
            ))}
          </div>
        </section>

        {/* ── Architecture Notice ───────────────────────────────────────── */}
        <div className="rounded-xl border border-brand-800 bg-brand-900/20 p-5 flex gap-3 animate-fade-in">
          <Leaf className="w-5 h-5 text-brand-400 flex-shrink-0 mt-0.5" />
          <div>
            <p className="text-sm font-medium text-brand-300">Architecture Initialized</p>
            <p className="text-sm text-slate-400 mt-1">
              The ingestion pipeline, domain models, ports, adapters, validation layer,
              and normalization layer are ready. Connect the OCR provider and Gemini API
              to begin tracking real activities.
            </p>
          </div>
        </div>

      </main>
    </div>
  )
}

// ── Sub-components ─────────────────────────────────────────────────────────

interface StatCardProps {
  label: string
  value: string
  unit: string
  icon: React.ReactNode
  color: string
  bg: string
}

function StatCard({ label, value, unit, icon, color, bg }: StatCardProps) {
  return (
    <div className="card hover:border-slate-600 transition-all duration-200 cursor-default group">
      <div className="flex items-start justify-between mb-3">
        <span className="text-sm text-slate-400">{label}</span>
        <div className={`${bg} ${color} p-2 rounded-lg group-hover:scale-110 transition-transform duration-200`}>
          {icon}
        </div>
      </div>
      <div className="text-3xl font-bold tracking-tight">{value}</div>
      {unit && <div className="text-xs text-slate-500 mt-1">{unit}</div>}
    </div>
  )
}

interface PipelineCardProps {
  source: ActivitySource
  label: string
  description: string
  phase: string
  status: 'active' | 'pending' | 'future'
}

function PipelineCard({ label, description, phase, status }: PipelineCardProps) {
  const statusConfig = {
    active:  { dot: 'bg-brand-400',  badge: 'badge-green',  text: 'Active' },
    pending: { dot: 'bg-yellow-400', badge: 'badge-slate',  text: 'In Progress' },
    future:  { dot: 'bg-slate-600',  badge: 'badge-slate',  text: 'Planned' },
  }
  const cfg = statusConfig[status]

  return (
    <div className="bg-slate-800/50 border border-slate-700 rounded-xl p-5
                    hover:border-slate-600 transition-all duration-200">
      <div className="flex items-center justify-between mb-3">
        <div className={`w-2 h-2 rounded-full ${cfg.dot}`} />
        <span className={cfg.badge}>{phase}</span>
      </div>
      <h3 className="font-semibold text-sm mb-1">{label}</h3>
      <p className="text-xs text-slate-400 leading-relaxed">{description}</p>
    </div>
  )
}
