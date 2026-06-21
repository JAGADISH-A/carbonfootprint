import { motion } from 'framer-motion'
import { TrendingDown, TrendingUp, Minus, Target, Lightbulb, Sparkles } from 'lucide-react'
import type { CarbonAnalyticsResponse, AICarbonCoachResponse } from '@/types/activity'

interface CarbonJourneyProps {
  analytics: CarbonAnalyticsResponse | null
  coach: AICarbonCoachResponse | null
  loading: boolean
}

export default function CarbonJourney({ analytics, coach, loading }: CarbonJourneyProps) {
  if (loading) {
    return (
      <div className="card">
        <div className="flex items-center gap-2.5 mb-4">
          <div className="w-8 h-8 rounded-xl bg-emerald-50 flex items-center justify-center">
            <Sparkles className="w-4 h-4 text-emerald-600" />
          </div>
          <div>
            <h2 className="text-sm font-semibold text-ink">Your Carbon Journey</h2>
            <p className="text-[11px] text-ink-muted">Loading your progress...</p>
          </div>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 animate-pulse">
          {[0, 1, 2, 3].map((i) => (
            <div key={i} className="h-24 skeleton rounded-xl" />
          ))}
        </div>
      </div>
    )
  }

  if (!analytics || (analytics.activityCount ?? 0) === 0) return null

  const totalKg = Number(analytics.totalCarbonKg ?? 0)
  const avgDaily = Number(analytics.averageDailyKg ?? 0)
  const categories = analytics.categoryTotals ?? []
  const monthly = analytics.monthlyTrend ?? []
  const topCategory = categories[0] ?? null
  const topCatLabel = topCategory
    ? topCategory.category.charAt(0) + topCategory.category.slice(1).toLowerCase()
    : null

  // Calculate week-over-week comparison
  const weeklyComparison = (() => {
    if (monthly.length < 2) return null
    const sorted = [...monthly].sort(
      (a, b) => new Date(b.month).getTime() - new Date(a.month).getTime()
    )
    const current = Number(sorted[0]?.carbonKg ?? 0)
    const previous = Number(sorted[1]?.carbonKg ?? 0)
    if (previous === 0) return null
    const changePercent = ((current - previous) / previous) * 100
    return {
      current,
      previous,
      changePercent,
      isLower: current < previous,
      isFlat: Math.abs(changePercent) < 5,
    }
  })()

  // Calculate potential savings from coach recommendations
  const potentialSavings = (() => {
    if (!coach?.recommendations || coach.recommendations.length === 0) return null
    // Estimate 10-20% savings potential from total
    return totalKg * 0.15
  })()

  // Find best improvement (lowest carbon category relative to its count)
  const bestImprovement = (() => {
    if (categories.length < 2) return null
    const sorted = [...categories].sort(
      (a, b) => Number(a.carbonKg ?? 0) / Math.max(1, a.activityCount) -
               Number(b.carbonKg ?? 0) / Math.max(1, b.activityCount)
    )
    const best = sorted[0]
    const label = best.category.charAt(0) + best.category.slice(1).toLowerCase()
    return {
      category: label,
      avgPerActivity: Number(best.carbonKg ?? 0) / Math.max(1, best.activityCount),
    }
  })()

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      className="card"
    >
      <div className="flex items-center gap-2.5 mb-4">
        <div className="w-8 h-8 rounded-xl bg-emerald-50 flex items-center justify-center">
          <Sparkles className="w-4 h-4 text-emerald-600" />
        </div>
        <div>
          <h2 className="text-sm font-semibold text-ink">Your Carbon Journey</h2>
          <p className="text-[11px] text-ink-muted">A snapshot of your recent progress</p>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {/* This Week */}
        <div className="rounded-xl bg-gradient-to-br from-emerald-50/80 to-leaf-50/40 border border-emerald-100/60 p-3.5">
          <div className="flex items-center gap-1.5 mb-2">
            <div className="w-5 h-5 rounded-md bg-emerald-100 flex items-center justify-center">
              <Target className="w-3 h-3 text-emerald-600" />
            </div>
            <span className="text-[10px] font-semibold text-emerald-700 uppercase tracking-wider">
              This Period
            </span>
          </div>
          <p className="text-xl font-bold text-ink leading-none mb-0.5">
            {totalKg.toFixed(1)}
          </p>
          <p className="text-[10px] text-ink-faint">kg CO₂e total</p>
        </div>

        {/* Largest Source */}
        <div className="rounded-xl bg-gradient-to-br from-orange-50/80 to-red-50/40 border border-orange-100/60 p-3.5">
          <div className="flex items-center gap-1.5 mb-2">
            <div className="w-5 h-5 rounded-md bg-orange-100 flex items-center justify-center">
              <span className="text-[10px]">🔥</span>
            </div>
            <span className="text-[10px] font-semibold text-orange-700 uppercase tracking-wider">
              Largest Source
            </span>
          </div>
          <p className="text-lg font-bold text-ink leading-none mb-0.5 truncate">
            {topCatLabel || '—'}
          </p>
          <p className="text-[10px] text-ink-faint">
            {topCategory ? `${Number(topCategory.carbonKg ?? 0).toFixed(1)} kg` : 'No data'}
          </p>
        </div>

        {/* Best Improvement */}
        <div className="rounded-xl bg-gradient-to-br from-blue-50/80 to-cyan-50/40 border border-blue-100/60 p-3.5">
          <div className="flex items-center gap-1.5 mb-2">
            <div className="w-5 h-5 rounded-md bg-blue-100 flex items-center justify-center">
              <Lightbulb className="w-3 h-3 text-blue-600" />
            </div>
            <span className="text-[10px] font-semibold text-blue-700 uppercase tracking-wider">
              Lowest Impact
            </span>
          </div>
          <p className="text-lg font-bold text-ink leading-none mb-0.5 truncate">
            {bestImprovement?.category || '—'}
          </p>
          <p className="text-[10px] text-ink-faint">
            {bestImprovement
              ? `~${bestImprovement.avgPerActivity.toFixed(1)} kg avg`
              : 'Upload more to see'}
          </p>
        </div>

        {/* Potential Savings or Week Comparison */}
        {weeklyComparison ? (
          <div className={`rounded-xl bg-gradient-to-br p-3.5 border ${
            weeklyComparison.isLower
              ? 'from-emerald-50/80 to-green-50/40 border-emerald-100/60'
              : weeklyComparison.isFlat
              ? 'from-gray-50/80 to-slate-50/40 border-gray-100/60'
              : 'from-amber-50/80 to-orange-50/40 border-amber-100/60'
          }`}>
            <div className="flex items-center gap-1.5 mb-2">
              <div className={`w-5 h-5 rounded-md flex items-center justify-center ${
                weeklyComparison.isLower ? 'bg-emerald-100' : weeklyComparison.isFlat ? 'bg-gray-100' : 'bg-amber-100'
              }`}>
                {weeklyComparison.isLower ? (
                  <TrendingDown className="w-3 h-3 text-emerald-600" />
                ) : weeklyComparison.isFlat ? (
                  <Minus className="w-3 h-3 text-gray-600" />
                ) : (
                  <TrendingUp className="w-3 h-3 text-amber-600" />
                )}
              </div>
              <span className={`text-[10px] font-semibold uppercase tracking-wider ${
                weeklyComparison.isLower ? 'text-emerald-700' : weeklyComparison.isFlat ? 'text-gray-700' : 'text-amber-700'
              }`}>
                vs Last Period
              </span>
            </div>
            <p className="text-lg font-bold text-ink leading-none mb-0.5">
              {weeklyComparison.isLower ? '' : '+'}{weeklyComparison.changePercent.toFixed(0)}%
            </p>
            <p className="text-[10px] text-ink-faint">
              {weeklyComparison.isLower ? 'Lower than last period 🎉' : weeklyComparison.isFlat ? 'Holding steady' : 'Higher than last period'}
            </p>
          </div>
        ) : potentialSavings ? (
          <div className="rounded-xl bg-gradient-to-br from-violet-50/80 to-purple-50/40 border border-violet-100/60 p-3.5">
            <div className="flex items-center gap-1.5 mb-2">
              <div className="w-5 h-5 rounded-md bg-violet-100 flex items-center justify-center">
                <span className="text-[10px]">💡</span>
              </div>
              <span className="text-[10px] font-semibold text-violet-700 uppercase tracking-wider">
                Potential Savings
              </span>
            </div>
            <p className="text-lg font-bold text-ink leading-none mb-0.5">
              ~{potentialSavings.toFixed(1)}
            </p>
            <p className="text-[10px] text-ink-faint">kg CO₂e achievable</p>
          </div>
        ) : (
          <div className="rounded-xl bg-gradient-to-br from-gray-50/80 to-slate-50/40 border border-gray-100/60 p-3.5">
            <div className="flex items-center gap-1.5 mb-2">
              <div className="w-5 h-5 rounded-md bg-gray-100 flex items-center justify-center">
                <Sparkles className="w-3 h-3 text-gray-600" />
              </div>
              <span className="text-[10px] font-semibold text-gray-700 uppercase tracking-wider">
                Daily Average
              </span>
            </div>
            <p className="text-lg font-bold text-ink leading-none mb-0.5">
              {avgDaily.toFixed(1)}
            </p>
            <p className="text-[10px] text-ink-faint">kg per day</p>
          </div>
        )}
      </div>
    </motion.div>
  )
}
