import { useEffect, useState, useCallback } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, Sparkles, MessageCircle, Flame, Activity, TrendingUp } from 'lucide-react'
import { getCarbonAnalytics, getCarbonInsights, getAICoach } from '@/api/services'
import type { CarbonAnalyticsResponse, CarbonInsightResponse, AICarbonCoachResponse, TopEmissionActivity } from '@/types/activity'
import {
  StatusBar,
  ActionCard,
  useActionRecommendations,
  CarbonJourney,
} from '@/components/home'
import EmptyStateCard from '@/components/common/EmptyStateCard'

const stagger = {
  hidden: { opacity: 0 },
  show: { opacity: 1, transition: { staggerChildren: 0.06 } },
}

const fadeUp = {
  hidden: { opacity: 0, y: 12 },
  show: { opacity: 1, y: 0, transition: { duration: 0.4, ease: [0.22, 1, 0.36, 1] as const } },
}

const categoryConfig: Record<string, { icon: React.ReactNode; color: string; bg: string }> = {
  ELECTRICITY: { icon: <Activity className="w-4 h-4" />, color: 'text-amber-600', bg: 'bg-amber-50' },
  FUEL:        { icon: <Flame className="w-4 h-4" />, color: 'text-orange-600', bg: 'bg-orange-50' },
  FOOD:        { icon: <span className="text-sm">🍽️</span>, color: 'text-emerald-600', bg: 'bg-emerald-50' },
  TRANSPORT:   { icon: <span className="text-sm">🚗</span>, color: 'text-teal-600', bg: 'bg-teal-50' },
  OTHER:       { icon: <Activity className="w-4 h-4" />, color: 'text-gray-600', bg: 'bg-gray-50' },
}

