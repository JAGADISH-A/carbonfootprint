import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { CheckCircle2, Sparkles, ArrowRight, MessageCircle } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import type { TopEmissionActivity } from '@/types/activity'

interface SuccessCelebrationProps {
  activity: TopEmissionActivity
  onAutoRedirect?: () => void
}

export default function SuccessCelebration({
  activity,
  onAutoRedirect,
}: SuccessCelebrationProps) {
  const navigate = useNavigate()
  const [showDetails, setShowDetails] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setShowDetails(true), 600)
    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    if (onAutoRedirect) {
      const timer = setTimeout(onAutoRedirect, 8000)
      return () => clearTimeout(timer)
    }
  }, [onAutoRedirect])

  const categoryLabel =
    activity.category.charAt(0) + activity.category.slice(1).toLowerCase()

  return (
    <motion.div
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="text-center py-4"
    >
      {/* Celebration animation */}
      <div className="relative mb-6">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: 'spring', stiffness: 300, damping: 15 }}
          className="w-20 h-20 rounded-full bg-emerald-50 flex items-center justify-center mx-auto"
        >
          <motion.div
            initial={{ scale: 0, rotate: -45 }}
            animate={{ scale: 1, rotate: 0 }}
            transition={{ delay: 0.2, type: 'spring', stiffness: 400 }}
          >
            <CheckCircle2 className="w-10 h-10 text-emerald-500" />
          </motion.div>
        </motion.div>

        {/* Floating sparkles */}
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <motion.div
            key={i}
            className="absolute left-1/2 top-1/2"
            initial={{ opacity: 0, scale: 0 }}
            animate={{
              opacity: [0, 1, 0],
              scale: [0, 1, 0.5],
              x: Math.cos((i * 60 * Math.PI) / 180) * 60,
              y: Math.sin((i * 60 * Math.PI) / 180) * 60,
            }}
            transition={{ delay: 0.3 + i * 0.08, duration: 0.8, ease: 'easeOut' }}
          >
            <Sparkles className="w-3 h-3 text-amber-400" />
          </motion.div>
        ))}
      </div>

      <motion.h2
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="text-xl font-bold text-ink mb-1"
      >
        Receipt processed! <span className="inline-block ml-1">🌿</span>
      </motion.h2>

      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.4 }}
        className="text-sm text-ink-muted mb-6"
      >
        Your carbon impact has been calculated and saved.
      </motion.p>

      {/* Result card */}
      {showDetails && (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="w-full max-w-sm mx-auto bg-card rounded-2xl border border-border-light shadow-card p-5 mb-6"
        >
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs text-ink-muted">Merchant</span>
              <span className="text-sm font-semibold text-ink">
                {activity.merchant || 'Unknown'}
              </span>
            </div>
            <div className="h-px bg-border-light" />
            <div className="flex items-center justify-between">
              <span className="text-xs text-ink-muted">Category</span>
              <span className="badge-green text-[11px]">{categoryLabel}</span>
            </div>
            <div className="h-px bg-border-light" />
            <div className="flex items-center justify-between">
              <span className="text-xs text-ink-muted">Carbon footprint</span>
              <span className="text-lg font-bold text-emerald-600">
                {Number(activity.carbonKg ?? 0).toFixed(2)} kg CO₂e
              </span>
            </div>
          </div>
        </motion.div>
      )}

      {/* Action buttons */}
      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6 }}
        className="flex flex-col sm:flex-row items-center justify-center gap-3"
      >
        <motion.button
          onClick={() => navigate('/')}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          className="btn-primary inline-flex items-center gap-2"
        >
          View dashboard
          <ArrowRight className="w-4 h-4" />
        </motion.button>
        <motion.button
          onClick={() => navigate('/coach')}
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
          className="btn-secondary inline-flex items-center gap-2"
        >
          <MessageCircle className="w-4 h-4" />
          Chat with EcoBuddy
        </motion.button>
      </motion.div>

      {/* Auto-redirect notice */}
      <motion.p
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 1 }}
        className="text-[11px] text-ink-faint mt-4"
      >
        Auto-redirecting to dashboard in a few seconds...
      </motion.p>
    </motion.div>
  )
}
