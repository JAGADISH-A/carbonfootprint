import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, Sparkles, MessageCircle } from 'lucide-react'
import { getAICoach } from '@/api/services'
import type { AICarbonCoachResponse } from '@/types/activity'

export default function CoachPreview() {
  const navigate = useNavigate()
  const [data, setData] = useState<AICarbonCoachResponse | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    getAICoach()
      .then((res) => {
        if (res.success && res.data) {
          setData(res.data)
        }
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.15 }}
        className="card"
      >
        <div className="flex items-center gap-3 mb-4">
          <div className="skeleton w-10 h-10 rounded-full" />
          <div className="skeleton w-24 h-4" />
        </div>
        <div className="space-y-2">
          <div className="skeleton w-full h-3" />
          <div className="skeleton w-3/4 h-3" />
        </div>
      </motion.div>
    )
  }

  const hasData = data && data.strengths.length > 0
  const firstInsight = hasData
    ? (data!.summary || data!.strengths[0])
    : null

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.15, ease: [0.22, 1, 0.36, 1] }}
      className="card"
    >
      <div className="flex items-center gap-3 mb-4">
        {/* Breathing avatar */}
        <motion.div
          className="w-10 h-10 rounded-full bg-leaf-100 flex items-center justify-center"
          animate={{
            scale: [1, 1.08, 1],
            boxShadow: [
              '0 0 0 0 rgba(133,204,18,0)',
              '0 0 0 6px rgba(133,204,18,0.12)',
              '0 0 0 0 rgba(133,204,18,0)',
            ],
          }}
          transition={{
            duration: 3,
            repeat: Infinity,
            ease: 'easeInOut',
          }}
        >
          <span className="text-lg">🌱</span>
        </motion.div>

        <div className="flex-1">
          <h3 className="text-sm font-semibold text-ink">EcoBuddy</h3>
          <p className="text-[11px] text-ink-faint">Your personal sustainability coach</p>
        </div>
        {data?.aiGenerated && (
          <motion.span
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.5, type: 'spring', stiffness: 300 }}
            className="badge-green"
          >
            <motion.div
              animate={{ rotate: [0, 15, -15, 0] }}
              transition={{ duration: 2, repeat: Infinity, delay: 2 }}
            >
              <Sparkles className="w-3 h-3" />
            </motion.div>
            AI
          </motion.span>
        )}
      </div>

      {hasData ? (
        <div className="space-y-3 mb-4">
          <motion.div
            initial={{ opacity: 0, x: -6 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3, duration: 0.4 }}
            className="flex items-start gap-2"
          >
            <MessageCircle className="w-4 h-4 text-emerald-500 mt-0.5 flex-shrink-0" />
            <p className="text-sm text-ink leading-relaxed line-clamp-3">
              {firstInsight}
            </p>
          </motion.div>
          {data!.recommendations.length > 0 && (
            <motion.p
              initial={{ opacity: 0, x: -6 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.4, duration: 0.4 }}
              className="text-xs text-ink-muted leading-relaxed line-clamp-2 ml-6"
            >
              {data!.recommendations[0]}
            </motion.p>
          )}
        </div>
      ) : (
        <p className="text-sm text-ink-muted leading-relaxed mb-4">
          Upload some receipts and I'll analyze your footprint, spot patterns, and suggest
          personalized ways to reduce your impact.
        </p>
      )}

      <motion.button
        onClick={() => navigate('/coach')}
        className="text-sm font-medium text-emerald-600 hover:text-emerald-700 flex items-center gap-1 group"
        whileHover={{ x: 2 }}
        whileTap={{ scale: 0.97 }}
      >
        {hasData ? 'Chat with EcoBuddy' : 'Meet your coach'}
        <motion.div
          animate={{ x: [0, 3, 0] }}
          transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
        >
          <ArrowRight className="w-4 h-4" />
        </motion.div>
      </motion.button>
    </motion.div>
  )
}
