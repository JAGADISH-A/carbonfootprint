import { useEffect, useState, useCallback } from 'react'
import { motion } from 'framer-motion'
import { Leaf, BarChart3, Clock, Trophy, MessageCircle, Sparkles } from 'lucide-react'
import { getCarbonAnalytics, getCarbonInsights } from '@/api/services'
import type { CarbonAnalyticsResponse, CarbonInsightResponse } from '@/types/activity'
import { equivalents, stagger, fadeUp } from './insights/constants'
import CarbonScore from './insights/CarbonScore'
import ImpactEquivalents from './insights/ImpactEquivalents'
import MonthlyTrend from './insights/MonthlyTrend'
import CategoryBreakdown from './insights/CategoryBreakdown'
import ActivityTimeline from './insights/ActivityTimeline'
import AchievementBadges from './insights/AchievementBadges'

function SectionHeader({ icon, title, subtitle }: { icon: React.ReactNode; title: string; subtitle: string }) {
  return (
    <div className="flex items-center gap-2.5 mb-3">
      <div className="w-8 h-8 rounded-xl bg-emerald-50 flex items-center justify-center flex-shrink-0">
        {icon}
      </div>
      <div>
        <h2 className="text-sm font-semibold text-ink">{title}</h2>
        <p className="text-[11px] text-ink-muted">{subtitle}</p>
      </div>
    </div>
  )
}

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
      <div className="space-y-4">
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="card flex flex-col items-center py-8">
            <div className="skeleton w-40 h-40 rounded-full mb-4" />
            <div className="skeleton w-28 h-4" />
          </div>
          <div className="card">
            <div className="skeleton w-32 h-5 mb-4" />
            <div className="grid grid-cols-2 gap-3">
              {[0, 1, 2, 3].map((i) => (
                <div key={i} className="skeleton h-20 rounded-xl" />
              ))}
            </div>
          </div>
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          <div className="card">
            <div className="skeleton w-36 h-5 mb-4" />
            <div className="flex gap-2 h-28">
              {[0, 1, 2, 3, 4].map((i) => (
                <div key={i} className="flex-1 skeleton rounded-xl" />
              ))}
            </div>
          </div>
          <div className="card space-y-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="flex items-center gap-3">
                <div className="skeleton w-8 h-8 rounded-lg" />
                <div className="flex-1 space-y-1.5">
                  <div className="skeleton w-20 h-2.5" />
                  <div className="skeleton w-full h-1.5 rounded-full" />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <motion.div variants={stagger} initial="hidden" animate="show">

        {/* ── Row 1: Carbon Score + Equivalents ────────────────────── */}
        <motion.div variants={fadeUp} className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
          <div className="card flex flex-col items-center py-6">
            <div className="flex items-center gap-2 mb-4">
              <Sparkles className="w-3.5 h-3.5 text-emerald-500" />
              <p className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">
                Your Carbon Score
              </p>
            </div>
            <CarbonScore kg={totalKg} />
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1.2 }}
              className="mt-4 flex flex-wrap items-center justify-center gap-2 text-sm text-ink-muted"
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
          </div>

          {hasData ? (
            <div className="card">
              <SectionHeader
                icon={<Leaf className="w-4 h-4 text-emerald-600" />}
                title="What does this actually mean?"
                subtitle="Your carbon in terms you can relate to"
              />
              <ImpactEquivalents equivalents={equivalents} totalKg={totalKg} />
            </div>
          ) : (
            <div className="card flex items-center justify-center">
              <div className="text-center py-6">
                <motion.div
                  className="w-16 h-16 rounded-2xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/50 flex items-center justify-center mx-auto mb-4"
                  animate={{ y: [0, -4, 0] }}
                  transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                >
                  <span className="text-3xl">📊</span>
                </motion.div>
                <h3 className="text-base font-semibold text-ink mb-1">Your insights await</h3>
                <p className="text-xs text-ink-muted max-w-[200px] mx-auto leading-relaxed">
                  Upload a receipt to see your carbon score and equivalents.
                </p>
              </div>
            </div>
          )}
        </motion.div>

        {/* ── Row 2: Monthly Trend + Category Breakdown ────────────── */}
        <motion.div variants={fadeUp} className="grid grid-cols-1 lg:grid-cols-2 gap-4 mb-4">
          {monthly.length > 0 && (
            <div className="card">
              <SectionHeader
                icon={<BarChart3 className="w-4 h-4 text-emerald-600" />}
                title="How you're trending"
                subtitle="Your emissions over time"
              />
              <MonthlyTrend monthly={monthly} maxKg={maxMonthKg} />
            </div>
          )}

          {categories.length > 0 && (
            <div className="card">
              <SectionHeader
                icon={<MessageCircle className="w-4 h-4 text-emerald-600" />}
                title="Where your emissions come from"
                subtitle="The categories that matter most"
              />
              <CategoryBreakdown categories={categories} maxKg={maxCatKg} />
            </div>
          )}
        </motion.div>

        {/* ── Row 3: Achievements + Activity Timeline ──────────────── */}
        <motion.div variants={fadeUp} className="grid grid-cols-1 lg:grid-cols-2 gap-4">
          {achievements.length > 0 && (
            <div className="card">
              <SectionHeader
                icon={<Trophy className="w-4 h-4 text-amber-600" />}
                title="What I'm proud of you for"
                subtitle="Milestones worth celebrating"
              />
              <AchievementBadges achievements={achievements} />
            </div>
          )}

          {activities.length > 0 && (
            <div className="card">
              <SectionHeader
                icon={<Clock className="w-4 h-4 text-emerald-600" />}
                title="Your recent activities"
                subtitle="What I've been tracking"
              />
              <ActivityTimeline activities={activities.slice(0, 6)} />
            </div>
          )}
        </motion.div>

      </motion.div>
    </div>
  )
}
