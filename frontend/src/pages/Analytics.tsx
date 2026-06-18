import { useEffect, useState, useCallback } from 'react'
import { motion } from 'framer-motion'
import { Leaf, BarChart3, Clock, Trophy, Flame } from 'lucide-react'
import { getCarbonAnalytics, getCarbonInsights } from '@/api/services'
import type { CarbonAnalyticsResponse, CarbonInsightResponse } from '@/types/activity'
import { equivalents, stagger, fadeUp } from './insights/constants'
import CarbonScore from './insights/CarbonScore'
import ImpactEquivalents from './insights/ImpactEquivalents'
import MonthlyTrend from './insights/MonthlyTrend'
import CategoryBreakdown from './insights/CategoryBreakdown'
import ActivityTimeline from './insights/ActivityTimeline'
import AchievementBadges from './insights/AchievementBadges'

export default function Analytics() {
  const [analytics, setAnalytics] = useState<CarbonAnalyticsResponse | null>(null)
  const [insights, setInsights] = useState<CarbonInsightResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const [aRes, iRes] = await Promise.all([
        getCarbonAnalytics(),
        getCarbonInsights(),
      ])
      if (aRes.success && aRes.data) setAnalytics(aRes.data)
      if (iRes.success && iRes.data) setInsights(iRes.data)
    } catch {
      // silent
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const totalKg = Number(analytics?.totalCarbonKg ?? 0)
  const avgDaily = Number(analytics?.averageDailyKg ?? 0)
  const categories = analytics?.categoryTotals ?? []
  const monthly = analytics?.monthlyTrend ?? []
  const activities = analytics?.topActivities ?? []
  const maxCatKg = Math.max(...categories.map((c) => c.carbonKg), 0)
  const maxMonthKg = Math.max(...monthly.map((m) => m.carbonKg), 0)
  const achievements = insights?.achievements ?? []
  const hasData = totalKg > 0

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto space-y-6">
        <div className="card flex flex-col items-center py-10">
          <div className="skeleton w-44 h-44 rounded-full mb-4" />
          <div className="skeleton w-32 h-4" />
        </div>
        <div className="card">
          <div className="skeleton w-40 h-5 mb-6" />
          <div className="flex gap-3 h-32">
            {[0, 1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="flex-1 skeleton rounded-xl" />
            ))}
          </div>
        </div>
        <div className="card space-y-4">
          {[0, 1, 2].map((i) => (
            <div key={i} className="flex items-center gap-3">
              <div className="skeleton w-9 h-9 rounded-xl" />
              <div className="flex-1 space-y-2">
                <div className="skeleton w-24 h-3" />
                <div className="skeleton w-full h-2 rounded-full" />
              </div>
            </div>
          ))}
        </div>
      </div>
    )
  }

  return (
    <div className="max-w-4xl mx-auto">
      <motion.div variants={stagger} initial="hidden" animate="show">

        {/* ── Carbon Score ──────────────────────────────────────────── */}
        <motion.div variants={fadeUp} className="card flex flex-col items-center py-10 mb-6">
          <p className="text-xs font-semibold text-emerald-600 uppercase tracking-wider mb-5">
            Your Carbon Score
          </p>
          <CarbonScore kg={totalKg} />
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 1.2 }}
            className="mt-5 flex flex-wrap items-center justify-center gap-2 text-sm text-ink-muted"
          >
            {avgDaily > 0 ? (
              <>
                <span>~{avgDaily.toFixed(1)} kg per day</span>
                <span className="w-1 h-1 rounded-full bg-ink-faint" />
                <span>{analytics?.activityCount ?? 0} activities</span>
              </>
            ) : (
              <span>Upload receipts to see your score</span>
            )}
          </motion.div>
        </motion.div>

        {/* ── Environmental Equivalents ─────────────────────────────── */}
        {!hasData && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="text-center py-10">
              <motion.div
                className="w-20 h-20 rounded-3xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/50 flex items-center justify-center mx-auto mb-5"
                animate={{ y: [0, -6, 0] }}
                transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
              >
                <span className="text-4xl">📊</span>
              </motion.div>
              <h3 className="text-lg font-semibold text-ink mb-1.5">Your insights await</h3>
              <p className="text-sm text-ink-muted max-w-xs mx-auto leading-relaxed mb-6">
                Upload your first receipt and we'll paint a picture of your environmental impact.
              </p>
              <p className="text-xs text-ink-faint">
                Carbon score · Environmental equivalents · Category breakdown · Monthly trends
              </p>
            </div>
          </motion.div>
        )}
        {hasData && (
          <motion.div variants={fadeUp} className="mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <Leaf className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">What does this mean?</h2>
                <p className="text-xs text-ink-muted">Your carbon in relatable terms</p>
              </div>
            </div>
            <ImpactEquivalents equivalents={equivalents} totalKg={totalKg} />
          </motion.div>
        )}

        {/* ── Monthly Trend ─────────────────────────────────────────── */}
        {monthly.length > 0 && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="flex items-center gap-3 mb-6">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <BarChart3 className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Monthly Trend</h2>
                <p className="text-xs text-ink-muted">Your emissions over time</p>
              </div>
            </div>
            <MonthlyTrend monthly={monthly} maxKg={maxMonthKg} />
          </motion.div>
        )}

        {/* ── Category Breakdown ────────────────────────────────────── */}
        {categories.length > 0 && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="flex items-center gap-3 mb-5">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <Flame className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Category Breakdown</h2>
                <p className="text-xs text-ink-muted">Where your emissions come from</p>
              </div>
            </div>
            <CategoryBreakdown categories={categories} maxKg={maxCatKg} />
          </motion.div>
        )}

        {/* ── Achievements ──────────────────────────────────────────── */}
        {achievements.length > 0 && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-amber-50 flex items-center justify-center">
                <Trophy className="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Achievements</h2>
                <p className="text-xs text-ink-muted">Milestones you&apos;ve reached</p>
              </div>
            </div>
            <AchievementBadges achievements={achievements} />
          </motion.div>
        )}

        {/* ── Activity Timeline ─────────────────────────────────────── */}
        {activities.length > 0 && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="flex items-center gap-3 mb-5">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <Clock className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Recent Activity</h2>
                <p className="text-xs text-ink-muted">Your latest tracked emissions</p>
              </div>
            </div>
            <ActivityTimeline activities={activities.slice(0, 8)} />
          </motion.div>
        )}

      </motion.div>
    </div>
  )
}
