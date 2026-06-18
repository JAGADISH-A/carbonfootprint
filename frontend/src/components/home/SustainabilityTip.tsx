import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Lightbulb, ArrowRight } from 'lucide-react'

const tips = [
  { text: "Here's something I find interesting — even turning off unused lights for an hour a day can cut household emissions by up to 10%.", action: "Try it tonight" },
  { text: "Walking or cycling for just one short trip instead of driving saves about 0.9 kg CO₂ — that's like turning off your phone charger for 4 days.", action: "Think about your next short trip" },
  { text: "Here's a stat that stuck with me — reducing food waste is one of the single most impactful things you can do. Food production generates huge emissions.", action: "Check your fridge before shopping" },
  { text: "One bus ride instead of a car trip cuts your travel emissions by over 50%. And you can read or scroll while someone else drives.", action: "Next time you commute" },
  { text: "LED bulbs use 75% less energy and last 25x longer. It's one of those rare changes that saves money AND the planet.", action: "Count the bulbs in your home" },
  { text: "Even one plant-based meal a week makes a real difference over time. And there are some genuinely amazing recipes out there.", action: "Pick one meal to try" },
  { text: "Phantom energy is real — chargers and devices plugged in but not in use still consume power. Unplugging them is a tiny habit with a measurable impact.", action: "Walk around and check" },
  { text: "Local and seasonal produce isn't just greener — it's often fresher and tastes better. The shortest supply chain is the best supply chain.", action: "Visit a local market this week" },
]

export default function SustainabilityTip() {
  const [currentTip, setCurrentTip] = useState(0)

  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTip((prev) => (prev + 1) % tips.length)
    }, 10000)
    return () => clearInterval(interval)
  }, [])

  const tip = tips[currentTip]

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.35, ease: [0.22, 1, 0.36, 1] }}
      className="rounded-2xl bg-gradient-to-br from-emerald-50 to-leaf-50 border border-emerald-100/60 p-5 overflow-hidden relative"
    >
      <div className="flex items-start gap-4">
        <motion.div
          className="w-10 h-10 rounded-2xl bg-emerald-100 flex items-center justify-center flex-shrink-0"
          animate={{ rotate: [0, 5, -5, 0] }}
          transition={{ duration: 6, repeat: Infinity, ease: 'easeInOut' }}
        >
          <Lightbulb className="w-5 h-5 text-emerald-600" />
        </motion.div>
        <div className="flex-1 min-h-[48px]">
          <p className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider mb-2">
            Did you know?
          </p>
          <div className="relative">
            <AnimatePresence mode="wait">
              <motion.div
                key={currentTip}
                initial={{ opacity: 0, y: 8, filter: 'blur(2px)' }}
                animate={{ opacity: 1, y: 0, filter: 'blur(0px)' }}
                exit={{ opacity: 0, y: -8, filter: 'blur(2px)' }}
                transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
              >
                <p className="text-sm text-emerald-800/80 leading-relaxed mb-2">
                  {tip.text}
                </p>
                <div className="flex items-center gap-1 text-emerald-600">
                  <span className="text-xs font-medium">{tip.action}</span>
                  <ArrowRight className="w-3 h-3" />
                </div>
              </motion.div>
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
