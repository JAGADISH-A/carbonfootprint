import { motion } from 'framer-motion'
import {
  TrendingUp,
  Leaf,
  Target,
  Activity,
  BarChart3,
  Zap,
  ShieldCheck,
  ChevronRight,
} from 'lucide-react'
import type { AICarbonCoachResponse, CarbonAnalyticsResponse, ActionPlanItem } from '@/types/activity'

interface CarbonSummaryPanelProps {
  coach: AICarbonCoachResponse | null
  analytics: CarbonAnalyticsResponse | null
  loading: boolean
}

const stagger = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.04 } },
}

const item = {
  hidden: { opacity: 0, y: 4 },
  show: { opacity: 1, y: 0, transition: { duration: 0.3, ease: [0.22, 1, 0.36, 1] } },
}

function StatCard({
  icon: Icon,
  label,
  value,
  unit,
  color,
}: {
  icon: React.ElementType
  label: string
  value: string | number
  unit?: string
  color: string
}) {
  return (
    <motion.div variants={item} className="bg-white rounded-lg border border-border-light p-2.5">
      <div className="flex items-center gap-2 mb-1.5">
        <div className={`w-6 h-6 rounded-md ${color} flex items-center justify-center`}>
          <Icon className="w-3 h-3 text-white" />
        </div>
        <span className="text-[9px] font-semibold text-ink-muted uppercase tracking-wider">{label}</span>
      </div>
      <div className="flex items-baseline gap-1">
        <span className="text-lg font-bold text-ink leading-none">{value}</span>
        {unit && <span className="text-[10px] text-ink-muted">{unit}</span>}
      </div>
    </motion.div>
  )
}

function CategoryBar({
  category,
  percentage,
  carbonKg,
}: {
  category: string
  percentage: number
  carbonKg: number
}) {
  const colors: Record<string, string> = {
    FOOD: 'bg-emerald-500',
    FUEL: 'bg-amber-500',
    TRANSPORT: 'bg-blue-500',
    ELECTRICITY: 'bg-yellow-500',
    SHOPPING: 'bg-purple-500',
    FLIGHT: 'bg-red-500',
    OTHER: 'bg-gray-400',
  }

  return (
    <div className="space-y-0.5">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-medium text-ink capitalize">{category.toLowerCase()}</span>
        <span className="text-[10px] text-ink-muted">{carbonKg.toFixed(1)} kg · {percentage.toFixed(0)}%</span>
      </div>
      <div className="h-1 bg-surface rounded-full overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${Math.max(percentage, 2)}%` }}
          transition={{ duration: 0.6, delay: 0.15, ease: [0.22, 1, 0.36, 1] }}
          className={`h-full rounded-full ${colors[category] || 'bg-gray-400'}`}
        />
      </div>
    </div>
  )
}

function ActionPlanMini({ items }: { items: ActionPlanItem[] }) {
  return (
    <div className="space-y-1.5">
      {items.slice(0, 3).map((action, i) => (
        <motion.div
          key={i}
          variants={item}
          className="flex items-start gap-2 group"
        >
          <div className="w-5 h-5 rounded bg-gradient-to-br from-emerald-500 to-emerald-600 flex items-center justify-center flex-shrink-0 text-[9px] font-bold text-white">
            {action.priority}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-[11px] font-medium text-ink leading-snug">{action.title}</p>
            <p className="text-[10px] text-ink-muted mt-0.5 line-clamp-1">{action.whyItMatters}</p>
          </div>
          <ChevronRight className="w-3 h-3 text-ink-faint mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity" />
        </motion.div>
      ))}
    </div>
  )
}

