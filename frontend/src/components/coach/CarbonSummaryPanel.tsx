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
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.06 },
  },
}

const item = {
  hidden: { opacity: 0, y: 8 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: [0.22, 1, 0.36, 1] } },
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
    <motion.div variants={item} className="bg-white rounded-xl border border-border-light p-3.5">
      <div className="flex items-center gap-2.5 mb-2">
        <div className={`w-8 h-8 rounded-lg ${color} flex items-center justify-center`}>
          <Icon className="w-4 h-4 text-white" />
        </div>
        <span className="text-[11px] font-medium text-ink-muted uppercase tracking-wider">{label}</span>
      </div>
      <div className="flex items-baseline gap-1">
        <span className="text-xl font-bold text-ink">{value}</span>
        {unit && <span className="text-xs text-ink-muted">{unit}</span>}
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
    <div className="space-y-1">
      <div className="flex items-center justify-between text-xs">
        <span className="text-ink font-medium capitalize">{category.toLowerCase()}</span>
        <span className="text-ink-muted">{carbonKg.toFixed(1)} kg ({percentage.toFixed(0)}%)</span>
      </div>
      <div className="h-1.5 bg-surface rounded-full overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${Math.max(percentage, 2)}%` }}
          transition={{ duration: 0.8, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
          className={`h-full rounded-full ${colors[category] || 'bg-gray-400'}`}
        />
      </div>
    </div>
  )
}

function ActionPlanMini({ items }: { items: ActionPlanItem[] }) {
  return (
    <div className="space-y-2">
      {items.slice(0, 3).map((action, i) => (
        <motion.div
          key={i}
          variants={item}
          className="flex items-start gap-2.5 group"
        >
          <div className="w-6 h-6 rounded-md bg-gradient-to-br from-emerald-500 to-emerald-600 flex items-center justify-center flex-shrink-0 text-[10px] font-bold text-white">
            {action.priority}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-xs font-medium text-ink leading-snug">{action.title}</p>
            <p className="text-[10px] text-ink-muted mt-0.5 line-clamp-2">{action.whyItMatters}</p>
          </div>
          <ChevronRight className="w-3 h-3 text-ink-faint mt-1 opacity-0 group-hover:opacity-100 transition-opacity" />
        </motion.div>
      ))}
    </div>
  )
}

