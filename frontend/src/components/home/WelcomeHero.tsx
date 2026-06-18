import { motion } from 'framer-motion'
import { Leaf } from 'lucide-react'

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good Morning'
  if (hour < 17) return 'Good Afternoon'
  return 'Good Evening'
}

function getFormattedDate(): string {
  return new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
    year: 'numeric',
  })
}

function FloatingLeaf({
  className,
  delay = 0,
  duration = 20,
  x = 0,
  y = 0,
}: {
  className?: string
  delay?: number
  duration?: number
  x?: number
  y?: number
}) {
  return (
    <motion.div
      className={`absolute pointer-events-none ${className}`}
      initial={{ opacity: 0, x, y }}
      animate={{
        opacity: [0, 0.08, 0.08, 0],
        y: [y, y - 30, y - 50, y - 70],
        x: [x, x + 15, x - 10, x + 5],
        rotate: [0, 10, -8, 5],
      }}
      transition={{
        duration,
        delay,
        repeat: Infinity,
        ease: 'easeInOut',
      }}
    >
      <Leaf className="w-full h-full text-emerald-700" />
    </motion.div>
  )
}

export default function WelcomeHero() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
      className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-emerald-50 via-card to-leaf-50 border border-emerald-100/60 p-8 sm:p-10"
    >
      {/* Floating leaves */}
      <FloatingLeaf className="w-8 h-8 top-3 right-16" delay={0} duration={18} x={0} y={0} />
      <FloatingLeaf className="w-5 h-5 top-8 right-40" delay={3} duration={22} x={0} y={10} />
      <FloatingLeaf className="w-6 h-6 bottom-4 right-24" delay={6} duration={20} x={0} y={0} />
      <FloatingLeaf className="w-4 h-4 top-12 right-60" delay={9} duration={24} x={0} y={5} />
      <FloatingLeaf className="w-7 h-7 bottom-8 right-8" delay={2} duration={19} x={0} y={0} />

      {/* Static decorative leaf */}
      <motion.div
        className="absolute top-4 right-6 opacity-[0.05]"
        animate={{ rotate: [-12, -8, -12] }}
        transition={{ duration: 8, repeat: Infinity, ease: 'easeInOut' }}
      >
        <Leaf className="w-40 h-40 text-emerald-700" />
      </motion.div>

      <div className="relative z-10">
        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.5 }}
          className="text-sm text-ink-muted font-medium mb-1"
        >
          {getFormattedDate()}
        </motion.p>
        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="text-3xl sm:text-4xl font-bold text-ink tracking-tight mb-2"
        >
          {getGreeting()}, Jagan{' '}
          <motion.span
            className="inline-block ml-1"
            animate={{ rotate: [0, 10, -5, 0] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
          >
            🌿
          </motion.span>
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.45, duration: 0.5 }}
          className="text-ink-muted text-base max-w-md"
        >
          Helping you build greener habits every day.
        </motion.p>
      </div>
    </motion.div>
  )
}
