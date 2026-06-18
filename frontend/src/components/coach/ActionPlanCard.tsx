import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import type { ActionPlanItem } from '@/types/activity'

interface ActionPlanCardProps {
  item: ActionPlanItem
  index: number
}

const priorityColors = [
  'from-emerald-500 to-emerald-600',
  'from-leaf-500 to-leaf-600',
  'from-teal-500 to-teal-600',
]

const priorityBg = [
  'bg-emerald-50 border-emerald-100',
  'bg-leaf-50 border-leaf-100',
  'bg-teal-50 border-teal-100',
]

export default function ActionPlanCard({ item, index }: ActionPlanCardProps) {
  const navigate = useNavigate()

  return (
    <motion.div
      initial={{ opacity: 0, x: -12 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{
        duration: 0.5,
        delay: 0.4 + index * 0.1,
        ease: [0.22, 1, 0.36, 1],
      }}
      whileHover={{ y: -2, transition: { duration: 0.2 } }}
      className={`rounded-xl border p-4 ${priorityBg[index] ?? priorityBg[2]}`}
    >
      <div className="flex items-start gap-3">
        <div
          className={`w-8 h-8 rounded-lg bg-gradient-to-br ${priorityColors[index] ?? priorityColors[2]} flex items-center justify-center flex-shrink-0 shadow-sm`}
        >
          <span className="text-sm font-bold text-white">{item.priority}</span>
        </div>
        <div className="flex-1 min-w-0">
          <h4 className="text-sm font-semibold text-ink mb-1">{item.title}</h4>
          <p className="text-xs text-ink-muted leading-relaxed mb-2">{item.whyItMatters}</p>
          <div className="flex items-center justify-between">
            <p className="text-[11px] text-emerald-700 font-medium">{item.whatToDo}</p>
            <motion.button
              onClick={() => navigate('/coach')}
              whileHover={{ x: 2 }}
              whileTap={{ scale: 0.95 }}
              className="text-emerald-600 hover:text-emerald-700 flex-shrink-0 ml-2"
            >
              <ArrowRight className="w-4 h-4" />
            </motion.button>
          </div>
        </div>
      </div>
    </motion.div>
  )
}
