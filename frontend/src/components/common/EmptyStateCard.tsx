import type { ReactNode } from 'react'
import { motion } from 'framer-motion'

interface EmptyStateProps {
  emoji: string
  title: string
  description: string
  action?: ReactNode
  size?: 'sm' | 'md' | 'lg'
}

export default function EmptyStateCard({ emoji, title, description, action, size = 'md' }: EmptyStateProps) {
  const sizes = {
    sm: { emoji: 'text-3xl', box: 'w-16 h-16', title: 'text-base', desc: 'text-xs', py: 'py-8' },
    md: { emoji: 'text-4xl', box: 'w-20 h-20', title: 'text-lg', desc: 'text-sm', py: 'py-12' },
    lg: { emoji: 'text-5xl', box: 'w-24 h-24', title: 'text-xl', desc: 'text-sm', py: 'py-16' },
  }
  const s = sizes[size]

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
      className={`flex flex-col items-center text-center ${s.py}`}
    >
      {/* Floating emoji */}
      <motion.div
        className={`${s.box} rounded-3xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/50 flex items-center justify-center mb-5`}
        animate={{
          y: [0, -6, 0],
          boxShadow: [
            '0 4px 12px rgba(16,185,129,0.06)',
            '0 8px 24px rgba(16,185,129,0.12)',
            '0 4px 12px rgba(16,185,129,0.06)',
          ],
        }}
        transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
      >
        <span className={s.emoji}>{emoji}</span>
      </motion.div>

      <motion.h3
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.15, duration: 0.4 }}
        className={`${s.title} font-semibold text-ink mb-1.5`}
      >
        {title}
      </motion.h3>

      <motion.p
        initial={{ opacity: 0, y: 6 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.25, duration: 0.4 }}
        className={`${s.desc} text-ink-muted max-w-xs leading-relaxed mb-6`}
      >
        {description}
      </motion.p>

      {action && (
        <motion.div
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.35, duration: 0.4 }}
        >
          {action}
        </motion.div>
      )}
    </motion.div>
  )
}
