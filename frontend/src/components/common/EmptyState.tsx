import type { ReactNode } from 'react'
import { motion } from 'framer-motion'

interface EmptyStateProps {
  icon: ReactNode
  title: string
  description: string
  action?: ReactNode
}

export default function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4 }}
      className="flex flex-col items-center justify-center py-16 text-center"
    >
      <div className="w-16 h-16 rounded-3xl bg-emerald-50 flex items-center justify-center mb-5">
        <div className="text-emerald-500">{icon}</div>
      </div>
      <h3 className="text-lg font-semibold text-ink mb-1">{title}</h3>
      <p className="text-sm text-ink-muted max-w-sm mb-6">{description}</p>
      {action}
    </motion.div>
  )
}
