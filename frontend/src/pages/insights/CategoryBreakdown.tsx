import { motion } from 'framer-motion'
import type { CategoryEmissionSummary } from '@/types/activity'
import { categoryConfig } from './constants'

export default function CategoryBreakdown({
  categories,
  maxKg,
}: {
  categories: CategoryEmissionSummary[]
  maxKg: number
}) {
  return (
    <div className="space-y-3">
      {categories.map((cat, i) => {
        const config = categoryConfig[cat.category] ?? categoryConfig.OTHER
        const width = maxKg > 0 ? (cat.carbonKg / maxKg) * 100 : 0
        const label = cat.category.charAt(0) + cat.category.slice(1).toLowerCase()

        return (
          <motion.div
            key={cat.category}
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 + i * 0.06, duration: 0.4 }}
            className="flex items-center gap-3"
          >
            <div className={`w-9 h-9 rounded-xl ${config.bg} flex items-center justify-center flex-shrink-0`}>
              <span className={config.color}>{config.icon}</span>
            </div>
            <div className="flex-1 min-w-0">
              <div className="flex items-center justify-between mb-1">
                <span className="text-sm font-medium text-ink truncate">{label}</span>
                <span className="text-sm font-semibold text-ink ml-2">{Number(cat.carbonKg ?? 0).toFixed(1)} kg</span>
              </div>
              <div className="h-2 bg-surface rounded-full overflow-hidden">
                <motion.div
                  className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-emerald-500"
                  initial={{ width: 0 }}
                  animate={{ width: `${Math.max(width, 2)}%` }}
                  transition={{ duration: 0.8, delay: 0.5 + i * 0.08, ease: [0.22, 1, 0.36, 1] }}
                />
              </div>
            </div>
            <span className="text-xs text-ink-faint w-8 text-right">{Math.round(cat.percentageOfTotal)}%</span>
          </motion.div>
        )
      })}
    </div>
  )
}
