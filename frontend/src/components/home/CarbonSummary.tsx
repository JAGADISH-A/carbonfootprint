import { useEffect, useState } from 'react'
import { motion, useSpring, useTransform } from 'framer-motion'
import { TrendingDown, TrendingUp, Sparkles } from 'lucide-react'
import { getCarbonAnalytics } from '@/api/services'
import type { CarbonAnalyticsResponse } from '@/types/activity'

function useCountUp(target: number, _duration = 1.2, delay = 0.3) {
  const safeTarget = Number(target ?? 0) || 0
  const spring = useSpring(0, { stiffness: 40, damping: 15, mass: 1 })
  const display = useTransform(spring, (v) => Number(v ?? 0).toFixed(1))

  useEffect(() => {
    const timeout = setTimeout(() => {
      spring.set(safeTarget)
    }, delay * 1000)
    return () => clearTimeout(timeout)
  }, [safeTarget, spring, delay])

  return display
}

function CircularProgress({ percentage }: { percentage: number }) {
  const radius = 58
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (percentage / 100) * circumference

  return (
    <div className="relative w-32 h-32">
      <svg className="w-full h-full -rotate-90" viewBox="0 0 128 128">
        <circle
          cx="64"
          cy="64"
          r={radius}
          fill="none"
          stroke="#E8F0E8"
          strokeWidth="8"
        />
        <motion.circle
          cx="64"
          cy="64"
          r={radius}
          fill="none"
          stroke="url(#progressGradient)"
          strokeWidth="8"
          strokeLinecap="round"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 1.5, ease: [0.22, 1, 0.36, 1], delay: 0.4 }}
        />
        <defs>
          <linearGradient id="progressGradient" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#10B981" />
            <stop offset="100%" stopColor="#34D399" />
          </linearGradient>
        </defs>
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <motion.span
          className="text-2xl font-bold text-ink"
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.8, duration: 0.4, type: 'spring', stiffness: 200 }}
        >
          {percentage}%
        </motion.span>
        <span className="text-[10px] text-ink-faint">daily goal</span>
      </div>
    </div>
  )
}

export default function CarbonSummary() {
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

  const totalCarbon = Number(data?.totalCarbonKg ?? 0)
  const avgDaily = Number(data?.averageDailyKg ?? 0)
  const activityCount = Number(data?.activityCount ?? 0)
  const hasData = activityCount > 0

  const displayCarbon = useCountUp(totalCarbon, 1.2, 0.5)
  const percentage = hasData ? Math.min(Math.round((avgDaily / 25) * 100), 100) : 0

  const coachMessage = hasData
    ? avgDaily < 5
      ? "You're doing really well — your daily average is under 5 kg. That's below most benchmarks."
      : avgDaily < 15
        ? "Your daily average is moderate. There's room to bring it down, but you're not far off."
        : "Your daily average is on the higher side. Let's see where we can make some easy cuts."
    : "Once you upload a receipt, I'll start tracking your footprint and giving you personalized tips."

  if (loading) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
        className="card flex flex-col items-center py-6"
      >
        <div className="skeleton w-36 h-36 rounded-full mb-6" />
        <div className="skeleton w-40 h-8 mb-3" />
        <div className="skeleton w-56 h-4" />
      </motion.div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
      className="card flex flex-col items-center text-center py-6"
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.3, duration: 0.4 }}
        className="flex items-center gap-2 mb-4"
      >
        <Sparkles className="w-3.5 h-3.5 text-emerald-500" />
        <p className="text-xs font-semibold text-emerald-600 uppercase tracking-wider">
          Your Footprint
        </p>
      </motion.div>

      <CircularProgress percentage={percentage} />

      <div className="mt-6 mb-2 flex items-baseline gap-1.5">
        <motion.span
          className="text-4xl font-bold text-ink tracking-tight"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.5, duration: 0.4 }}
        >
          {displayCarbon}
        </motion.span>
        <span className="text-base text-ink-faint">kg CO₂e</span>
      </div>

      {hasData ? (
        <motion.div
          initial={{ opacity: 0, y: 4 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1, duration: 0.4 }}
          className="flex items-center gap-1.5 mb-3"
        >
          <motion.div
            animate={{ y: [0, -2, 0] }}
            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
          >
            {avgDaily < 20 ? (
              <TrendingDown className="w-4 h-4 text-emerald-500" />
            ) : (
              <TrendingUp className="w-4 h-4 text-amber-500" />
            )}
          </motion.div>
          <span className={`text-sm font-medium ${avgDaily < 20 ? 'text-emerald-600' : 'text-amber-600'}`}>
            ~{avgDaily.toFixed(1)} kg/day average
          </span>
        </motion.div>
      ) : (
        <div className="flex items-center gap-1.5 mb-3">
          <motion.span
            animate={{ y: [0, -2, 0] }}
            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
          >
            <TrendingUp className="w-4 h-4 text-emerald-400" />
          </motion.span>
          <span className="text-sm text-ink-muted">Awaiting your first receipt</span>
        </div>
      )}

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1.2, duration: 0.5 }}
        className="text-sm text-ink-muted max-w-xs leading-relaxed"
      >
        {coachMessage}
      </motion.p>
    </motion.div>
  )
}
