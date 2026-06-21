import { useEffect, useState } from 'react'
import { motion, useSpring, useTransform, type MotionValue, useMotionValueEvent } from 'framer-motion'
import { TrendingDown, TrendingUp, Minus, Leaf } from 'lucide-react'
import type { CarbonAnalyticsResponse } from '@/types/activity'

function MotionValueText({ motionValue, className }: { motionValue: MotionValue<string>; className?: string }) {
  const [text, setText] = useState('0')
  useMotionValueEvent(motionValue, 'change', (latest) => setText(latest))
  return <span className={className}>{text}</span>
}

function useCountUp(target: number, delay = 0.2) {
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

function getTrendInfo(data: CarbonAnalyticsResponse) {
  const monthly = data.monthlyTrend ?? []
  if (monthly.length < 2) return null
  const current = Number(monthly[monthly.length - 1]?.carbonKg ?? 0)
  const previous = Number(monthly[monthly.length - 2]?.carbonKg ?? 0)
  if (previous === 0) return null
  const change = ((current - previous) / previous) * 100
  return { change, isUp: change > 0, isFlat: Math.abs(change) < 5 }
}

interface StatusBarProps {
  data: CarbonAnalyticsResponse | null
  loading: boolean
}

export default function StatusBar({ data, loading }: StatusBarProps) {
  const totalKg = Number(data?.totalCarbonKg ?? 0)
  const avgDaily = Number(data?.averageDailyKg ?? 0)
  const activityCount = data?.activityCount ?? 0

  const countTotal = useCountUp(totalKg, 0.2)
  const countDaily = useCountUp(avgDaily, 0.3)

  const trend = data ? getTrendInfo(data) : null

  if (loading) {
    return (
      <div className="rounded-2xl bg-cream-50/40 border border-border-light p-5 animate-pulse">
        <div className="flex justify-between items-start gap-4">
          <div className="flex-1 space-y-2">
            <div className="h-4 bg-gray-200 rounded w-1/3" />
            <div className="h-8 bg-gray-200 rounded w-1/2" />
            <div className="h-3 bg-gray-200 rounded w-1/4" />
          </div>
          <div className="space-y-2 text-right">
            <div className="h-3 bg-gray-200 rounded w-16 ml-auto" />
            <div className="h-6 bg-gray-200 rounded w-12 ml-auto" />
            <div className="h-4 bg-gray-200 rounded w-20 ml-auto" />
          </div>
        </div>
      </div>
    )
  }

  if (!data || activityCount === 0) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center gap-3 px-4 py-3 rounded-xl bg-cream-50/60 border border-border-light"
      >
        <div className="w-2 h-2 rounded-full bg-amber-400 animate-pulse" />
        <p className="text-xs text-ink-muted">
          No data yet — <span className="text-emerald-600 font-medium">upload a receipt</span> to see your footprint
        </p>
      </motion.div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="rounded-2xl bg-gradient-to-br from-emerald-50 via-card to-leaf-50 border border-emerald-100/60 p-5"
    >
      <div className="flex items-start justify-between gap-4">
        {/* Left: Main number */}
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-1">
            <Leaf className="w-4 h-4 text-emerald-600" />
            <span className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider">
              Your Carbon Footprint
            </span>
          </div>
          <div className="flex items-baseline gap-2">
            <span className="text-4xl font-bold text-ink tracking-tight">
              <MotionValueText motionValue={countTotal} />
            </span>
            <span className="text-sm text-ink-muted font-medium">kg CO₂e</span>
          </div>
          <p className="text-xs text-ink-faint mt-1">
            Total across {activityCount} activit{activityCount === 1 ? 'y' : 'ies'}
          </p>
        </div>

        {/* Right: Stats */}
        <div className="flex flex-col items-end gap-3">
          {/* Daily average */}
          <div className="text-right">
            <p className="text-[10px] text-ink-faint uppercase tracking-wider mb-0.5">Daily avg</p>
            <div className="flex items-baseline gap-1">
              <span className="text-xl font-bold text-ink">
                <MotionValueText motionValue={countDaily} />
              </span>
              <span className="text-xs text-ink-muted">kg</span>
            </div>
          </div>

          {/* Trend */}
          {trend && (
            <div className="flex items-center gap-1.5">
              {trend.isFlat ? (
                <Minus className="w-3.5 h-3.5 text-gray-400" />
              ) : trend.isUp ? (
                <TrendingUp className="w-3.5 h-3.5 text-amber-500" />
              ) : (
                <TrendingDown className="w-3.5 h-3.5 text-emerald-500" />
              )}
              <span className={`text-xs font-semibold ${trend.isFlat ? 'text-gray-500' : trend.isUp ? 'text-amber-600' : 'text-emerald-600'}`}>
                {trend.isFlat ? 'Steady' : `${trend.isUp ? '+' : ''}${trend.change.toFixed(0)}%`}
              </span>
              <span className="text-[10px] text-ink-faint">vs last month</span>
            </div>
          )}
        </div>
      </div>
    </motion.div>
  )
}