export default function CarbonSummaryPanel({ coach, analytics, loading }: CarbonSummaryPanelProps) {
  if (loading) {
    return (
      <div className="space-y-2.5">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="skeleton h-16 rounded-lg" />
        ))}
      </div>
    )
  }

  const totalKg = analytics?.totalCarbonKg ?? 0
  const activityCount = analytics?.activityCount ?? 0
  const avgDaily = analytics?.averageDailyKg ?? 0
  const confidence = coach?.confidence ?? 0
  const categories = analytics?.categoryTotals ?? []
  const actionPlan = coach?.actionPlan ?? []

  return (
    <motion.div
      variants={stagger}
      initial="hidden"
      animate="show"
      className="space-y-3 overflow-y-auto h-full pr-1"
    >
      {/* Stats Grid */}
      <div className="grid grid-cols-2 gap-2">
        <StatCard icon={TrendingUp} label="Total CO₂" value={totalKg.toFixed(1)} unit="kg" color="bg-gradient-to-br from-emerald-500 to-emerald-600" />
        <StatCard icon={Activity} label="Activities" value={activityCount} color="bg-gradient-to-br from-blue-500 to-blue-600" />
        <StatCard icon={BarChart3} label="Daily Avg" value={avgDaily.toFixed(1)} unit="kg" color="bg-gradient-to-br from-amber-500 to-amber-600" />
        <StatCard icon={ShieldCheck} label="Confidence" value={`${confidence}%`} color="bg-gradient-to-br from-purple-500 to-purple-600" />
      </div>

      {/* Carbon Score */}
      <motion.div variants={item} className="bg-white rounded-lg border border-border-light p-3">
        <div className="flex items-center gap-1.5 mb-2">
          <Leaf className="w-3.5 h-3.5 text-emerald-600" />
          <h3 className="text-[10px] font-semibold text-ink uppercase tracking-wider">Carbon Score</h3>
        </div>
        <div className="flex items-center gap-3">
          <div className="relative w-12 h-12">
            <svg className="w-12 h-12 -rotate-90" viewBox="0 0 48 48">
              <circle cx="24" cy="24" r="20" fill="none" stroke="#ecfdf5" strokeWidth="4" />
              <motion.circle
                cx="24" cy="24" r="20" fill="none" stroke="#10b981" strokeWidth="4" strokeLinecap="round"
                strokeDasharray={126}
                initial={{ strokeDashoffset: 126 }}
                animate={{ strokeDashoffset: 126 - (126 * Math.min(confidence, 100)) / 100 }}
                transition={{ duration: 1, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
              />
            </svg>
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-xs font-bold text-ink">{confidence}</span>
            </div>
          </div>
          <div className="flex-1">
            <p className="text-xs font-medium text-ink">
              {confidence >= 80 ? 'Excellent tracking' : confidence >= 50 ? 'Good progress' : 'Getting started'}
            </p>
            <p className="text-[10px] text-ink-muted leading-snug">
              {confidence >= 80
                ? 'Your data paints a clear picture.'
                : confidence >= 50
                ? 'Upload more for deeper insights.'
                : 'Upload receipts to unlock coaching.'}
            </p>
          </div>
        </div>
      </motion.div>

      {/* Categories */}
      {categories.length > 0 && (
        <motion.div variants={item} className="bg-white rounded-lg border border-border-light p-3">
          <div className="flex items-center gap-1.5 mb-2">
            <Target className="w-3.5 h-3.5 text-emerald-600" />
            <h3 className="text-[10px] font-semibold text-ink uppercase tracking-wider">Categories</h3>
          </div>
          <div className="space-y-2">
            {categories.slice(0, 5).map((cat) => (
              <CategoryBar key={cat.category} category={cat.category} percentage={cat.percentageOfTotal} carbonKg={cat.carbonKg} />
            ))}
          </div>
        </motion.div>
      )}

      {/* Action Plan */}
      {actionPlan.length > 0 && (
        <motion.div variants={item} className="bg-white rounded-lg border border-border-light p-3">
          <div className="flex items-center gap-1.5 mb-2">
            <Zap className="w-3.5 h-3.5 text-emerald-600" />
            <h3 className="text-[10px] font-semibold text-ink uppercase tracking-wider">Action Plan</h3>
          </div>
          <ActionPlanMini items={actionPlan} />
        </motion.div>
      )}

      {/* Strengths */}
      {coach?.strengths && coach.strengths.length > 0 && (
        <motion.div variants={item} className="bg-emerald-50/50 rounded-lg border border-emerald-100 p-3">
          <h3 className="text-[10px] font-semibold text-emerald-700 uppercase tracking-wider mb-1.5">
            What's going well
          </h3>
          <ul className="space-y-1">
            {coach.strengths.slice(0, 3).map((s, i) => (
              <li key={i} className="text-[11px] text-emerald-800 leading-snug flex gap-1.5">
                <span className="text-emerald-500 mt-px">•</span>
                <span className="line-clamp-2">{s}</span>
              </li>
            ))}
          </ul>
        </motion.div>
      )}

      {/* Concerns */}
      {coach?.concerns && coach.concerns.length > 0 && (
        <motion.div variants={item} className="bg-amber-50/50 rounded-lg border border-amber-100 p-3">
          <h3 className="text-[10px] font-semibold text-amber-700 uppercase tracking-wider mb-1.5">
            Worth watching
          </h3>
          <ul className="space-y-1">
            {coach.concerns.slice(0, 3).map((c, i) => (
              <li key={i} className="text-[11px] text-amber-800 leading-snug flex gap-1.5">
                <span className="text-amber-500 mt-px">•</span>
                <span className="line-clamp-2">{c}</span>
              </li>
            ))}
          </ul>
        </motion.div>
      )}
    </motion.div>
  )
}
