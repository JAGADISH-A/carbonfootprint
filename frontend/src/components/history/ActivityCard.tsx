import { motion } from 'framer-motion'
import { Shield, AlertTriangle, Info } from 'lucide-react'
import type { TopEmissionActivity, CarbonInsightResponse } from '@/types/activity'
import { categoryConfig } from '@/pages/insights/constants'

interface ActivityCardProps {
  activity: TopEmissionActivity
  index: number
  insights?: CarbonInsightResponse | null
}

// Backend-aware insight derivation: uses actual insight data when available,
// falls back to context-aware generic insights based on activity properties.
function getInsight(
  activity: TopEmissionActivity,
  insights: CarbonInsightResponse | null | undefined,
  index: number
): string {
  // If backend provides recommendations, use the most relevant one
  if (insights?.recommendations && insights.recommendations.length > 0) {
    // Try to find a category-specific recommendation
    const categoryRec = insights.recommendations.find((r) => {
      const lower = r.toLowerCase()
      return lower.includes(activity.category.toLowerCase())
    })
    if (categoryRec) return categoryRec
    // Use recommendation by index rotation for variety
    return insights.recommendations[index % insights.recommendations.length]
  }

  // If backend provides insights, use them
  if (insights?.insights && insights.insights.length > 0) {
    return insights.insights[index % insights.insights.length]
  }

  // If backend provides warnings and this is a high-emission activity
  if (insights?.warnings && insights.warnings.length > 0 && Number(activity.carbonKg ?? 0) > 5) {
    return insights.warnings[0]
  }

  // Context-aware fallback: derive insight from activity properties
  const kg = Number(activity.carbonKg ?? 0)
  const hasMerchant = !!activity.merchant

  if (kg > 20) {
    return `This ${activity.category.toLowerCase()} activity generated ${kg.toFixed(1)} kg CO₂e — one of your higher-impact purchases`
  }
  if (hasMerchant) {
    return `Tracked from ${activity.merchant} — check your coach for personalized reduction tips`
  }
  return `Your ${activity.category.toLowerCase()} activity has been analyzed for carbon impact`
}

function getConfidence(activity: TopEmissionActivity): 'high' | 'medium' | 'low' {
  // Simulate confidence based on data completeness
  const hasMerchant = !!activity.merchant
  const hasCarbon = Number(activity.carbonKg ?? 0) > 0

  if (hasMerchant && hasCarbon) return 'high'
  if (hasCarbon) return 'medium'
  return 'low'
}

function formatTime(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true,
  })
}

export default function ActivityCard({ activity, index, insights }: ActivityCardProps) {
  const config = categoryConfig[activity.category] ?? categoryConfig.OTHER
  const label = activity.category.charAt(0) + activity.category.slice(1).toLowerCase()
  const confidence = getConfidence(activity)
  const insight = getInsight(activity, insights, index)

  const confidenceConfig = {
    high: { label: 'High', color: 'bg-emerald-100 text-emerald-700', icon: Shield },
    medium: { label: 'Medium', color: 'bg-amber-100 text-amber-700', icon: Info },
    low: { label: 'Low', color: 'bg-gray-100 text-gray-600', icon: AlertTriangle },
  }

  const conf = confidenceConfig[confidence]
  const ConfIcon = conf.icon

  return (
    <motion.div
      initial={{ opacity: 0, x: -8 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.05, duration: 0.3 }}
      whileHover={{ x: 4, backgroundColor: 'rgba(236,253,245,0.2)' }}
      className="flex items-start gap-3 p-3 rounded-xl border border-border-light/50 hover:border-emerald-200/50 transition-all cursor-default"
    >
      {/* Category icon */}
      <motion.div
        className={`w-10 h-10 rounded-xl ${config.bg} flex items-center justify-center flex-shrink-0`}
        whileHover={{ scale: 1.1, rotate: [0, -5, 5, 0] }}
        transition={{ duration: 0.3 }}
      >
        <span className={config.color}>{config.icon}</span>
      </motion.div>

      {/* Content */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-1">
          <p className="text-sm font-medium text-ink truncate">
            {activity.merchant || label}
          </p>
          <span className="badge-green text-[9px] shrink-0">{label}</span>
        </div>

        {/* AI Insight */}
        <div className="flex items-start gap-1.5 mt-1.5">
          <div className="w-4 h-4 rounded bg-emerald-50 flex items-center justify-center flex-shrink-0 mt-0.5">
            <span className="text-[8px]">💡</span>
          </div>
          <p className="text-[11px] text-ink-muted leading-relaxed line-clamp-2">
            {insight}
          </p>
        </div>

        {/* Meta row */}
        <div className="flex items-center gap-3 mt-2">
          <span className="text-[10px] text-ink-faint">
            {formatTime(activity.occurredAt)}
          </span>
          <div className={`flex items-center gap-1 text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${conf.color}`}>
            <ConfIcon className="w-2.5 h-2.5" />
            {conf.label}
          </div>
        </div>
      </div>

      {/* Carbon amount */}
      <div className="text-right flex-shrink-0">
        <p className="text-sm font-bold text-ink">
          {Number(activity.carbonKg ?? 0).toFixed(1)}
        </p>
        <p className="text-[9px] text-ink-muted">kg CO₂e</p>
      </div>
    </motion.div>
  )
}
