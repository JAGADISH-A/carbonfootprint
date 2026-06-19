import { useEffect, useState, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Sparkles,
  Send,
  RefreshCw,
  ArrowRight,
  MessageCircle,
} from 'lucide-react'
import { getAICoach } from '@/api/services'
import type { AICarbonCoachResponse, ActionPlanItem } from '@/types/activity'
import { StorySection, ActionPlanCard } from '@/components/coach'

const stagger = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.08 },
  },
}

function SkeletonStory() {
  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <div className="skeleton w-12 h-12 rounded-full" />
        <div className="space-y-1.5">
          <div className="skeleton w-48 h-5" />
          <div className="skeleton w-64 h-3" />
        </div>
      </div>
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="flex gap-4">
          <div className="skeleton w-10 h-10 rounded-xl" />
          <div className="flex-1 space-y-2">
            <div className="skeleton w-20 h-3" />
            <div className="skeleton w-full h-3" />
            <div className="skeleton w-4/5 h-3" />
          </div>
        </div>
      ))}
    </div>
  )
}

function hasStoryData(coach: AICarbonCoachResponse | null): boolean {
  if (!coach) return false
  return !!(coach.whatHappened || coach.actionPlan?.length)
}

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function Coach() {
  const [coach, setCoach] = useState<AICarbonCoachResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [question, setQuestion] = useState('')
  const [questionSent, setQuestionSent] = useState(false)

  const fetchData = useCallback(async (signal?: AbortSignal) => {
    setLoading(true)
    try {
      const coachRes = await getAICoach(undefined, signal)
      if (coachRes.success && coachRes.data) setCoach(coachRes.data)
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

  const handleSendQuestion = () => {
    if (!question.trim()) return
    setQuestionSent(true)
    setQuestion('')
    setTimeout(() => setQuestionSent(false), 3000)
  }

  const isStory = hasStoryData(coach)
  const hasLegacyData = coach && !isStory && (coach.strengths.length > 0 || coach.recommendations.length > 0)
  const hasData = isStory || hasLegacyData

  if (loading) {
    return <SkeletonStory />
  }

  if (!hasData) {
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
          onClick={() => window.location.href = '/upload'}
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
    <motion.div variants={stagger} initial="hidden" animate="show">

      {/* ── Greeting ─────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5 }}
        className="flex items-center gap-3 mb-6"
      >
        <motion.div
          className="w-12 h-12 rounded-full bg-leaf-100 flex items-center justify-center flex-shrink-0"
          animate={{
            scale: [1, 1.06, 1],
            boxShadow: [
              '0 0 0 0 rgba(133,204,18,0)',
              '0 0 0 8px rgba(133,204,18,0.1)',
              '0 0 0 0 rgba(133,204,18,0)',
            ],
          }}
          transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
        >
          <span className="text-2xl">🌱</span>
        </motion.div>
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-ink tracking-tight">
            {greeting}, Jagan
          </h1>
          <p className="text-ink-muted text-sm mt-0.5">
            I looked at your activities and wrote up your carbon story.
            {coach?.aiGenerated && (
              <span className="inline-flex items-center gap-1 ml-2 text-emerald-600 font-medium text-xs">
                <Sparkles className="w-3 h-3" /> AI-powered
              </span>
            )}
            {coach?.confidence !== undefined && coach.confidence > 0 && (
              <span className="inline-flex items-center gap-1 ml-2 text-ink-muted text-xs">
                <span className="w-1.5 h-1.5 rounded-full bg-emerald-400" />
                {coach.confidence}% confidence
              </span>
            )}
          </p>
        </div>
      </motion.div>

      {/* ── Story: What Happened ─────────────────────────────────────── */}
      {isStory && coach?.whatHappened && (
        <StorySection icon="📖" label="What happened this week" delay={0.1}>
          <p>{coach.whatHappened}</p>
        </StorySection>
      )}

      {/* ── Story: What Surprised Me ─────────────────────────────────── */}
      {isStory && coach?.whatSurprisedMe && (
        <StorySection icon="💡" label="What surprised me" delay={0.2}>
          <p>{coach.whatSurprisedMe}</p>
        </StorySection>
      )}

      {/* ── Story: What's Going Well ─────────────────────────────────── */}
      {isStory && coach?.whatsGoingWell && (
        <StorySection icon="✨" label="What's going well" delay={0.3} accent>
          <p>{coach.whatsGoingWell}</p>
        </StorySection>
      )}

      {/* ── Story: Biggest Opportunity ────────────────────────────────── */}
      {isStory && coach?.biggestOpportunity && (
        <StorySection icon="🎯" label="Biggest opportunity" delay={0.4}>
          <p>{coach.biggestOpportunity}</p>
        </StorySection>
      )}

      {/* ── Action Plan ──────────────────────────────────────────────── */}
      {isStory && coach?.actionPlan && coach.actionPlan.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.5, ease: [0.22, 1, 0.36, 1] }}
          className="mt-6"
        >
          <div className="flex items-center gap-2.5 mb-4">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-emerald-600 flex items-center justify-center">
              <span className="text-sm font-bold text-white">1-2-3</span>
            </div>
            <div>
              <h2 className="text-sm font-semibold text-ink">Your action plan</h2>
              <p className="text-[11px] text-ink-muted">Three changes, in order of impact</p>
            </div>
          </div>
          <div className="space-y-3">
            {coach.actionPlan.map((item: ActionPlanItem, i: number) => (
              <ActionPlanCard key={i} item={item} index={i} />
            ))}
          </div>
        </motion.div>
      )}

      {/* ── Closing ──────────────────────────────────────────────────── */}
      {isStory && coach?.closing && (
        <motion.div
          initial={{ opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.7, ease: [0.22, 1, 0.36, 1] }}
          className="mt-6 text-center"
        >
          <p className="text-sm text-ink-muted italic max-w-md mx-auto">
            {coach.closing}
          </p>
        </motion.div>
      )}

      {/* ── Legacy format fallback ───────────────────────────────────── */}
      {!isStory && hasLegacyData && (
        <div className="space-y-6">
          {coach?.summary && (
            <StorySection icon="📖" label="What I found" delay={0.1}>
              <p>{coach.summary}</p>
            </StorySection>
          )}

          {coach?.strengths && coach.strengths.length > 0 && (
            <StorySection icon="✨" label="What's going well" delay={0.2} accent>
              {coach.strengths.map((s, i) => (
                <p key={i}>{s}</p>
              ))}
            </StorySection>
          )}

          {coach?.concerns && coach.concerns.length > 0 && (
            <StorySection icon="⚠️" label="Worth paying attention to" delay={0.3}>
              {coach.concerns.map((c, i) => (
                <p key={i}>{c}</p>
              ))}
            </StorySection>
          )}

          {coach?.recommendations && coach.recommendations.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
              className="mt-4"
            >
              <div className="flex items-center gap-2.5 mb-3">
                <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-emerald-500 to-emerald-600 flex items-center justify-center">
                  <span className="text-sm font-bold text-white">1-2-3</span>
                </div>
                <div>
                  <h2 className="text-sm font-semibold text-ink">Your action plan</h2>
                  <p className="text-[11px] text-ink-muted">Friendly suggestions</p>
                </div>
              </div>
              <div className="space-y-3">
                {coach.recommendations.map((r, i) => (
                  <ActionPlanCard
                    key={i}
                    item={{
                      priority: i + 1,
                      title: r,
                      whyItMatters: 'Based on your specific carbon patterns.',
                      whatToDo: 'Start with this one today.',
                    }}
                    index={i}
                  />
                ))}
              </div>
            </motion.div>
          )}
        </div>
      )}

      {/* ── Ask EcoBuddy ─────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0, y: 12 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.8, ease: [0.22, 1, 0.36, 1] }}
        className="mt-8"
      >
        <div className="card">
          <div className="flex items-center gap-2.5 mb-3">
            <div className="w-8 h-8 rounded-lg bg-emerald-50 flex items-center justify-center">
              <MessageCircle className="w-4 h-4 text-emerald-600" />
            </div>
            <div>
              <h2 className="text-sm font-semibold text-ink">Ask me anything</h2>
              <p className="text-[11px] text-ink-muted">Sustainability questions welcome</p>
            </div>
          </div>

          <AnimatePresence mode="wait">
            {questionSent ? (
              <motion.div
                key="sent"
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -8 }}
                className="flex items-center gap-2 text-sm text-emerald-600 py-3"
              >
                <motion.div
                  animate={{ rotate: 360 }}
                  transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                >
                  <RefreshCw className="w-4 h-4" />
                </motion.div>
                Let me think about that...
              </motion.div>
            ) : (
              <motion.div
                key="input"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex gap-2"
              >
                <input
                  type="text"
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSendQuestion()}
                  placeholder="e.g. What's the easiest way to cut my footprint?"
                  className="input flex-1 text-sm"
                />
                <motion.button
                  onClick={handleSendQuestion}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  disabled={!question.trim()}
                  className="w-10 h-10 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center flex-shrink-0 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <Send className="w-4 h-4" />
                </motion.button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </motion.div>

      {/* ── Refresh ──────────────────────────────────────────────────── */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5, delay: 0.9 }}
        className="text-center mt-6 pb-2"
      >
        <motion.button
          onClick={fetchData}
          whileHover={{ scale: 1.03 }}
          whileTap={{ scale: 0.97 }}
          className="text-sm text-ink-muted hover:text-emerald-600 inline-flex items-center gap-1.5 transition-colors"
        >
          <RefreshCw className="w-3.5 h-3.5" />
          Refresh my story
        </motion.button>
      </motion.div>

    </motion.div>
  )
}
