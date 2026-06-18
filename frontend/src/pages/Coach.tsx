import { useEffect, useState, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Sparkles,
  TrendingUp,
  AlertTriangle,
  Trophy,
  Target,
  Heart,
  Send,
  Leaf,
  RefreshCw,
  ArrowRight,
  Lightbulb,
  Shield,
  Flame,
} from 'lucide-react'
import { getAICoach, getCarbonInsights } from '@/api/services'
import type { AICarbonCoachResponse, CarbonInsightResponse } from '@/types/activity'

const stagger = {
  hidden: { opacity: 0 },
  show: {
    opacity: 1,
    transition: { staggerChildren: 0.08 },
  },
}

const fadeUp = {
  hidden: { opacity: 0, y: 16 },
  show: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.22, 1, 0.36, 1] as const } },
}

function SkeletonSection() {
  return (
    <div className="space-y-6">
      {/* Greeting skeleton */}
      <div className="flex items-center gap-4 mb-8">
        <div className="skeleton w-16 h-16 rounded-full" />
        <div className="space-y-2">
          <div className="skeleton w-48 h-7" />
          <div className="skeleton w-72 h-4" />
        </div>
      </div>
      {/* Card skeletons */}
      {[0, 1, 2, 3].map((i) => (
        <div key={i} className="card">
          <div className="flex items-center gap-3 mb-4">
            <div className="skeleton w-10 h-10 rounded-2xl" />
            <div className="skeleton w-32 h-5" />
          </div>
          <div className="space-y-2">
            <div className="skeleton w-full h-3" />
            <div className="skeleton w-4/5 h-3" />
            <div className="skeleton w-2/3 h-3" />
          </div>
        </div>
      ))}
    </div>
  )
}

function StrengthCard({ text, index }: { text: string; index: number }) {
  const icons = [Shield, Leaf, Heart, TrendingUp, Trophy]
  const Icon = icons[index % icons.length]
  const colors = [
    'bg-emerald-50 text-emerald-600 border-emerald-100',
    'bg-leaf-50 text-leaf-600 border-leaf-100',
    'bg-teal-50 text-teal-600 border-teal-100',
    'bg-cyan-50 text-cyan-600 border-cyan-100',
    'bg-green-50 text-green-600 border-green-100',
  ]

  return (
    <motion.div
      variants={fadeUp}
      whileHover={{ y: -3, transition: { duration: 0.2 } }}
      className={`rounded-2xl border p-5 ${colors[index % colors.length]}`}
    >
      <div className="flex items-start gap-3">
        <div className="w-9 h-9 rounded-xl bg-white/60 flex items-center justify-center flex-shrink-0 mt-0.5">
          <Icon className="w-4.5 h-4.5" />
        </div>
        <p className="text-sm font-medium leading-relaxed text-ink">{text}</p>
      </div>
    </motion.div>
  )
}

function ConcernCard({ text, index }: { text: string; index: number }) {
  const icons = [AlertTriangle, Flame, Target]
  const Icon = icons[index % icons.length]

  return (
    <motion.div
      variants={fadeUp}
      whileHover={{ y: -3, transition: { duration: 0.2 } }}
      className="rounded-2xl border border-amber-100 bg-amber-50 p-5"
    >
      <div className="flex items-start gap-3">
        <div className="w-9 h-9 rounded-xl bg-white/60 flex items-center justify-center flex-shrink-0 mt-0.5">
          <Icon className="w-4.5 h-4.5 text-amber-600" />
        </div>
        <p className="text-sm font-medium leading-relaxed text-ink">{text}</p>
      </div>
    </motion.div>
  )
}

function RecommendationItem({ text, index }: { text: string; index: number }) {
  return (
    <motion.div
      variants={fadeUp}
      className="flex items-start gap-3 py-3"
    >
      <div className="w-7 h-7 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0 mt-0.5">
        <span className="text-xs font-bold text-emerald-600">{index + 1}</span>
      </div>
      <p className="text-sm text-ink leading-relaxed">{text}</p>
    </motion.div>
  )
}

