import { motion } from 'framer-motion'
import { Trophy } from 'lucide-react'

export default function AchievementBadges({ achievements }: { achievements: string[] }) {
  return (
    <div className="flex flex-wrap gap-2">
      {achievements.map((a, i) => (
        <motion.div
          key={i}
          initial={{ opacity: 0, scale: 0.8 }}
          animate={{ opacity: 1, scale: 1 }}
          transition={{ delay: 0.3 + i * 0.08, type: 'spring', stiffness: 300 }}
          whileHover={{ scale: 1.05, y: -2 }}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-50 border border-amber-100 text-xs font-medium text-amber-700"
        >
          <Trophy className="w-3 h-3" />
          {a}
        </motion.div>
      ))}
    </div>
  )
}
