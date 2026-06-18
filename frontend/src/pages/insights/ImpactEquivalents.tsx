import { motion } from 'framer-motion'
import type { Equivalent } from './constants'

export default function ImpactEquivalents({
  equivalents,
  totalKg,
}: {
  equivalents: Equivalent[]
  totalKg: number
}) {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
      {equivalents.map((eq, i) => (
        <motion.div
          key={eq.label}
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 + i * 0.08, duration: 0.5 }}
          whileHover={{ y: -3, transition: { duration: 0.2 } }}
          className="card-interactive text-center py-5"
        >
          <div className={`w-10 h-10 rounded-2xl ${eq.bg} flex items-center justify-center mx-auto mb-3`}>
            <span className={eq.color}>{eq.icon}</span>
          </div>
          <motion.p
            className="text-xl font-bold text-ink mb-0.5"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.6 + i * 0.1 }}
          >
            {eq.value(totalKg)}
          </motion.p>
          <p className="text-xs font-medium text-ink-muted">{eq.label}</p>
          <p className="text-[10px] text-ink-faint mt-0.5">{eq.sublabel}</p>
        </motion.div>
      ))}
    </div>
  )
}