export default function CarbonSummaryPanel({ coach, analytics, loading }: CarbonSummaryPanelProps) {
  if (loading) {
    return (
      <div className="space-y-4 p-1">
        {[1, 2, 3, 4].map((i) => (
          <div key={i} className="skeleton h-20 rounded-xl" />
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
      className="space-y-4 overflow-y-auto h-full pr-1"
    >
      {/* Stats Grid */}
      <div className="grid grid-cols-2 gap-3">
        <StatCard
          icon={TrendingUp}
          label="Total CO₂"
          value={totalKg.toFixed(1)}
          unit="kg"
          color="bg-gradient-to-br from-emerald-500 to-emerald-600"
        />
        <StatCard
          icon={Activity}
          label="Activities"
          value={activityCount}
          color="bg-gradient-to-br from-blue-500 to-blue-600"
        />
        <StatCard
          icon={BarChart3}
          label="Daily Avg"
          value={avgDaily.toFixed(1)}
          unit="kg"
          color="bg-gradient-to-br from-amber-500 to-amber-600"
        />
        <StatCard
          icon={ShieldCheck}
          label="Confidence"
          value={`${confidence}%`}
          color="bg-gradient-to-br from-purple-500 to-purple-600"
        />
      </div>

      {/* Carbon Score */}
      <motion.div
        variants={item}
        className="bg-white rounded-xl border border-border-light p-4"
      >
        <div className="flex items-center gap-2 mb-3">
          <Leaf className="w-4 h-4 text-emerald-600" />
          <h3 className="text-xs font-semibold text-ink uppercase tracking-wider">Carbon Score</h3>
        </div>
        <div className="flex items-center gap-4">
          <div className="relative w-16 h-16">
            <svg className="w-16 h-16 -rotate-90" viewBox="0 0 64 64">
              <circle cx="32" cy="32" r="28" fill="none" stroke="#ecfdf5" strokeWidth="6" />
              <motion.circle
                cx="32"
                cy="32"
                r="28"
                fill="none"
                stroke="#10b981"
                strokeWidth="6"
                strokeLinecap="round"
                strokeDasharray={176}
                initial={{ strokeDashoffset: 176 }}
                animate={{ strokeDashoffset: 176 - (176 * Math.min(confidence, 100)) / 100 }}
                transition={{ duration: 1.2, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
              />
            </svg>
            <div className="absolute inset-0 flex items-center justify-center">
              <span className="text-sm font-bold text-ink">{confidence}</span>
            </div>
          </div>
          <div className="flex-1">
            <p className="text-sm font-medium text-ink">
              {confidence >= 80 ? 'Excellent tracking' : confidence >= 50 ? 'Good progress' : 'Getting started'}
            </p>
            <p className="text-xs text-ink-muted mt-0.5">
              {confidence >= 80
                ? 'Your data paints a clear picture.'
                : confidence >= 50
                ? 'Upload more receipts for deeper insights.'
                : 'Upload receipts to unlock personalized coaching.'}
            </p>
          </div>
        </div>
      </motion.div>

      {/* Categories Breakdown */}
      {categories.length > 0 && (
        <motion.div
          variants={item}
          className="bg-white rounded-xl border border-border-light p-4"
        >
          <div className="flex items-center gap-2 mb-3">
            <Target className="w-4 h-4 text-emerald-600" />
            <h3 className="text-xs font-semibold text-ink uppercase tracking-wider">Categories</h3>
          </div>
          <div className="space-y-3">
            {categories.slice(0, 5).map((cat) => (
              <CategoryBar
                key={cat.category}
                category={cat.category}
                percentage={cat.percentageOfTotal}
                carbonKg={cat.carbonKg}
              />
            ))}
          </div>
        </motion.div>
      )}

      {/* Action Plan */}
      {actionPlan.length > 0 && (
        <motion.div
          variants={item}
          className="bg-white rounded-xl border border-border-light p-4"
        >
          <div className="flex items-center gap-2 mb-3">
            <Zap className="w-4 h-4 text-emerald-600" />
            <h3 className="text-xs font-semibold text-ink uppercase tracking-wider">Action Plan</h3>
          </div>
          <ActionPlanMini items={actionPlan} />
        </motion.div>
      )}

      {/* Strengths & Concerns */}
      {coach?.strengths && coach.strengths.length > 0 && (
        <motion.div
          variants={item}
          className="bg-emerald-50/50 rounded-xl border border-emerald-100 p-4"
        >
          <h3 className="text-xs font-semibold text-emerald-700 uppercase tracking-wider mb-2">
            What's going well
          </h3>
          <ul className="space-y-1.5">
            {coach.strengths.slice(0, 3).map((s, i) => (
              <li key={i} className="text-xs text-emerald-800 leading-relaxed flex gap-2">
                <span className="text-emerald-500 mt-0.5">•</span>
                {s}
              </li>
            ))}
          </ul>
        </motion.div>
      )}

      {coach?.concerns && coach.concerns.length > 0 && (
        <motion.div
          variants={item}
          className="bg-amber-50/50 rounded-xl border border-amber-100 p-4"
        >
          <h3 className="text-xs font-semibold text-amber-700 uppercase tracking-wider mb-2">
            Worth watching
          </h3>
          <ul className="space-y-1.5">
            {coach.concerns.slice(0, 3).map((c, i) => (
              <li key={i} className="text-xs text-amber-800 leading-relaxed flex gap-2">
                <span className="text-amber-500 mt-0.5">•</span>
                {c}
              </li>
            ))}
          </ul>
        </motion.div>
      )}
    </motion.div>
  )
}