function getTimeAgo(dateStr: string): string {
  const date = new Date(dateStr)
  const now = new Date()
  const diffMs = now.getTime() - date.getTime()
  const diffMins = Math.floor(diffMs / 60000)
  if (diffMins < 1) return 'Just now'
  if (diffMins < 60) return `${diffMins}m ago`
  const diffHours = Math.floor(diffMins / 60)
  if (diffHours < 24) return `${diffHours}h ago`
  const diffDays = Math.floor(diffHours / 24)
  if (diffDays === 1) return 'Yesterday'
  return `${diffDays}d ago`
}

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function Home() {
  const navigate = useNavigate()
  const [analytics, setAnalytics] = useState<CarbonAnalyticsResponse | null>(null)
  const [insights, setInsights] = useState<CarbonInsightResponse | null>(null)
  const [coach, setCoach] = useState<AICarbonCoachResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const fetchData = useCallback(async (signal?: AbortSignal) => {
    setLoading(true)
    setError(false)
    try {
      const [aRes, iRes, cRes] = await Promise.all([
        getCarbonAnalytics(undefined, signal),
        getCarbonInsights(undefined, signal),
        getAICoach(undefined, signal),
      ])
      if (aRes.success && aRes.data) setAnalytics(aRes.data)
      if (iRes.success && iRes.data) setInsights(iRes.data)
      if (cRes.success && cRes.data) setCoach(cRes.data)
    } catch (err) {
      if (err instanceof Error && err.name === 'AbortError') return
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    fetchData(controller.signal)
    return () => controller.abort()
  }, [fetchData])

  const actions = useActionRecommendations(analytics, insights, coach)
  const heroAction = actions[0] ?? null
  const quickActions = actions.slice(1, 4)
  const activities = analytics?.topActivities?.slice(0, 3) ?? []
  const totalKg = Number(analytics?.totalCarbonKg ?? 0)
  const avgDaily = Number(analytics?.averageDailyKg ?? 0)
  const activityCount = analytics?.activityCount ?? 0
  const topCategory = analytics?.categoryTotals?.[0] ?? null

  const hasData = activityCount > 0
  const greeting = getGreeting()

  return (
    <motion.div variants={stagger} initial="hidden" animate="show">

      {/* Error Banner */}
      {error && (
        <div className="mb-5 p-4 bg-red-50 border border-red-100 rounded-xl text-sm flex items-center justify-between text-red-800">
          <span>Failed to load carbon dashboard data. Please try again.</span>
          <button
            onClick={() => fetchData()}
            className="px-3 py-1 bg-red-100 hover:bg-red-200 font-semibold rounded-md transition-colors"
          >
            Retry
          </button>
        </div>
      )}

      {/* ── 1. HERO GREETING ────────────────────────────────────────── */}
      <motion.div variants={fadeUp} className="mb-4">
        <h1 className="text-2xl sm:text-3xl font-bold text-ink tracking-tight">
          {greeting} <span className="inline-block ml-1">🌱</span>
        </h1>
        <p className="text-sm text-ink-muted mt-1">
          {hasData
            ? "Here's your carbon footprint at a glance."
            : 'Upload your first receipt to get started.'}
        </p>
      </motion.div>

      {/* ── 2. TODAY'S CARBON (prominent) ───────────────────────────── */}
      <motion.div variants={fadeUp} className="mb-5">
        <StatusBar data={analytics} loading={loading} />
      </motion.div>

      {/* ── 3. AI RECOMMENDATION (hero action) ──────────────────────── */}
      <motion.div variants={fadeUp} className="mb-5">
        {loading ? (
          <div className="py-6">
            <div className="flex items-center gap-4">
              <div className="skeleton w-14 h-14 rounded-2xl" />
              <div className="flex-1 space-y-2">
                <div className="skeleton w-24 h-3" />
                <div className="skeleton w-48 h-5" />
                <div className="skeleton w-64 h-3" />
              </div>
            </div>
          </div>
        ) : heroAction ? (
          <ActionCard action={heroAction} index={0} featured />
        ) : (
          <div className="rounded-2xl bg-gradient-to-br from-emerald-50 via-card to-leaf-50 border border-emerald-100/60 p-4">
            <div className="flex items-center gap-3">
              <div className="w-11 h-11 rounded-xl bg-white/80 border border-emerald-100/50 flex items-center justify-center shadow-sm">
                <span className="text-xl">🌱</span>
              </div>
              <div>
                <p className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">Welcome</p>
                <h2 className="text-sm font-bold text-ink">
                  {hasData ? 'Looking good!' : 'Ready to get started?'}
                </h2>
                <p className="text-xs text-ink-muted">
                  {hasData
                    ? 'Upload more receipts for personalized action recommendations.'
                    : 'Upload a receipt and I will find your biggest carbon reduction opportunities.'}
                </p>
              </div>
            </div>
          </div>
        )}
      </motion.div>

      {/* ── 4. QUICK ACTIONS (secondary) ────────────────────────────── */}
      {quickActions.length > 0 && (
        <motion.div variants={fadeUp} className="mb-5">
          <div className="flex items-center gap-2 mb-2">
            <Sparkles className="w-3.5 h-3.5 text-emerald-500" />
            <h2 className="text-xs font-semibold text-ink uppercase tracking-wider">
              What you can do next
            </h2>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5">
            {quickActions.map((action, i) => (
              <ActionCard key={action.id} action={action} index={i + 1} />
            ))}
          </div>
        </motion.div>
      )}

      {/* ── 4.5. CARBON JOURNEY ───────────────────────────────────── */}
      <motion.div variants={fadeUp} className="mb-5">
        <CarbonJourney analytics={analytics} coach={coach} loading={loading} />
      </motion.div>

      {/* ── 5. RECENT ACTIVITY ──────────────────────────────────────── */}
      <motion.div variants={fadeUp} className="mb-5">
        <div className="card">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-xl bg-emerald-50 flex items-center justify-center">
                <MessageCircle className="w-4 h-4 text-emerald-600" />
              </div>
              <div>
                <h3 className="text-sm font-semibold text-ink">Recent activity</h3>
                <p className="text-[10px] text-ink-faint">
                  {hasData ? `${activities.length} of ${activityCount}` : 'No activity yet'}
                </p>
              </div>
            </div>
            {hasData && (
              <button
                onClick={() => navigate('/history')}
                className="text-xs font-medium text-emerald-600 hover:text-emerald-700 flex items-center gap-1"
              >
                View all <ArrowRight className="w-3 h-3" />
              </button>
            )}
          </div>

          {loading ? (
            <div className="space-y-3 py-2 animate-pulse">
              {[0, 1, 2].map((i) => (
                <div key={i} className="flex items-center gap-3">
                  <div className="skeleton w-9 h-9 rounded-xl" />
                  <div className="flex-1 space-y-1.5">
                    <div className="skeleton w-28 h-3" />
                    <div className="skeleton w-20 h-2.5" />
                  </div>
                  <div className="skeleton w-12 h-4" />
                </div>
              ))}
            </div>
          ) : !hasData ? (
            <EmptyStateCard
              emoji="📄"
              title="No activities yet"
              description="Upload a receipt to see your activity feed and track your carbon impact."
              size="sm"
              action={
                <motion.button
                  onClick={() => navigate('/upload')}
                  whileHover={{ scale: 1.03, y: -1 }}
                  whileTap={{ scale: 0.97 }}
                  className="btn-primary text-sm inline-flex items-center gap-2"
                >
                  Upload receipt
                  <ArrowRight className="w-4 h-4" />
                </motion.button>
              }
            />
          ) : (
            <div className="space-y-0">
              {activities.map((activity: TopEmissionActivity) => {
                const cat = activity.category as string
                const config = categoryConfig[cat] ?? categoryConfig.OTHER
                const label = cat.charAt(0) + cat.slice(1).toLowerCase()

                return (
                  <div
                    key={activity.activityId}
                    className="flex items-center gap-3 py-2.5 border-b border-border-light/50 last:border-0"
                  >
                    <div className={`w-9 h-9 rounded-xl ${config.bg} flex items-center justify-center flex-shrink-0`}>
                      {config.icon}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-ink truncate">
                        {activity.merchant || label}
                      </p>
                      <p className="text-[10px] text-ink-faint">
                        {label} · {getTimeAgo(activity.occurredAt)}
                      </p>
                    </div>
                    <span className="text-sm font-bold text-ink">
                      {Number(activity.carbonKg ?? 0).toFixed(1)} kg
                    </span>
                  </div>
                )
              })}
            </div>
          )}
        </div>
      </motion.div>

      {/* ── 6. SUPPORTING METRICS (compact row) ─────────────────────── */}
      {loading ? (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5 mb-5">
          {[0, 1, 2, 3].map((i) => (
            <motion.div
              key={i}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: i * 0.05 }}
              className="h-20 skeleton"
            />
          ))}
        </div>
      ) : hasData && (
        <motion.div variants={fadeUp} className="mb-5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
            <div className="rounded-xl bg-gradient-to-br from-emerald-50/80 to-leaf-50/40 border border-emerald-100/60 p-3 text-center">
              <TrendingUp className="w-4 h-4 text-emerald-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-ink leading-none">{totalKg.toFixed(1)}</p>
              <p className="text-[10px] text-ink-faint mt-0.5">Total kg</p>
            </div>
            <div className="rounded-xl bg-gradient-to-br from-amber-50/80 to-orange-50/40 border border-amber-100/60 p-3 text-center">
              <Activity className="w-4 h-4 text-amber-500 mx-auto mb-1" />
              <p className="text-lg font-bold text-ink leading-none">{avgDaily.toFixed(1)}</p>
              <p className="text-[10px] text-ink-faint mt-0.5">Daily avg</p>
            </div>
            <div className="rounded-xl bg-gradient-to-br from-orange-50/80 to-red-50/40 border border-orange-100/60 p-3 text-center">
              <Flame className="w-4 h-4 text-orange-500 mx-auto mb-1" />
              <p className="text-sm font-bold text-ink leading-none truncate">
                {topCategory
                  ? topCategory.category.charAt(0) + topCategory.category.slice(1).toLowerCase()
                  : '—'}
              </p>
              <p className="text-[10px] text-ink-faint mt-0.5">Top source</p>
            </div>
            <div className="rounded-xl bg-gradient-to-br from-teal-50/80 to-emerald-50/40 border border-teal-100/60 p-3 text-center">
              <span className="text-sm">📄</span>
              <p className="text-lg font-bold text-ink leading-none mt-0.5">{activityCount}</p>
              <p className="text-[10px] text-ink-faint mt-0.5">Activities</p>
            </div>
          </div>
        </motion.div>
      )}

      {/* ── 7. COACH CTA ────────────────────────────────────────────── */}
      {loading ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="h-16 skeleton mb-5"
        />
      ) : hasData && (
        <motion.div variants={fadeUp}>
          <button
            onClick={() => navigate('/coach')}
            className="w-full rounded-xl bg-gradient-to-r from-emerald-50 to-leaf-50 border border-emerald-100/60 p-3.5 flex items-center gap-3 text-left transition-all hover:shadow-sm"
          >
            <div className="w-9 h-9 rounded-full bg-leaf-100 flex items-center justify-center flex-shrink-0">
              <span className="text-base">🌱</span>
            </div>
            <div className="flex-1">
              <h3 className="text-sm font-semibold text-ink">Talk to EcoBuddy</h3>
              <p className="text-[10px] text-ink-faint">
                Personalized coaching based on your {activityCount} activities
              </p>
            </div>
            <div className="flex items-center gap-1 text-emerald-600">
              <span className="text-xs font-medium">Open</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </div>
          </button>
        </motion.div>
      )}

    </motion.div>
  )
}
