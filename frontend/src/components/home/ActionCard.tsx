import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import type { ActionRecommendation } from './generateActions'

const confidenceConfig = {
  high: { label: 'High', color: 'bg-emerald-100 text-emerald-700' },
  medium: { label: 'Medium', color: 'bg-amber-100 text-amber-700' },
  low: { label: 'Low', color: 'bg-gray-100 text-gray-600' },
}

const typeConfig = {
  opportunity: { label: 'Biggest opportunity', accent: 'border-l-emerald-500' },
  quick_win: { label: 'Quick win', accent: 'border-l-leaf-500' },
  habit: { label: 'Habit shift', accent: 'border-l-teal-500' },
  challenge: { label: 'Challenge', accent: 'border-l-cyan-500' },
  upload: { label: 'Get started', accent: 'border-l-emerald-400' },
}

interface ActionCardProps {
  action: ActionRecommendation
  index: number
  featured?: boolean
}

export default function ActionCard({ action, index, featured = false }: ActionCardProps) {
  const navigate = useNavigate()
  const confidence = confidenceConfig[action.confidence]
  const type = typeConfig[action.type]

  if (featured) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, delay: 0.1, ease: [0.22, 1, 0.36, 1] }}
        whileHover={{ y: -2, transition: { duration: 0.25, ease: 'easeOut' } }}
        className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-emerald-50 via-card to-leaf-50 border border-emerald-100/60 p-4 cursor-pointer"
        onClick={() => navigate(action.ctaLink)}
      >
        <div className="flex items-start gap-3">
          <div className="w-12 h-12 rounded-xl bg-white/80 border border-emerald-100/50 flex items-center justify-center flex-shrink-0 shadow-sm">
            <span className="text-xl">{action.icon}</span>
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="text-[10px] font-bold text-emerald-600 uppercase tracking-wider">
                {type.label}
              </span>
              <span className={`text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${confidence.color}`}>
                {confidence.label}
              </span>
            </div>

            <h3 className="text-sm font-bold text-ink mb-1 leading-snug">
              {action.title}
            </h3>

            <p className="text-[11px] text-ink-muted leading-relaxed mb-2">
              {action.description}
            </p>

            <div className="flex items-center justify-between">
              {action.estimatedSavingKg !== null && (
                <div className="flex items-center gap-1.5 bg-white/70 rounded-lg px-2 py-1 border border-emerald-100/50">
                  <span className="text-[10px] text-ink-faint">Saving</span>
                  <span className="text-xs font-bold text-emerald-700">
                    {action.estimatedSavingKg < 1
                      ? `${(action.estimatedSavingKg * 1000).toFixed(0)}g`
                      : `${action.estimatedSavingKg.toFixed(1)} kg`}
                  </span>
                  <span className="text-[10px] text-ink-faint">CO₂e</span>
                </div>
              )}

              <button
                onClick={() => navigate(action.ctaLink)}
                className="btn-primary text-xs py-2 px-4"
              >
                {action.ctaLabel}
              </button>
            </div>
          </div>
        </div>
      </motion.div>
    )
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5, delay: 0.15 + index * 0.08, ease: [0.22, 1, 0.36, 1] }}
      className={`card-interactive border-l-4 ${type.accent} p-3 cursor-pointer`}
      onClick={() => navigate(action.ctaLink)}
    >
      <div className="flex items-start gap-2.5">
        <div className="w-9 h-9 rounded-lg bg-cream-50/80 border border-border-light flex items-center justify-center flex-shrink-0">
          <span className="text-base">{action.icon}</span>
        </div>

        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-1.5 mb-0.5">
            <span className="text-[9px] font-bold text-ink-faint uppercase tracking-wider">
              {type.label}
            </span>
            <span className={`text-[8px] font-semibold px-1 py-0.5 rounded-full ${confidence.color}`}>
              {confidence.label}
            </span>
          </div>

          <h4 className="text-sm font-semibold text-ink mb-0.5 leading-snug line-clamp-2">
            {action.title}
          </h4>

          <p className="text-[10px] text-ink-muted leading-relaxed mb-1.5 line-clamp-2">
            {action.description}
          </p>

          <div className="flex items-center justify-between">
            {action.estimatedSavingKg !== null ? (
              <div className="flex items-center gap-1">
                <span className="text-xs font-bold text-emerald-600">
                  ↓ {action.estimatedSavingKg < 1
                    ? `${(action.estimatedSavingKg * 1000).toFixed(0)}g`
                    : `${action.estimatedSavingKg.toFixed(1)} kg`}
                </span>
                <span className="text-[9px] text-ink-faint">CO₂e</span>
              </div>
            ) : (
              <span />
            )}

            <span className="text-[10px] font-medium text-emerald-600">
              {action.ctaLabel}
            </span>
          </div>
        </div>
      </div>
    </motion.div>
  )
}
