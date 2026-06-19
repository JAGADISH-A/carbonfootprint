import { useEffect, useState, useCallback } from 'react'
import { motion } from 'framer-motion'
import { ArrowRight, RefreshCw } from 'lucide-react'
import { getAICoach, getCarbonAnalytics } from '@/api/services'
import type { AICarbonCoachResponse, CarbonAnalyticsResponse } from '@/types/activity'
import { CarbonSummaryPanel, CoachChat } from '@/components/coach'

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function Coach() {
  const [coach, setCoach] = useState<AICarbonCoachResponse | null>(null)
  const [analytics, setAnalytics] = useState<CarbonAnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)

  const fetchData = useCallback(async (signal?: AbortSignal) => {
    setLoading(true)
    try {
      const [coachRes, analyticsRes] = await Promise.all([
        getAICoach(undefined, signal),
        getCarbonAnalytics(undefined, signal),
      ])
      if (coachRes.success && coachRes.data) setCoach(coachRes.data)
      if (analyticsRes.success && analyticsRes.data) setAnalytics(analyticsRes.data)
    } catch {
      // silent
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    const controller = new AbortController()
    fetchData(controller.signal)
    return () => controller.abort()
  }, [fetchData])

  const hasData = coach && (coach.summary || (coach.strengths.length > 0) || coach.actionPlan?.length)

  if (!hasData && !loading) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        className="text-center py-12"
      >
        <motion.div
          className="w-20 h-20 rounded-full bg-emerald-50 flex items-center justify-center mx-auto mb-5"
          animate={{
            scale: [1, 1.06, 1],
            boxShadow: [
              '0 0 0 0 rgba(16,185,129,0)',
              '0 0 0 10px rgba(16,185,129,0.06)',
              '0 0 0 0 rgba(16,185,129,0)',
            ],
          }}
          transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
        >
          <span className="text-3xl">🌱</span>
        </motion.div>

        <h1 className="text-xl font-bold text-ink mb-2">Hey there!</h1>
        <p className="text-ink-muted max-w-sm mx-auto mb-6 leading-relaxed text-sm">
          I'm EcoBuddy, your personal sustainability coach. Upload a receipt or two
          and I'll analyze your carbon footprint and tell you your carbon story.
        </p>

        <motion.button
          onClick={() => (window.location.href = '/upload')}
          whileHover={{ scale: 1.03, y: -1 }}
          whileTap={{ scale: 0.97 }}
          className="btn-primary inline-flex items-center gap-2"
        >
          Upload your first receipt
          <ArrowRight className="w-4 h-4" />
        </motion.button>
      </motion.div>
    )
  }

  const greeting = getGreeting()

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] -mt-4 -mx-4 sm:-mx-6 lg:-mx-8 xl:-mx-10">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex items-center justify-between px-5 py-3 border-b border-border-light bg-white/80 backdrop-blur-sm flex-shrink-0"
      >
        <div className="flex items-center gap-3">
          <motion.div
            className="w-9 h-9 rounded-full bg-emerald-100 flex items-center justify-center"
            animate={{
              scale: [1, 1.04, 1],
            }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
          >
            <span className="text-lg">🌱</span>
          </motion.div>
          <div>
            <h1 className="text-sm font-bold text-ink tracking-tight">
              {greeting}
              {coach?.aiGenerated && (
                <span className="ml-2 inline-flex items-center gap-1 text-emerald-600 font-medium text-[10px] bg-emerald-50 px-1.5 py-0.5 rounded-full">
                  AI-powered
                </span>
              )}
            </h1>
            <p className="text-[11px] text-ink-muted">
              Your personal carbon coach
            </p>
          </div>
        </div>

        <motion.button
          onClick={fetchData}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.95 }}
          className="text-xs text-ink-muted hover:text-emerald-600 inline-flex items-center gap-1.5 transition-colors px-2 py-1 rounded-lg hover:bg-emerald-50"
        >
          <RefreshCw className="w-3 h-3" />
          Refresh
        </motion.button>
      </motion.div>

      {/* Two-column layout */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left: Carbon Summary */}
        <div className="w-[340px] xl:w-[380px] flex-shrink-0 border-r border-border-light bg-white/40 overflow-hidden">
          <div className="h-full p-4">
            <CarbonSummaryPanel
              coach={coach}
              analytics={analytics}
              loading={loading}
            />
          </div>
        </div>

        {/* Right: Chat */}
        <div className="flex-1 min-w-0 p-4">
          <CoachChat enabled={hasData} />
        </div>
      </div>
    </div>
  )
}
