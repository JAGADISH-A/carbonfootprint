import { motion } from 'framer-motion'
import { Sparkles } from 'lucide-react'

export default function TypingIndicator() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -4 }}
      transition={{ duration: 0.2, ease: [0.22, 1, 0.36, 1] }}
      className="flex gap-2.5 mb-3"
    >
      <div className="w-7 h-7 rounded-full bg-gradient-to-br from-emerald-600 to-emerald-700 flex items-center justify-center flex-shrink-0">
        <Sparkles className="w-3.5 h-3.5 text-white" />
      </div>
      <div className="flex-1">
        <div className="flex items-center gap-1.5 mb-0.5">
          <span className="text-[10px] font-semibold text-ink-muted">EcoBuddy</span>
          <span className="text-[9px] text-ink-faint">typing...</span>
        </div>
        <div className="bg-white border border-border-light rounded-xl rounded-bl-md px-3 py-2 inline-flex items-center gap-1.5 shadow-sm">
          <motion.span
            className="w-1.5 h-1.5 rounded-full bg-emerald-400"
            animate={{ opacity: [0.3, 1, 0.3], scale: [0.85, 1.1, 0.85] }}
            transition={{ duration: 1.2, repeat: Infinity, delay: 0 }}
          />
          <motion.span
            className="w-1.5 h-1.5 rounded-full bg-emerald-400"
            animate={{ opacity: [0.3, 1, 0.3], scale: [0.85, 1.1, 0.85] }}
            transition={{ duration: 1.2, repeat: Infinity, delay: 0.2 }}
          />
          <motion.span
            className="w-1.5 h-1.5 rounded-full bg-emerald-400"
            animate={{ opacity: [0.3, 1, 0.3], scale: [0.85, 1.1, 0.85] }}
            transition={{ duration: 1.2, repeat: Infinity, delay: 0.4 }}
          />
        </div>
      </div>
    </motion.div>
  )
}
