import { motion } from 'framer-motion'
import {
  Flame,
  PieChart,
  Lightbulb,
  TrendingUp,
  ShieldCheck,
  Zap,
} from 'lucide-react'
import type { ChatCardData } from '@/types/activity'

interface ChatCardsProps {
  cards: ChatCardData[]
}

const ICONS: Record<string, React.ElementType> = {
  carbon: Flame,
  category: PieChart,
  opportunity: Lightbulb,
  trend: TrendingUp,
  confidence: ShieldCheck,
  action: Zap,
}

const COLOR_MAP: Record<string, { bg: string; icon: string; value: string; border: string }> = {
  emerald: { bg: 'bg-emerald-50', icon: 'bg-emerald-500', value: 'text-emerald-700', border: 'border-emerald-100' },
  amber: { bg: 'bg-amber-50', icon: 'bg-amber-500', value: 'text-amber-700', border: 'border-amber-100' },
  blue: { bg: 'bg-blue-50', icon: 'bg-blue-500', value: 'text-blue-700', border: 'border-blue-100' },
  red: { bg: 'bg-red-50', icon: 'bg-red-500', value: 'text-red-700', border: 'border-red-100' },
  purple: { bg: 'bg-purple-50', icon: 'bg-purple-500', value: 'text-purple-700', border: 'border-purple-100' },
}

const DEFAULT_COLOR = { bg: 'bg-emerald-50', icon: 'bg-emerald-500', value: 'text-emerald-700', border: 'border-emerald-100' }

export default function ChatCards({ cards }: ChatCardsProps) {
  if (!cards || cards.length === 0) return null

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-1.5">
      {cards.map((card, i) => {
        const Icon = ICONS[card.icon] || Zap
        const colors = COLOR_MAP[card.color] || DEFAULT_COLOR

        return (
          <motion.div
            key={i}
            initial={{ opacity: 0, y: 4 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.25, delay: 0.05 + i * 0.04, ease: [0.22, 1, 0.36, 1] }}
            className={`${colors.bg} ${colors.border} border rounded-lg p-2.5 flex items-start gap-2`}
          >
            <div className={`${colors.icon} w-6 h-6 rounded-md flex items-center justify-center flex-shrink-0`}>
              <Icon className="w-3 h-3 text-white" />
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-[9px] font-semibold text-ink-muted uppercase tracking-wider leading-none mb-0.5">
                {card.title}
              </p>
              <p className={`text-xs font-bold ${colors.value} leading-tight`}>
                {card.value}
              </p>
              {card.description && (
                <p className="text-[10px] text-ink-muted mt-0.5 leading-snug line-clamp-2">
                  {card.description}
                </p>
              )}
            </div>
          </motion.div>
        )
      })}
    </div>
  )
}
