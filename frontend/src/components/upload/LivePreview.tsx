import { motion, AnimatePresence } from 'framer-motion'
import { Store, ShoppingCart, Calendar, DollarSign, Leaf, Sparkles } from 'lucide-react'

interface DetectedField {
  label: string
  value: string | null
  icon: React.ReactNode
  confidence?: 'high' | 'medium' | 'low'
}

interface LivePreviewProps {
  fields: DetectedField[]
  visible: boolean
}

const confidenceColors = {
  high: 'bg-emerald-100 text-emerald-700',
  medium: 'bg-amber-100 text-amber-700',
  low: 'bg-gray-100 text-gray-600',
}

export default function LivePreview({ fields, visible }: LivePreviewProps) {
  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          exit={{ opacity: 0, height: 0 }}
          transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
          className="overflow-hidden"
        >
          <div className="bg-gradient-to-br from-cream-50/80 to-card border border-border-light rounded-2xl p-4">
            <div className="flex items-center gap-2 mb-3">
              <div className="w-6 h-6 rounded-lg bg-emerald-100 flex items-center justify-center">
                <Sparkles className="w-3 h-3 text-emerald-600" />
              </div>
              <p className="text-[10px] font-bold text-ink-muted uppercase tracking-wider">
                Detected information
              </p>
            </div>

            <div className="space-y-2">
              {fields.map((field, i) => (
                <motion.div
                  key={field.label}
                  initial={{ opacity: 0, x: -8 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.1, duration: 0.3 }}
                  className="flex items-center gap-2.5 py-1.5"
                >
                  <div className="w-6 h-6 rounded-md bg-surface flex items-center justify-center flex-shrink-0">
                    {field.icon}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-[10px] text-ink-muted">{field.label}</p>
                    <AnimatePresence mode="wait">
                      {field.value ? (
                        <motion.p
                          key={field.value}
                          initial={{ opacity: 0, y: 4 }}
                          animate={{ opacity: 1, y: 0 }}
                          className="text-sm font-medium text-ink truncate"
                        >
                          {field.value}
                        </motion.p>
                      ) : (
                        <motion.div
                          className="h-4 w-20 bg-surface rounded animate-pulse"
                        />
                      )}
                    </AnimatePresence>
                  </div>
                  {field.confidence && field.value && (
                    <motion.span
                      initial={{ opacity: 0, scale: 0.8 }}
                      animate={{ opacity: 1, scale: 1 }}
                      className={`text-[9px] font-semibold px-1.5 py-0.5 rounded-full ${confidenceColors[field.confidence]}`}
                    >
                      {field.confidence}
                    </motion.span>
                  )}
                </motion.div>
              ))}
            </div>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  )
}

// Export field builders for easy use
export function buildDetectedFields(data: {
  store?: string | null
  items?: string[]
  category?: string | null
  date?: string | null
  amount?: number | null
  carbon?: number | null
}): DetectedField[] {
  return [
    {
      label: 'Store',
      value: data.store || null,
      icon: <Store className="w-3 h-3 text-ink-faint" />,
      confidence: data.store ? 'high' : undefined,
    },
    {
      label: 'Items',
      value: data.items?.join(', ') || null,
      icon: <ShoppingCart className="w-3 h-3 text-ink-faint" />,
      confidence: data.items?.length ? 'medium' : undefined,
    },
    {
      label: 'Category',
      value: data.category
        ? data.category.charAt(0) + data.category.slice(1).toLowerCase()
        : null,
      icon: <Leaf className="w-3 h-3 text-ink-faint" />,
      confidence: data.category ? 'high' : undefined,
    },
    {
      label: 'Date',
      value: data.date
        ? new Date(data.date).toLocaleDateString('en-US', {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
          })
        : null,
      icon: <Calendar className="w-3 h-3 text-ink-faint" />,
      confidence: data.date ? 'high' : undefined,
    },
    {
      label: 'Amount',
      value: data.amount ? `$${data.amount.toFixed(2)}` : null,
      icon: <DollarSign className="w-3 h-3 text-ink-faint" />,
      confidence: data.amount ? 'medium' : undefined,
    },
    {
      label: 'Carbon footprint',
      value: data.carbon ? `${data.carbon.toFixed(2)} kg CO₂e` : null,
      icon: <Leaf className="w-3 h-3 text-emerald-500" />,
      confidence: data.carbon ? 'high' : undefined,
    },
  ]
}
