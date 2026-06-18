import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { Leaf, Sparkles } from 'lucide-react'
import { getCarbonAnalytics } from '@/api/services'

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

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

function getGreetingEmoji(): string {
  const hour = new Date().getHours()
  if (hour < 12) return '🌅'
  if (hour < 17) return '☀️'
  return '🌙'
}

function getPersonalizedMessage(activityCount: number, totalKg: number): string {
  if (activityCount === 0) {
    return "I'm here to help you understand and reduce your carbon footprint. Upload a receipt or two and I'll start building your personal sustainability profile."
  }
  if (activityCount <= 3) {
    return "I've been looking at your recent activities. We're just getting started — upload a few more receipts and I'll be able to spot some patterns for you."
  }
  if (totalKg < 50) {
    return "I've been reviewing your footprint and it's looking pretty good so far. Let me show you what I've noticed."
  }
  return "I've been analyzing your activities and I found something interesting. Let me walk you through it."
}

export default function WelcomeHero() {
  const [activityCount, setActivityCount] = useState(0)
  const [totalKg, setTotalKg] = useState(0)

  useEffect(() => {
    getCarbonAnalytics()
      .then((res) => {
        if (res.success && res.data) {
          setActivityCount(res.data.activityCount ?? 0)
          setTotalKg(Number(res.data.totalCarbonKg ?? 0))
        }
      })
      .catch(() => {})
  }, [])

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
      className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-50 via-card to-leaf-50 border border-emerald-100/60 p-5 sm:p-6"
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
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.5 }}
          className="flex items-center gap-2 mb-3"
        >
          <motion.div
            className="w-10 h-10 rounded-full bg-leaf-100 flex items-center justify-center"
            animate={{
              scale: [1, 1.06, 1],
              boxShadow: [
                '0 0 0 0 rgba(133,204,18,0)',
                '0 0 0 8px rgba(133,204,18,0.1)',
                '0 0 0 0 rgba(133,204,18,0)',
              ],
            }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
          >
            <span className="text-xl">{getGreetingEmoji()}</span>
          </motion.div>
          <span className="text-sm text-ink-muted font-medium">EcoBuddy</span>
          {activityCount > 0 && (
            <motion.span
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              transition={{ delay: 0.6, type: 'spring', stiffness: 300 }}
              className="inline-flex items-center gap-1 text-emerald-600 text-xs font-medium"
            >
              <Sparkles className="w-3 h-3" />
              AI-powered
            </motion.span>
          )}
        </motion.div>

        <motion.h1
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="text-3xl sm:text-4xl font-bold text-ink tracking-tight mb-3"
        >
          {getGreeting()}, Jagan{' '}
          <motion.span
            className="inline-block ml-1"
            animate={{ rotate: [0, 10, -5, 0] }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
          >
            🌱
          </motion.span>
        </motion.h1>

        <motion.p
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.45, duration: 0.5 }}
          className="text-ink-muted text-base max-w-lg leading-relaxed"
        >
          {getPersonalizedMessage(activityCount, totalKg)}
        </motion.p>
      </div>
    </motion.div>
  )
}
