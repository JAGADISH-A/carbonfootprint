import { useEffect, useState, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight, Clock, TrendingUp, Calendar } from 'lucide-react'
import { getCarbonAnalytics } from '@/api/services'
import { useDashboard } from '@/api/DashboardContext'
import type { CarbonAnalyticsResponse, TopEmissionActivity } from '@/types/activity'
import { DateGroup, CategoryFilter } from '@/components/history'
import type { DateGroupData, CategoryFilterValue } from '@/components/history'
import EmptyStateCard from '@/components/common/EmptyStateCard'

const stagger = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.06 } },
}

const fadeUp = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: [0.22, 1, 0.36, 1] as const } },
}

function groupActivitiesByPeriod(
  activities: TopEmissionActivity[]
): DateGroupData[] {
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)
  const weekAgo = new Date(today)
  weekAgo.setDate(weekAgo.getDate() - 7)
  const monthAgo = new Date(today)
  monthAgo.setMonth(monthAgo.getMonth() - 1)

  const groups: Record<string, TopEmissionActivity[]> = {
    today: [],
    yesterday: [],
    thisWeek: [],
    lastMonth: [],
    older: [],
  }

  activities.forEach((a) => {
    const date = new Date(a.occurredAt)
    if (date >= today) {
      groups.today.push(a)
    } else if (date >= yesterday) {
      groups.yesterday.push(a)
    } else if (date >= weekAgo) {
      groups.thisWeek.push(a)
    } else if (date >= monthAgo) {
      groups.lastMonth.push(a)
    } else {
      groups.older.push(a)
    }
  })

  const result: DateGroupData[] = []

  const buildGroup = (
    _key: string,
    label: string,
    dateRange: string,
    activities: TopEmissionActivity[],
    previousPeriodCarbon?: number
  ): DateGroupData | null => {
    if (activities.length === 0) return null

    const totalCarbon = activities.reduce(
      (sum, a) => sum + Number(a.carbonKg ?? 0),
      0
    )

    // Find dominant category
    const categoryTotals: Record<string, number> = {}
    activities.forEach((a) => {
      const cat = a.category || 'OTHER'
      categoryTotals[cat] = (categoryTotals[cat] || 0) + Number(a.carbonKg ?? 0)
    })
    const dominantCategory = Object.entries(categoryTotals).sort(
      ([, a], [, b]) => b - a
    )[0]?.[0] || 'OTHER'

    return {
      label,
      dateRange,
      activities,
      totalCarbon,
      dominantCategory,
      previousPeriodCarbon,
    }
  }

  // Calculate previous period totals for comparison
  const yesterdayTotal = groups.yesterday.reduce(
    (sum, a) => sum + Number(a.carbonKg ?? 0),
    0
  )
  const thisWeekTotal = groups.thisWeek.reduce(
    (sum, a) => sum + Number(a.carbonKg ?? 0),
    0
  )

  const todayGroup = buildGroup(
    'today',
    'Today',
    formatDayRange(today),
    groups.today,
    yesterdayTotal
  )
  if (todayGroup) result.push(todayGroup)

  const yesterdayGroup = buildGroup(
    'yesterday',
    'Yesterday',
    formatDayRange(yesterday),
    groups.yesterday,
    thisWeekTotal / Math.max(1, groups.thisWeek.length)
  )
  if (yesterdayGroup) result.push(yesterdayGroup)

  const thisWeekGroup = buildGroup(
    'thisWeek',
    'This Week',
    `${formatShortDate(weekAgo)} – ${formatShortDate(yesterday)}`,
    groups.thisWeek
  )
  if (thisWeekGroup) result.push(thisWeekGroup)

  const lastMonthGroup = buildGroup(
    'lastMonth',
    'Last Month',
    `${formatShortDate(monthAgo)} – ${formatShortDate(weekAgo)}`,
    groups.lastMonth
  )
  if (lastMonthGroup) result.push(lastMonthGroup)

  const olderGroup = buildGroup(
    'older',
    'Older',
    'More than a month ago',
    groups.older
  )
  if (olderGroup) result.push(olderGroup)

  return result
}

function formatDayRange(date: Date): string {
  return date.toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'short',
    day: 'numeric',
  })
}

function formatShortDate(date: Date): string {
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
  })
}

