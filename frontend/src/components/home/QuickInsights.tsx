import { useEffect, useState } from 'react'
import { motion, useSpring, useTransform } from 'framer-motion'
import { Activity, Flame, Calendar } from 'lucide-react'
import { getCarbonAnalytics } from '@/api/services'
import type { CarbonAnalyticsResponse } from '@/types/activity'

function useCountUp(target: number, _duration = 1, delay = 0.2) {
  const safeTarget = Number(target ?? 0) || 0
  const spring = useSpring(0, { stiffness: 50, damping: 18 })
  const display = useTransform(spring, (v) => {
    if (safeTarget % 1 === 0) return Math.round(v).toString()
    return Number(v ?? 0).toFixed(1)
  })

  useEffect(() => {
    const timeout = setTimeout(() => spring.set(safeTarget), delay * 1000)
    return () => clearTimeout(timeout)
  }, [safeTarget, spring, delay])

  return display
}

interface InsightCardProps {
  icon: React.ReactNode
  label: string
  value: string | ReturnType<typeof useCountUp>
  isNumeric: boolean
  subtext: string
  delay: number
}

function InsightCard({ icon, label, value, isNumeric, subtext, delay }: InsightCardProps) {
  const countVal = isNumeric ? (value as ReturnType<typeof useCountUp>) : null
  const textVal = !isNumeric ? (value as string) : null

  return (
    <motion.div
      initial={{ opacity: 0, y: 14 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -3, transition: { duration: 0.25, ease: 'easeOut' } }}
      className="card-interactive flex items-center gap-4"
    >
      <motion.div
        className="w-11 h-11 rounded-2xl bg-emerald-50 flex items-center justify-center flex-shrink-0"
        whileHover={{ rotate: [0, -10, 10, 0], scale: 1.1 }}
        transition={{ duration: 0.4 }}
      >
        <div className="text-emerald-600">{icon}</div>
      </motion.div>
      <div className="min-w-0">
        <p className="text-[11px] font-medium text-ink-faint uppercase tracking-wider mb-0.5">{label}</p>
        <p className="text-lg font-bold text-ink leading-tight">
          {isNumeric && countVal ? <motion.span>{countVal}</motion.span> : textVal}
        </p>
        <p className="text-xs text-ink-muted mt-0.5 truncate">{subtext}</p>
      </div>
    </motion.div>
  )
}

export default function QuickInsights() {
  const [data, setData] = useState<CarbonAnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getCarbonAnalytics()
      .then((res) => {
        if (res.success && res.data) {
          setData(res.data)
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  const activityCount = Number(data?.activityCount ?? 0)
  const topCategory = data?.categoryTotals?.[0] ?? null
  const avgDaily = Number(data?.averageDailyKg ?? 0)
  const totalCarbon = Number(data?.totalCarbonKg ?? 0)

  const countActivity = useCountUp(activityCount, 1, 0.3)
  const countMonthly = useCountUp(totalCarbon, 1, 0.4)

  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {[0, 1, 2].map((i) => (
          <motion.div
            key={i}
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: i * 0.1 }}
            className="card"
          >
            <div className="flex items-center gap-4">
              <div className="skeleton w-11 h-11 rounded-2xl" />
              <div className="space-y-2 flex-1">
                <div className="skeleton w-16 h-3" />
                <div className="skeleton w-20 h-6" />
                <div className="skeleton w-24 h-3" />
              </div>
            </div>
          </motion.div>
        ))}
      </div>
    )
  }

  const topCatLabel = topCategory
    ? topCategory.category.charAt(0) + topCategory.category.slice(1).toLowerCase()
    : '—'

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      <InsightCard
        icon={<Activity className="w-5 h-5" />}
        label="Activities"
        value={countActivity}
        isNumeric={activityCount > 0}
        subtext={activityCount > 0 ? 'tracked so far' : 'Your journey awaits'}
        delay={0}
      />
      <InsightCard
        icon={<Flame className="w-5 h-5" />}
        label="Biggest Source"
        value={topCatLabel}
        isNumeric={false}
        subtext={topCategory ? `${Number(topCategory.carbonKg ?? 0).toFixed(1)} kg CO₂e` : 'Upload to discover'}
        delay={0.06}
      />
      <InsightCard
        icon={<Calendar className="w-5 h-5" />}
        label="Monthly Carbon"
        value={activityCount > 0 ? countMonthly : '—'}
        isNumeric={activityCount > 0}
        subtext={activityCount > 0 ? `~${avgDaily.toFixed(1)} kg/day average` : 'Your impact will appear here'}
        delay={0.12}
      />
    </div>
  )
}
