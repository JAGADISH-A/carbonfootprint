import { motion } from 'framer-motion'
import { TrendingUp, TrendingDown, Minus, ChevronDown } from 'lucide-react'
import { useState } from 'react'
import type { TopEmissionActivity } from '@/types/activity'
import { categoryConfig } from '@/pages/insights/constants'
import ActivityCard from './ActivityCard'

export interface DateGroupData {
  label: string
  dateRange: string
  activities: TopEmissionActivity[]
  totalCarbon: number
  dominantCategory: string
  previousPeriodCarbon?: number
}

interface DateGroupProps {
  group: DateGroupData
  index: number
  defaultExpanded?: boolean
}

export default function DateGroup({ group, index, defaultExpanded = true }: DateGroupProps) {
  const [expanded, setExpanded] = useState(defaultExpanded)
  const dominantConfig = categoryConfig[group.dominantCategory] ?? categoryConfig.OTHER
  const dominantLabel = group.dominantCategory
    ? group.dominantCategory.charAt(0) + group.dominantCategory.slice(1).toLowerCase()
    : 'None'

  // Calculate trend
  const trend = group.previousPeriodCarbon !== undefined
    ? group.totalCarbon - group.previousPeriodCarbon
    : null
  const trendPercent = trend !== null && group.previousPeriodCarbon
    ? ((trend / group.previousPeriodCarbon) * 100).toFixed(0)
    : null

  // Category breakdown for this group
  const categoryBreakdown: Record<string, number> = {}
  group.activities.forEach((a) => {
    const cat = a.category || 'OTHER'
    categoryBreakdown[cat] = (categoryBreakdown[cat] || 0) + Number(a.carbonKg ?? 0)
  })
  const sortedCategories = Object.entries(categoryBreakdown)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 3)

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: index * 0.1, ease: [0.22, 1, 0.36, 1] }}
      className="card overflow-hidden"
    >
      {/* Group header */}
      <button
        onClick={() => setExpanded(!expanded)}
        className="w-full text-left p-4 hover:bg-cream-50/50 transition-colors"
      >
        <div className="flex items-start justify-between gap-4">
          {/* Left: Label and date range */}
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <h3 className="text-base font-semibold text-ink">{group.label}</h3>
              {trend !== null && trendPercent && (
                <div className={`flex items-center gap-0.5 text-[10px] font-semibold px-1.5 py-0.5 rounded-full ${
                  trend > 0
                    ? 'bg-amber-100 text-amber-700'
                    : trend < 0
                    ? 'bg-emerald-100 text-emerald-700'
                    : 'bg-gray-100 text-gray-600'
                }`}>
                  {trend > 0 ? (
                    <TrendingUp className="w-2.5 h-2.5" />
                  ) : trend < 0 ? (
                    <TrendingDown className="w-2.5 h-2.5" />
                  ) : (
                    <Minus className="w-2.5 h-2.5" />
                  )}
                  {trend > 0 ? '+' : ''}{trendPercent}%
                </div>
              )}
            </div>
            <p className="text-xs text-ink-muted">{group.dateRange}</p>
          </div>

          {/* Right: Stats */}
          <div className="flex items-center gap-4">
            {/* Dominant category */}
            <div className="flex items-center gap-1.5">
              <div className={`w-6 h-6 rounded-md ${dominantConfig.bg} flex items-center justify-center`}>
                <span className={dominantConfig.color}>{dominantConfig.icon}</span>
              </div>
              <div className="text-right">
                <p className="text-[10px] text-ink-muted">Top source</p>
                <p className="text-xs font-medium text-ink">{dominantLabel}</p>
              </div>
            </div>

            {/* Activity count */}
            <div className="text-right">
              <p className="text-[10px] text-ink-muted">Activities</p>
              <p className="text-xs font-medium text-ink">{group.activities.length}</p>
            </div>

            {/* Total carbon */}
            <div className="text-right">
              <p className="text-[10px] text-ink-muted">Total</p>
              <p className="text-sm font-bold text-ink">{group.totalCarbon.toFixed(1)} kg</p>
            </div>

            {/* Expand/collapse */}
            <motion.div
              animate={{ rotate: expanded ? 180 : 0 }}
              transition={{ duration: 0.2 }}
            >
              <ChevronDown className="w-4 h-4 text-ink-muted" />
            </motion.div>
          </div>
        </div>

        {/* Mini category bar */}
        {sortedCategories.length > 0 && (
          <div className="mt-3 flex gap-1 h-1.5">
            {sortedCategories.map(([cat, carbon], i) => {
              const config = categoryConfig[cat] ?? categoryConfig.OTHER
              const width = (carbon / group.totalCarbon) * 100
              return (
                <motion.div
                  key={cat}
                  initial={{ width: 0 }}
                  animate={{ width: `${width}%` }}
                  transition={{ delay: 0.2 + i * 0.05, duration: 0.4 }}
                  className={`rounded-full ${config.bg.replace('50', '300')}`}
                  title={`${cat}: ${carbon.toFixed(1)} kg`}
                />
              )
            })}
          </div>
        )}
      </button>

      {/* Expanded activities */}
      <motion.div
        initial={false}
        animate={{
          height: expanded ? 'auto' : 0,
          opacity: expanded ? 1 : 0,
        }}
        transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
        className="overflow-hidden"
      >
        <div className="px-4 pb-4 space-y-2">
          {group.activities.map((activity, i) => (
            <ActivityCard
              key={activity.activityId}
              activity={activity}
              index={i}
            />
          ))}
        </div>
      </motion.div>
    </motion.div>
  )
}
