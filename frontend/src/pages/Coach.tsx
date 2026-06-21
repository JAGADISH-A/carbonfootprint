import { useEffect, useState, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight, RefreshCw, BarChart3 } from 'lucide-react'
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
  const navigate = useNavigate()
  const [coach, setCoach] = useState<AICarbonCoachResponse | null>(null)
  const [analytics, setAnalytics] = useState<CarbonAnalyticsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [summaryOpen, setSummaryOpen] = useState(false)

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

  const hasData = !!(coach && (coach.summary || (coach.strengths && coach.strengths.length > 0) || (coach.actionPlan && coach.actionPlan.length > 0)))

  if (!hasData && !loading) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
        className="text-center py-8"
      >
        <motion.div
          className="w-16 h-16 rounded-full bg-emerald-50 flex items-center justify-center mx-auto mb-4"
          animate={{
            scale: [1, 1.05, 1],
            boxShadow: [
              '0 0 0 0 rgba(16,185,129,0)',
              '0 0 0 8px rgba(16,185,129,0.06)',
              '0 0 0 0 rgba(16,185,129,0)',
            ],
          }}
          transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
        >
          <span className="text-2xl">🌱</span>
        </motion.div>

        <h1 className="text-lg font-bold text-ink mb-1.5">Hey there!</h1>
        <p className="text-ink-muted max-w-xs mx-auto mb-5 leading-relaxed text-sm">
          I'm EcoBuddy, your personal sustainability coach. Upload a receipt or two
          and I'll analyze your carbon footprint.
        </p>

        <motion.button
          onClick={() => navigate('/upload')}
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
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3 }}
        className="flex items-center justify-between px-4 py-2 border-b border-border-light bg-white/80 backdrop-blur-sm flex-shrink-0"
      >
        <div className="flex items-center gap-2.5">
          <div className="w-7 h-7 rounded-full bg-emerald-100 flex items-center justify-center">
            <span className="text-sm">🌱</span>
          </div>
          <div>
            <h1 className="text-xs font-bold text-ink tracking-tight">
              {greeting}
              {coach?.aiGenerated && (
                <span className="ml-1.5 inline-flex items-center gap-0.5 text-emerald-600 font-medium text-[9px] bg-emerald-50 px-1 py-0.5 rounded-full">
                  AI
                </span>
              )}
            </h1>
            <p className="text-[10px] text-ink-muted">Carbon coach</p>
          </div>
        </div>

        <div className="flex items-center gap-1">
          <button
            onClick={() => setSummaryOpen(!summaryOpen)}
            className="lg:hidden text-[10px] text-ink-muted hover:text-emerald-600 inline-flex items-center gap-1 transition-colors px-1.5 py-0.5 rounded hover:bg-emerald-50"
          >
            <BarChart3 className="w-2.5 h-2.5" />
            {summaryOpen ? 'Hide' : 'Data'}
          </button>

          <motion.button
            onClick={() => fetchData()}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            className="text-[10px] text-ink-muted hover:text-emerald-600 inline-flex items-center gap-1 transition-colors px-1.5 py-0.5 rounded hover:bg-emerald-50"
          >
            <RefreshCw className="w-2.5 h-2.5" />
            Refresh
          </motion.button>
        </div>
      </motion.div>

      {/* Mobile/Tablet: Summary (collapsible) */}
      <div className="lg:hidden">
        <motion.div
          initial={false}
          animate={{ height: summaryOpen ? 'auto' : 0 }}
          transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
          className="overflow-hidden border-b border-border-light bg-white/60"
        >
          <div className="p-3 max-h-[50vh] overflow-y-auto">
            <CarbonSummaryPanel coach={coach} analytics={analytics} loading={loading} />
          </div>
        </motion.div>
      </div>

      {/* Desktop: Summary (always visible) + Chat side by side */}
      {/* Mobile/Tablet: Chat takes remaining height, input always visible */}
      <div className="flex-1 flex overflow-hidden">
        {/* Desktop summary sidebar */}
        <div className="hidden lg:block lg:w-[300px] xl:w-[340px] flex-shrink-0 border-r border-border-light bg-white/40 overflow-y-auto">
          <div className="p-3">
            <CarbonSummaryPanel coach={coach} analytics={analytics} loading={loading} />
          </div>
        </div>

        {/* Chat — always fills remaining space, input pinned to bottom */}
        <div className="flex-1 min-w-0 p-3 flex flex-col overflow-hidden">
          <CoachChat enabled={hasData} />
        </div>
      </div>
    </div>
  )
}
