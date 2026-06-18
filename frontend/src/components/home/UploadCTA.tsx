import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { FileUp, ArrowRight } from 'lucide-react'

export default function UploadCTA() {
  const navigate = useNavigate()

  return (
    <motion.button
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.2, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ scale: 1.015, y: -2 }}
      whileTap={{ scale: 0.985 }}
      onClick={() => navigate('/upload')}
      className="w-full card-interactive flex items-center gap-5 text-left group"
    >
      <motion.div
        className="w-14 h-14 rounded-2xl bg-emerald-500 flex items-center justify-center flex-shrink-0 shadow-emerald"
        whileHover={{ rotate: [0, -8, 8, 0] }}
        transition={{ duration: 0.5 }}
      >
        <FileUp className="w-6 h-6 text-white" />
      </motion.div>

      <div className="flex-1">
        <h3 className="text-base font-semibold text-ink mb-0.5">Got a receipt?</h3>
        <p className="text-sm text-ink-muted">
          Drop it in and I'll analyze your carbon footprint in seconds.
        </p>
      </div>

      <motion.div
        className="flex-shrink-0"
        animate={{ x: [0, 4, 0] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut', delay: 1 }}
      >
        <ArrowRight className="w-5 h-5 text-ink-ghost group-hover:text-emerald-500 transition-colors duration-200" />
      </motion.div>
    </motion.button>
  )
}
