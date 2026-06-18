import { motion } from 'framer-motion'

interface StorySectionProps {
  icon: string
  label?: string
  children: React.ReactNode
  delay?: number
  accent?: boolean
}

export default function StorySection({
  icon,
  label,
  children,
  delay = 0,
  accent = false,
}: StorySectionProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{
        duration: 0.6,
        delay,
        ease: [0.22, 1, 0.36, 1],
      }}
      className={`relative ${accent ? 'pl-5 border-l-2 border-emerald-300' : ''}`}
    >
      <div className="flex items-start gap-4">
        <motion.div
          className="w-10 h-10 rounded-xl bg-cream-50/80 border border-border-light flex items-center justify-center flex-shrink-0"
          whileHover={{ scale: 1.1, rotate: [0, -5, 5, 0] }}
          transition={{ duration: 0.3 }}
        >
          <span className="text-lg">{icon}</span>
        </motion.div>
        <div className="flex-1 min-w-0">
          {label && (
            <p className="text-[10px] font-bold text-ink-muted uppercase tracking-wider mb-2">
              {label}
            </p>
          )}
          <div className="text-sm text-ink leading-relaxed space-y-2">
            {children}
          </div>
        </div>
      </div>
    </motion.div>
  )
}
