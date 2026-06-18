import { motion } from 'framer-motion'
import type { MonthlyEmissionTrend } from '@/types/activity'

export default function MonthlyTrend({
  monthly,
  maxKg,
}: {
  monthly: MonthlyEmissionTrend[]
  maxKg: number
}) {
  return (
    <div className="flex items-end gap-2 h-36">
      {monthly.map((m, i) => {
        const height = maxKg > 0 ? (Number(m.carbonKg ?? 0) / maxKg) * 100 : 0
        const shortMonth = m.month.length > 3 ? m.month.slice(0, 3) : m.month

        return (
          <div key={m.month} className="flex flex-col items-center gap-2 flex-1 h-full justify-end">
            <motion.div
              className="text-[10px] text-ink-faint font-medium opacity-0"
              animate={{ opacity: 1 }}
              transition={{ delay: 1.2 + i * 0.05 }}
            >
              {Number(m.carbonKg ?? 0) > 0 ? `${Number(m.carbonKg ?? 0).toFixed(1)}` : ''}
            </motion.div>
            <motion.div
              className="w-full max-w-[36px] rounded-xl relative overflow-hidden"
              style={{ background: 'linear-gradient(to top, #059669, #34D399)' }}
              initial={{ height: 0 }}
              animate={{ height: `${Math.max(height, 4)}%` }}
              transition={{ duration: 0.8, delay: 0.4 + i * 0.1, ease: [0.22, 1, 0.36, 1] }}
            >
              <motion.div
                className="absolute inset-0 bg-gradient-to-t from-emerald-600/20 to-transparent"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 1 + i * 0.1 }}
              />
            </motion.div>
            <span className="text-[10px] text-ink-faint font-medium">{shortMonth}</span>
          </div>
        )
      })}
    </div>
  )
}
