import { motion } from 'framer-motion'
import type { TopEmissionActivity } from '@/types/activity'
import { categoryConfig } from './constants'

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

export default function ActivityTimeline({
  activities,
}: {
  activities: TopEmissionActivity[]
}) {
  return (
    <div className="space-y-0">
      {activities.map((activity, i) => {
        const config = categoryConfig[activity.category] ?? categoryConfig.OTHER
        const label = activity.category.charAt(0) + activity.category.slice(1).toLowerCase()
        const isLast = i === activities.length - 1

        return (
          <motion.div
            key={activity.activityId}
            initial={{ opacity: 0, x: -12 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 + i * 0.08, duration: 0.4 }}
            className="flex gap-4"
          >
            <div className="flex flex-col items-center">
              <motion.div
                className={`w-9 h-9 rounded-xl ${config.bg} flex items-center justify-center flex-shrink-0 z-10`}
                whileHover={{ scale: 1.1 }}
              >
                <span className={config.color}>{config.icon}</span>
              </motion.div>
              {!isLast && (
                <motion.div
                  className="w-px flex-1 bg-border-light my-1"
                  initial={{ scaleY: 0 }}
                  animate={{ scaleY: 1 }}
                  transition={{ delay: 0.5 + i * 0.08, duration: 0.3 }}
                  style={{ transformOrigin: 'top' }}
                />
              )}
            </div>

            <div className={`flex-1 ${!isLast ? 'pb-5' : ''}`}>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-medium text-ink">
                    {activity.merchant || label}
                  </p>
                  <p className="text-xs text-ink-faint mt-0.5">
                    {label} · {getTimeAgo(activity.occurredAt)}
                  </p>
                </div>
                <span className="text-sm font-bold text-ink">
                  {Number(activity.carbonKg ?? 0).toFixed(1)} kg
                </span>
              </div>
            </div>
          </motion.div>
        )
      })}
    </div>
  )
}
