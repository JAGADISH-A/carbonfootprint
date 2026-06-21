import { motion } from 'framer-motion'

interface CarbonScoreProps {
  kg: number
  max?: number
  weeklyDifference?: number | null
}

export default function CarbonScore({ kg, max = 50, weeklyDifference }: CarbonScoreProps) {
  const radius = 70
  const circumference = 2 * Math.PI * radius
  const percentage = Math.min((kg / max) * 100, 100)
  const offset = circumference - (percentage / 100) * circumference

  const scoreColor = percentage < 30 ? '#10B981' : percentage < 60 ? '#F59E0B' : '#EF4444'
  const label = percentage < 20 ? 'Excellent' : percentage < 40 ? 'Good' : percentage < 60 ? 'Moderate' : 'High'

  return (
    <div className="relative w-44 h-44 mx-auto">
      <svg className="w-full h-full -rotate-90" viewBox="0 0 160 160">
        <circle cx="80" cy="80" r={radius} fill="none" stroke="#E8F0E8" strokeWidth="10" />
        <motion.circle
          cx="80" cy="80" r={radius} fill="none" stroke={scoreColor}
          strokeWidth="10" strokeLinecap="round" strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration: 1.5, ease: [0.22, 1, 0.36, 1], delay: 0.3 }}
        />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <motion.span
          className="text-3xl font-bold text-ink"
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.8, type: 'spring', stiffness: 200 }}
        >
          {Number(kg ?? 0).toFixed(1)}
        </motion.span>
        <span className="text-xs text-ink-faint mt-0.5">kg CO₂e</span>
        <motion.span
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1 }}
          className="text-[10px] font-semibold uppercase tracking-wider mt-1"
          style={{ color: scoreColor }}
        >
          {label}
        </motion.span>
        {weeklyDifference != null && weeklyDifference !== 0 && (
          <motion.span
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 1.3 }}
            className={`text-[10px] font-semibold mt-1 ${weeklyDifference < 0 ? 'text-emerald-600' : 'text-red-500'}`}
          >
            {weeklyDifference < 0 ? '▼' : '▲'} {weeklyDifference < 0 ? '' : '+'}{weeklyDifference.toFixed(1)} this week
          </motion.span>
        )}
        {weeklyDifference != null && weeklyDifference === 0 && (
          <motion.span
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 1.3 }}
            className="text-[10px] font-medium text-ink-faint mt-1"
          >
            No change
          </motion.span>
        )}
      </div>
    </div>
  )
}