export default function Coach() {
  const [coach, setCoach] = useState<AICarbonCoachResponse | null>(null)
  const [insights, setInsights] = useState<CarbonInsightResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [question, setQuestion] = useState('')
  const [questionSent, setQuestionSent] = useState(false)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const [coachRes, insightsRes] = await Promise.all([
        getAICoach(),
        getCarbonInsights(),
      ])
      if (coachRes.success && coachRes.data) setCoach(coachRes.data)
      if (insightsRes.success && insightsRes.data) setInsights(insightsRes.data)
    } catch {
      // silent
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const handleSendQuestion = () => {
    if (!question.trim()) return
    setQuestionSent(true)
    setQuestion('')
    setTimeout(() => setQuestionSent(false), 3000)
  }

  const hasData = coach && (coach.strengths.length > 0 || coach.recommendations.length > 0)

  if (loading) {
    return (
      <div className="max-w-3xl mx-auto">
        <SkeletonSection />
      </div>
    )
  }

  if (!hasData) {
    return (
      <div className="max-w-3xl mx-auto">
        {/* Empty state greeting */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="text-center py-16"
        >
          <motion.div
            className="w-24 h-24 rounded-full bg-emerald-50 flex items-center justify-center mx-auto mb-6"
            animate={{
              scale: [1, 1.06, 1],
              boxShadow: [
                '0 0 0 0 rgba(16,185,129,0)',
                '0 0 0 12px rgba(16,185,129,0.06)',
                '0 0 0 0 rgba(16,185,129,0)',
              ],
            }}
            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
          >
            <span className="text-4xl">🌱</span>
          </motion.div>

          <h1 className="text-2xl font-bold text-ink mb-2">Meet EcoBuddy</h1>
          <p className="text-ink-muted max-w-md mx-auto mb-8 leading-relaxed">
            Your personal sustainability mentor. Upload some receipts first,
            and I&apos;ll analyze your carbon footprint and give you personalized coaching.
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
      </div>
    )
  }

  const greeting = getGreeting()

  return (
    <div className="max-w-3xl mx-auto">
      <motion.div variants={stagger} initial="hidden" animate="show">

        {/* ── Greeting ──────────────────────────────────────────────── */}
        <motion.div variants={fadeUp} className="flex items-center gap-5 mb-10">
          <motion.div
            className="w-16 h-16 rounded-full bg-leaf-100 flex items-center justify-center flex-shrink-0"
            animate={{
              scale: [1, 1.06, 1],
              boxShadow: [
                '0 0 0 0 rgba(133,204,18,0)',
                '0 0 0 10px rgba(133,204,18,0.1)',
                '0 0 0 0 rgba(133,204,18,0)',
              ],
            }}
            transition={{ duration: 3.5, repeat: Infinity, ease: 'easeInOut' }}
          >
            <span className="text-3xl">🌱</span>
          </motion.div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-ink tracking-tight">
              {greeting}
            </h1>
            <p className="text-ink-muted mt-0.5">
              Here&apos;s your personalized sustainability report.
              {coach?.aiGenerated && (
                <span className="inline-flex items-center gap-1 ml-2 text-emerald-600 font-medium text-xs">
                  <Sparkles className="w-3 h-3" /> AI-powered
                </span>
              )}
            </p>
          </div>
        </motion.div>

        {/* ── AI Summary ────────────────────────────────────────────── */}
        {coach?.summary && (
          <motion.div
            variants={fadeUp}
            className="rounded-3xl bg-gradient-to-br from-emerald-50 via-card to-leaf-50 border border-emerald-100/60 p-7 mb-6"
          >
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-emerald-100 flex items-center justify-center">
                <Sparkles className="w-5 h-5 text-emerald-600" />
              </div>
              <h2 className="text-base font-semibold text-ink">AI Summary</h2>
            </div>
            <p className="text-sm text-ink-light leading-relaxed">{coach.summary}</p>
          </motion.div>
        )}

        {/* ── Strengths / Positive Habits ───────────────────────────── */}
        {coach && coach.strengths.length > 0 && (
          <motion.div variants={fadeUp} className="mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <TrendingUp className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Positive Habits</h2>
                <p className="text-xs text-ink-muted">Things you&apos;re doing well</p>
              </div>
            </div>
            <motion.div
              variants={stagger}
              initial="hidden"
              animate="show"
              className="grid grid-cols-1 sm:grid-cols-2 gap-3"
            >
              {coach.strengths.map((s, i) => (
                <StrengthCard key={i} text={s} index={i} />
              ))}
            </motion.div>
          </motion.div>
        )}

        {/* ── Concerns / Areas to Improve ───────────────────────────── */}
        {coach && coach.concerns.length > 0 && (
          <motion.div variants={fadeUp} className="mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-amber-50 flex items-center justify-center">
                <AlertTriangle className="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Areas to Improve</h2>
                <p className="text-xs text-ink-muted">Opportunities for a smaller footprint</p>
              </div>
            </div>
            <motion.div
              variants={stagger}
              initial="hidden"
              animate="show"
              className="grid grid-cols-1 sm:grid-cols-2 gap-3"
            >
              {coach.concerns.map((c, i) => (
                <ConcernCard key={i} text={c} index={i} />
              ))}
            </motion.div>
          </motion.div>
        )}

        {/* ── Recommendations ───────────────────────────────────────── */}
        {coach && coach.recommendations.length > 0 && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="flex items-center gap-3 mb-5">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <Lightbulb className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Personalized Recommendations</h2>
                <p className="text-xs text-ink-muted">Actionable steps based on your data</p>
              </div>
            </div>
            <motion.div variants={stagger} initial="hidden" animate="show" className="divide-y divide-border-light">
              {coach.recommendations.map((r, i) => (
                <RecommendationItem key={i} text={r} index={i} />
              ))}
            </motion.div>
          </motion.div>
        )}

        {/* ── Weekly Challenge ──────────────────────────────────────── */}
        {coach?.weeklyChallenge && (
          <motion.div
            variants={fadeUp}
            className="rounded-3xl bg-gradient-to-br from-leaf-50 to-emerald-50 border border-leaf-100 p-7 mb-6 relative overflow-hidden"
          >
            {/* Decorative circles */}
            <div className="absolute -top-8 -right-8 w-32 h-32 rounded-full bg-leaf-100/40" />
            <div className="absolute -bottom-4 -left-4 w-20 h-20 rounded-full bg-emerald-100/30" />

            <div className="relative z-10">
              <div className="flex items-center gap-3 mb-4">
                <motion.div
                  className="w-10 h-10 rounded-2xl bg-leaf-200 flex items-center justify-center"
                  animate={{ rotate: [0, 5, -5, 0] }}
                  transition={{ duration: 4, repeat: Infinity, ease: 'easeInOut' }}
                >
                  <Target className="w-5 h-5 text-leaf-700" />
                </motion.div>
                <div>
                  <h2 className="text-base font-semibold text-ink">Weekly Challenge</h2>
                  <p className="text-xs text-ink-muted">Push yourself this week</p>
                </div>
              </div>
              <p className="text-sm text-ink-light leading-relaxed max-w-lg">
                {coach.weeklyChallenge}
              </p>
            </div>
          </motion.div>
        )}

        {/* ── Insights (from deterministic endpoint) ────────────────── */}
        {insights && insights.insights.length > 0 && (
          <motion.div variants={fadeUp} className="card mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
                <Leaf className="w-5 h-5 text-emerald-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Data Insights</h2>
                <p className="text-xs text-ink-muted">Patterns detected in your activity</p>
              </div>
            </div>
            <div className="space-y-3">
              {insights.insights.map((insight, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.3 + i * 0.06 }}
                  className="flex items-start gap-3"
                >
                  <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 mt-2 flex-shrink-0" />
                  <p className="text-sm text-ink-light leading-relaxed">{insight}</p>
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}

        {/* ── Motivation Card ───────────────────────────────────────── */}
        {coach?.motivation && (
          <motion.div
            variants={fadeUp}
            className="rounded-3xl bg-card border border-border-light shadow-card p-7 mb-6 text-center"
          >
            <motion.div
              className="w-14 h-14 rounded-full bg-gradient-to-br from-emerald-100 to-leaf-100 flex items-center justify-center mx-auto mb-5"
              animate={{ scale: [1, 1.05, 1] }}
              transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            >
              <Heart className="w-6 h-6 text-emerald-600" />
            </motion.div>
            <p className="text-lg font-semibold text-ink leading-relaxed max-w-md mx-auto italic">
              &ldquo;{coach.motivation}&rdquo;
            </p>
          </motion.div>
        )}

        {/* ── Achievements (from insights) ──────────────────────────── */}
        {insights && insights.achievements.length > 0 && (
          <motion.div variants={fadeUp} className="mb-6">
            <div className="flex items-center gap-3 mb-4">
              <div className="w-10 h-10 rounded-2xl bg-amber-50 flex items-center justify-center">
                <Trophy className="w-5 h-5 text-amber-600" />
              </div>
              <div>
                <h2 className="text-base font-semibold text-ink">Achievements Unlocked</h2>
                <p className="text-xs text-ink-muted">Milestones you&apos;ve reached</p>
              </div>
            </div>
            <div className="flex flex-wrap gap-2">
              {insights.achievements.map((a, i) => (
                <motion.div
                  key={i}
                  initial={{ opacity: 0, scale: 0.8 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.4 + i * 0.08, type: 'spring', stiffness: 300 }}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-amber-50 border border-amber-100 text-xs font-medium text-amber-700"
                >
                  <Trophy className="w-3 h-3" />
                  {a}
                </motion.div>
              ))}
            </div>
          </motion.div>
        )}

        {/* ── Ask Another Question ──────────────────────────────────── */}
        <motion.div variants={fadeUp} className="card mb-4">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 rounded-2xl bg-emerald-50 flex items-center justify-center">
              <Sparkles className="w-5 h-5 text-emerald-600" />
            </div>
            <div>
              <h2 className="text-base font-semibold text-ink">Ask EcoBuddy</h2>
              <p className="text-xs text-ink-muted">Get advice on any sustainability topic</p>
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
                Analyzing your question...
              </motion.div>
            ) : (
              <motion.div
                key="input"
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="flex gap-3"
              >
                <input
                  type="text"
                  value={question}
                  onChange={(e) => setQuestion(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSendQuestion()}
                  placeholder="e.g. How can I reduce my electricity usage?"
                  className="input flex-1"
                />
                <motion.button
                  onClick={handleSendQuestion}
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  disabled={!question.trim()}
                  className="w-11 h-11 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center flex-shrink-0 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
                >
                  <Send className="w-4 h-4" />
                </motion.button>
              </motion.div>
            )}
          </AnimatePresence>
        </motion.div>

        {/* ── Refresh ───────────────────────────────────────────────── */}
        <motion.div variants={fadeUp} className="text-center pb-4">
          <motion.button
            onClick={fetchData}
            whileHover={{ scale: 1.03 }}
            whileTap={{ scale: 0.97 }}
            className="text-sm text-ink-muted hover:text-emerald-600 inline-flex items-center gap-1.5 transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Refresh my coaching
          </motion.button>
        </motion.div>

      </motion.div>
    </div>
  )
}

function getGreeting(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}
