import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import {
  Zap,
  Flame,
  PlaneTakeoff,
  ShoppingBag,
  UtensilsCrossed,
  Car,
  Droplets,
  Home,
  ArrowRight,
  MessageCircle,
} from 'lucide-react'
import { getCarbonAnalytics } from '@/api/services'
import type { CarbonAnalyticsResponse, TopEmissionActivity } from '@/types/activity'

const categoryConfig: Record<string, { icon: React.ReactNode; color: string; bg: string }> = {
  ELECTRICITY: { icon: <Zap className="w-4 h-4" />, color: 'text-amber-600', bg: 'bg-amber-50' },
  FUEL:        { icon: <Flame className="w-4 h-4" />, color: 'text-orange-600', bg: 'bg-orange-50' },
  FLIGHT:      { icon: <PlaneTakeoff className="w-4 h-4" />, color: 'text-blue-600', bg: 'bg-blue-50' },
  SHOPPING:    { icon: <ShoppingBag className="w-4 h-4" />, color: 'text-purple-600', bg: 'bg-purple-50' },
  FOOD:        { icon: <UtensilsCrossed className="w-4 h-4" />, color: 'text-emerald-600', bg: 'bg-emerald-50' },
  TRANSPORT:   { icon: <Car className="w-4 h-4" />, color: 'text-teal-600', bg: 'bg-teal-50' },
  GAS:         { icon: <Flame className="w-4 h-4" />, color: 'text-red-600', bg: 'bg-red-50' },
  WATER:       { icon: <Droplets className="w-4 h-4" />, color: 'text-cyan-600', bg: 'bg-cyan-50' },
  ACCOMMODATION: { icon: <Home className="w-4 h-4" />, color: 'text-indigo-600', bg: 'bg-indigo-50' },
  OTHER:       { icon: <ShoppingBag className="w-4 h-4" />, color: 'text-gray-600', bg: 'bg-gray-50' },
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

function ActivitySkeleton() {
  return (
    <div className="flex items-center gap-4 py-4">
      <div className="skeleton w-10 h-10 rounded-2xl" />
      <div className="flex-1 space-y-2">
        <div className="skeleton w-32 h-3" />
        <div className="skeleton w-20 h-3" />
      </div>
      <div className="skeleton w-16 h-4" />
    </div>
  )
}

export default function RecentActivity() {
  const navigate = useNavigate()
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

  const activities = data?.topActivities ?? []
  const hasData = activities.length > 0

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.25, ease: [0.22, 1, 0.36, 1] }}
      className="card"
    >
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
            <MessageCircle className="w-5 h-5 text-emerald-600" />
          </div>
          <div>
            <h2 className="text-base font-semibold text-ink">What I noticed</h2>
            <p className="text-xs text-ink-muted">Your recent activity breakdown</p>
          </div>
        </div>
        {hasData && (
          <motion.button
            onClick={() => navigate('/history')}
            className="text-sm font-medium text-emerald-600 hover:text-emerald-700 flex items-center gap-1"
            whileHover={{ x: 2 }}
            whileTap={{ scale: 0.97 }}
          >
            View all <ArrowRight className="w-3.5 h-3.5" />
          </motion.button>
        )}
      </div>

      {loading ? (
        <div className="divide-y divide-border-light">
          {[0, 1, 2].map((i) => (
            <ActivitySkeleton key={i} />
          ))}
        </div>
      ) : !hasData ? (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="text-center py-10"
        >
          <motion.div
            className="w-14 h-14 rounded-3xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/50 flex items-center justify-center mx-auto mb-4"
            animate={{ y: [0, -5, 0] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
          >
            <span className="text-2xl">🌱</span>
          </motion.div>
          <p className="text-sm font-medium text-ink mb-1">No activities yet</p>
          <p className="text-xs text-ink-muted max-w-[260px] mx-auto leading-relaxed">
            Upload a receipt and I'll start tracking your carbon footprint.
            Even one activity gives me something to work with.
          </p>
        </motion.div>
      ) : (
        <div className="space-y-0">
          {activities.slice(0, 5).map((activity: TopEmissionActivity, i: number) => {
            const cat = activity.category as string
            const config = categoryConfig[cat] ?? categoryConfig.OTHER
            const label = cat.charAt(0) + cat.slice(1).toLowerCase()

            return (
              <motion.div
                key={activity.activityId}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{
                  duration: 0.4,
                  delay: 0.3 + i * 0.06,
                  ease: [0.22, 1, 0.36, 1],
                }}
                whileHover={{ x: 4, backgroundColor: 'rgba(236,253,245,0.3)' }}
                className="flex items-center gap-4 py-3.5 border-b border-border-light last:border-0 rounded-xl px-2 -mx-2 cursor-default transition-colors"
              >
                <motion.div
                  className={`w-10 h-10 rounded-2xl ${config.bg} flex items-center justify-center flex-shrink-0`}
                  whileHover={{ scale: 1.1, rotate: [0, -5, 5, 0] }}
                  transition={{ duration: 0.3 }}
                >
                  <span className={config.color}>{config.icon}</span>
                </motion.div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-medium text-ink truncate">
                      {activity.merchant || label}
                    </span>
                    <span className="badge-green text-[10px] shrink-0">
                      {label}
                    </span>
                  </div>
                </div>

                <div className="text-right flex-shrink-0">
                  <p className="text-sm font-semibold text-ink">{Number(activity.carbonKg ?? 0).toFixed(1)} kg</p>
                  <p className="text-[11px] text-ink-faint">{getTimeAgo(activity.occurredAt)}</p>
                </div>
              </motion.div>
            )
          })}
        </div>
      )}
    </motion.div>
  )
}
