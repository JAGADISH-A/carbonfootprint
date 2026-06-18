import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Leaf } from 'lucide-react'

const tips = [
  'Turning off unused lights can reduce household electricity emissions by up to 10%.',
  'Walking or cycling for short trips instead of driving saves approximately 0.9 kg CO₂ per km.',
  'Reducing food waste is one of the most impactful actions — food production generates significant emissions.',
  'Using public transport instead of a personal car can cut your travel emissions by over 50%.',
  'Switching to LED bulbs uses 75% less energy and lasts 25 times longer than incandescent bulbs.',
  'Eating more plant-based meals even once a week can meaningfully reduce your dietary carbon footprint.',
  'Unplugging chargers and devices when not in use prevents phantom energy consumption.',
  'Choosing local and seasonal produce reduces emissions from transportation and cold storage.',
]

export default function SustainabilityTip() {
  const [currentTip, setCurrentTip] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTip((prev) => (prev + 1) % tips.length)
    }, 8000)
    return () => clearInterval(interval)
  }, [])

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="rounded-3xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/60 p-6 overflow-hidden relative"
    >
      <div className="flex items-start gap-4">
        <motion.div
          className="w-10 h-10 rounded-2xl bg-emerald-100 flex items-center justify-center flex-shrink-0"
          animate={{ rotate: [0, 5, -5, 0] }}
          transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
        >
          <Leaf className="w-5 h-5 text-emerald-600" />
        </motion.div>
        <div className="flex-1 min-h-[48px]">
          <p className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider mb-2">
            Sustainability Tip
          </p>
          <div className="relative">
            <AnimatePresence mode="wait">
              <motion.p
                key={currentTip}
                initial={{ opacity: 0, y: 8, filter: 'blur(2px)' }}
                animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
                exit={{ opacity: 0, y: -8, filter: 'blur(2px)' }}
                transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                className="text-sm text-emerald-800/80 leading-relaxed"
              >
                {tips[currentTip]}
              </motion.p>
            </AnimatePresence>
          </div>
          {/* Progress dots */}
          <div className="flex gap-1.5 mt-3">
            {tips.map((_, i) => (
              <motion.div
                key={i}
                className="h-1 rounded-full bg-emerald-200"
                animate={{
                  width: i === currentTip ? 20 : 4,
                  backgroundColor: i === currentTip ? '#059669' : '#A7F3D0',
                }}
                transition={{ duration: 0.4, ease: 'easeInOut' }}
              />
            ))}
          </div>
        </div>
      </div>
    </motion.div>
  )
}