export default function Activities() {
  const navigate = useNavigate()
  const { lastUpdated } = useDashboard()
  const [analytics, setAnalytics] = useState<CarbonAnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [selectedPeriod, setSelectedPeriod] = useState<'week' | 'month' | 'all'>('all')
  const [selectedCategory, setSelectedCategory] = useState<CategoryFilterValue>(null)
  const [retryKey, setRetryKey] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    setLoading(true)
    setError(false)
    const params: { from?: string; to?: string } = {}
    if (selectedPeriod === 'week') {
      const d = new Date()
      d.setDate(d.getDate() - 7)
      params.from = d.toISOString()
    } else if (selectedPeriod === 'month') {
      const d = new Date()
      d.setMonth(d.getMonth() - 1)
      params.from = d.toISOString()
    }
    getCarbonAnalytics(params, controller.signal)
      .then((res) => {
        if (res.success && res.data) {
          setAnalytics(res.data)
        } else {
          setError(true)
        }
      })
      .catch((err) => {
        if (err?.name !== 'AbortError') setError(true)
      })
      .finally(() => setLoading(false))
    return () => controller.abort()
  }, [selectedPeriod, lastUpdated, retryKey])

  const activities = analytics?.topActivities ?? []
  const hasData = activities.length > 0

  // Filter activities by category
  const filteredActivities = useMemo(() => {
    if (!selectedCategory) return activities
    return activities.filter((a) => a.category === selectedCategory)
  }, [activities, selectedCategory])

  // Group activities by time period
  const dateGroups = useMemo(
    () => groupActivitiesByPeriod(filteredActivities),
    [filteredActivities]
  )

  // Get unique categories with counts
  const categoryStats = useMemo(() => {
    const stats: Record<string, { carbonKg: number; count: number }> = {}
    activities.forEach((a) => {
      const cat = a.category || 'OTHER'
      if (!stats[cat]) stats[cat] = { carbonKg: 0, count: 0 }
      stats[cat].carbonKg += Number(a.carbonKg ?? 0)
      stats[cat].count += 1
    })
    return Object.entries(stats).map(([category, data]) => ({
      category,
      ...data,
    }))
  }, [activities])

  return (
    <motion.div variants={stagger} initial="hidden" animate="show">

      {/* ── Header ──────────────────────────────────────────────────── */}
      <motion.div variants={fadeUp} className="flex items-center justify-between mb-4">
        <div>
          <h1 className="text-xl font-bold text-ink tracking-tight">Activity Timeline</h1>
          <p className="text-xs text-ink-muted mt-0.5">
            {hasData
              ? `${activities.length} activities across ${dateGroups.length} time periods`
              : 'Your tracked carbon activities will appear here'}
          </p>
        </div>

        {hasData && (
          <div className="flex items-center gap-1.5 bg-cream-50 rounded-xl p-1 border border-border-light">
            {(['week', 'month', 'all'] as const).map((period) => (
              <button
                key={period}
                onClick={() => setSelectedPeriod(period)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  selectedPeriod === period
                    ? 'bg-white text-ink shadow-sm'
                    : 'text-ink-muted hover:text-ink'
                }`}
              >
                {period === 'week' ? '7 days' : period === 'month' ? '30 days' : 'All time'}
              </button>
            ))}
          </div>
        )}
      </motion.div>

      {error ? (
        <div className="card p-8 text-center max-w-md mx-auto my-12">
          <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center mx-auto mb-3">
            <span className="text-xl text-red-600">⚠️</span>
          </div>
          <h3 className="text-base font-bold text-ink mb-1">Failed to load activities</h3>
          <p className="text-xs text-ink-muted mb-4">
            There was an issue fetching your carbon activities. Please try again.
          </p>
          <button
            onClick={() => setRetryKey((k) => k + 1)}
            className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold rounded-md transition-colors text-xs"
          >
            Retry
          </button>
        </div>
      ) : loading ? (
        <div className="space-y-4">
          {[0, 1, 2].map((i) => (
            <div key={i} className="card p-4 animate-pulse">
              <div className="flex items-center gap-4 mb-3">
                <div className="skeleton w-24 h-5 bg-gray-200" />
                <div className="skeleton w-16 h-4 bg-gray-200" />
                <div className="skeleton w-20 h-4 bg-gray-200" />
              </div>
              <div className="space-y-2">
                {[0, 1].map((j) => (
                  <div key={j} className="flex gap-3">
                    <div className="skeleton w-10 h-10 rounded-xl bg-gray-200" />
                    <div className="flex-1 space-y-2">
                      <div className="skeleton w-32 h-3 bg-gray-200" />
                      <div className="skeleton w-48 h-2.5 bg-gray-200" />
                    </div>
                    <div className="skeleton w-12 h-5 bg-gray-200" />
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : !hasData ? (
        <motion.div variants={fadeUp}>
          <div className="card">
            <EmptyStateCard
              emoji="🌿"
              title="Your journey starts here"
              description="Every sustainable habit begins with one action. Upload your first receipt and watch your impact unfold over time."
              size="lg"
              action={
                <motion.button
                  onClick={() => navigate('/upload')}
                  whileHover={{ scale: 1.03, y: -1 }}
                  whileTap={{ scale: 0.97 }}
                  className="btn-primary inline-flex items-center gap-2"
                >
                  Upload your first receipt
                  <ArrowRight className="w-4 h-4" />
                </motion.button>
              }
            />
          </div>
        </motion.div>
      ) : (
        <div className="space-y-4">

          {/* ── Category Filter ───────────────────────────────────────── */}
          <motion.div variants={fadeUp}>
            <CategoryFilter
              categories={categoryStats}
              selected={selectedCategory}
              onChange={setSelectedCategory}
            />
          </motion.div>

          {/* ── Summary Stats ─────────────────────────────────────────── */}
          <motion.div variants={fadeUp} className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="card p-3 text-center">
              <TrendingUp className="w-4 h-4 text-emerald-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-ink">
                {analytics?.totalCarbonKg.toFixed(1) ?? '0'}
              </p>
              <p className="text-[10px] text-ink-muted">Total kg CO₂e</p>
            </div>
            <div className="card p-3 text-center">
              <Clock className="w-4 h-4 text-amber-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-ink">
                {analytics?.activityCount ?? 0}
              </p>
              <p className="text-[10px] text-ink-muted">Activities</p>
            </div>
            <div className="card p-3 text-center">
              <Calendar className="w-4 h-4 text-blue-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-ink">
                {dateGroups.length}
              </p>
              <p className="text-[10px] text-ink-muted">Time periods</p>
            </div>
            <div className="card p-3 text-center">
              <span className="text-lg">📊</span>
              <p className="text-lg font-bold text-ink mt-0.5">
                {analytics?.averageDailyKg.toFixed(1) ?? '0'}
              </p>
              <p className="text-[10px] text-ink-muted">Daily avg</p>
            </div>
          </motion.div>

          {/* ── Timeline Groups ───────────────────────────────────────── */}
          <motion.div variants={fadeUp} className="space-y-4">
            {dateGroups.map((group, i) => (
              <DateGroup
                key={group.label}
                group={group}
                index={i}
                defaultExpanded={i < 2}
              />
            ))}
          </motion.div>

          {/* ── Pattern Insight ───────────────────────────────────────── */}
          {activities.length >= 3 && (
            <motion.div
              variants={fadeUp}
              className="rounded-2xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/60 p-4"
            >
              <div className="flex items-start gap-3">
                <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center flex-shrink-0">
                  <span className="text-sm">🔍</span>
                </div>
                <div>
                  <h3 className="text-sm font-semibold text-ink mb-1">Pattern detected</h3>
                  <p className="text-xs text-ink-muted leading-relaxed">
                    {generatePatternInsight(dateGroups, categoryStats)}
                  </p>
                </div>
              </div>
            </motion.div>
          )}

        </div>
      )}

    </motion.div>
  )
}

function generatePatternInsight(
  groups: DateGroupData[],
  categoryStats: { category: string; carbonKg: number; count: number }[]
): string {
  if (categoryStats.length === 0) return ''

  const topCategory = categoryStats[0]
  const topLabel =
    topCategory.category.charAt(0) + topCategory.category.slice(1).toLowerCase()

  if (groups.length >= 2) {
    const recentTotal = groups[0].totalCarbon
    const previousTotal = groups[1].totalCarbon

    if (recentTotal < previousTotal * 0.8) {
      return `Great progress! Your emissions dropped ${Math.round(
        ((previousTotal - recentTotal) / previousTotal) * 100
      )}% compared to the previous period. ${topLabel} is still your biggest source.`
    }

    if (recentTotal > previousTotal * 1.2) {
      return `Your emissions increased this period. ${topLabel} accounted for most of it. Consider the actions in your coach for reduction tips.`
    }
  }

  return `Your biggest source is ${topLabel} at ${topCategory.carbonKg.toFixed(
    1
  )} kg CO₂e. This represents ${Math.round(
    (topCategory.carbonKg /
      categoryStats.reduce((sum, c) => sum + c.carbonKg, 0)) *
      100
  )}% of your total footprint.`
}
