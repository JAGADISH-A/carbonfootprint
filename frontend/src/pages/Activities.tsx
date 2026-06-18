import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { ArrowRight } from 'lucide-react'
import EmptyStateCard from '@/components/common/EmptyStateCard'

export default function Activities() {
  const navigate = useNavigate()

  return (
    <div>
      <div className="max-w-2xl mx-auto">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
          className="text-center mb-2"
        >
          <h1 className="text-2xl font-bold text-ink tracking-tight mb-1">Activity History</h1>
          <p className="text-sm text-ink-muted">A timeline of your tracked carbon activities.</p>
        </motion.div>

        <div className="card">
          <EmptyStateCard
            emoji="🌿"
            title="Your journey starts here"
            description="Every sustainable habit begins with one action. Upload your first receipt and watch your impact unfold."
            size="lg"
            action={
              <motion.button
                onClick={() => navigate('/upload')}
                whileHover={{ scale: 1.03, y: -1 }}
                whileTap={{ scale: 0.97 }}
                className="btn-primary inline-flex items-center gap-2"
              >
                Upload your first receipt
                <ArrowRight className="w-4 h-4" />
              </motion.button>
            }
          />
        </div>
      </div>
    </div>
  )
}
