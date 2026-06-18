import { motion, AnimatePresence } from 'framer-motion'
import {
  Upload,
  ScanLine,
  Brain,
  FileText,
  Calculator,
  Shield,
  CheckCircle2,
  Loader2,
} from 'lucide-react'

export type WorkflowStage =
  | 'idle'
  | 'uploading'
  | 'reading'
  | 'understanding'
  | 'extracting'
  | 'calculating'
  | 'confidence'
  | 'saving'
  | 'success'
  | 'error'

interface StageConfig {
  id: WorkflowStage
  icon: React.ReactNode
  activeIcon: React.ReactNode
  label: string
  description: string
  tip: string
}

const stages: StageConfig[] = [
  {
    id: 'uploading',
    icon: <Upload className="w-4 h-4" />,
    activeIcon: <Upload className="w-4 h-4" />,
    label: 'Uploading',
    description: 'Securely sending your receipt...',
    tip: 'Files are encrypted in transit',
  },
  {
    id: 'reading',
    icon: <ScanLine className="w-4 h-4" />,
    activeIcon: (
      <motion.div
        animate={{ rotateY: [0, 180, 360] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
      >
        <ScanLine className="w-4 h-4" />
      </motion.div>
    ),
    label: 'Reading receipt',
    description: 'OCR is scanning your document...',
    tip: 'Reading text, tables, and logos',
  },
  {
    id: 'understanding',
    icon: <Brain className="w-4 h-4" />,
    activeIcon: (
      <motion.div
        animate={{ scale: [1, 1.2, 1] }}
        transition={{ duration: 1.5, repeat: Infinity }}
      >
        <Brain className="w-4 h-4" />
      </motion.div>
    ),
    label: 'Understanding purchase',
    description: 'AI is identifying products and context...',
    tip: 'Making sense of what you bought',
  },
  {
    id: 'extracting',
    icon: <FileText className="w-4 h-4" />,
    activeIcon: <FileText className="w-4 h-4" />,
    label: 'Extracting information',
    description: 'Pulling out key details...',
    tip: 'Store, items, date, amount',
  },
  {
    id: 'calculating',
    icon: <Calculator className="w-4 h-4" />,
    activeIcon: (
      <motion.div
        animate={{ rotate: [0, 360] }}
        transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
      >
        <Calculator className="w-4 h-4" />
      </motion.div>
    ),
    label: 'Carbon calculation',
    description: 'Estimating your environmental impact...',
    tip: 'Using emission factors and AI models',
  },
  {
    id: 'confidence',
    icon: <Shield className="w-4 h-4" />,
    activeIcon: <Shield className="w-4 h-4" />,
    label: 'AI confidence check',
    description: 'Verifying the results...',
    tip: 'Making sure the numbers make sense',
  },
  {
    id: 'saving',
    icon: <CheckCircle2 className="w-4 h-4" />,
    activeIcon: <CheckCircle2 className="w-4 h-4" />,
    label: 'Saving activity',
    description: 'Adding to your carbon profile...',
    tip: 'Almost done!',
  },
]

interface ProcessingTimelineProps {
  currentStage: WorkflowStage
  completedStages: WorkflowStage[]
}

export default function ProcessingTimeline({
  currentStage,
  completedStages,
}: ProcessingTimelineProps) {
  const currentIdx = stages.findIndex((s) => s.id === currentStage)
  const currentConfig = stages[currentIdx]

  return (
    <div className="w-full max-w-md mx-auto">
      {/* Current stage hero */}
      <AnimatePresence mode="wait">
        {currentConfig && currentStage !== 'success' && currentStage !== 'error' && (
          <motion.div
            key={currentStage}
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 10 }}
            transition={{ duration: 0.3 }}
            className="text-center mb-6"
          >
            <motion.div
              className="w-16 h-16 rounded-2xl bg-emerald-50 border border-emerald-100 flex items-center justify-center mx-auto mb-3"
              animate={{
                scale: [1, 1.05, 1],
                boxShadow: [
                  '0 0 0 0 rgba(16,185,129,0)',
                  '0 0 0 12px rgba(16,185,129,0.08)',
                  '0 0 0 0 rgba(16,185,129,0)',
                ],
              }}
              transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
            >
              <div className="text-emerald-600">{currentConfig.activeIcon}</div>
            </motion.div>
            <h3 className="text-sm font-semibold text-ink mb-1">
              {currentConfig.description}
            </h3>
            <p className="text-[11px] text-ink-muted">{currentConfig.tip}</p>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Timeline */}
      <div className="relative">
        {/* Vertical line */}
        <div className="absolute left-[15px] top-0 bottom-0 w-px bg-border-light" />

        {/* Animated progress line */}
        <motion.div
          className="absolute left-[15px] top-0 w-px bg-emerald-400"
          initial={{ height: 0 }}
          animate={{
            height: `${(currentIdx / (stages.length - 1)) * 100}%`,
          }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
        />

        {/* Stage items */}
        <div className="space-y-1">
          {stages.map((stage, i) => {
            const isActive = stage.id === currentStage
            const isDone = completedStages.includes(stage.id)
            const isPending = !isActive && !isDone

            return (
              <motion.div
                key={stage.id}
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: i * 0.05, duration: 0.3 }}
                className={`relative flex items-center gap-3 py-2 px-3 rounded-xl transition-all duration-300 ${
                  isActive
                    ? 'bg-emerald-50/80'
                    : isPending
                    ? 'opacity-40'
                    : ''
                }`}
              >
                {/* Stage indicator */}
                <div className="relative z-10">
                  <motion.div
                    className={`w-[30px] h-[30px] rounded-full flex items-center justify-center flex-shrink-0 transition-all duration-300 ${
                      isDone
                        ? 'bg-emerald-500'
                        : isActive
                        ? 'bg-emerald-100 border-2 border-emerald-400'
                        : 'bg-surface border border-border-light'
                    }`}
                    animate={
                      isActive
                        ? {
                            scale: [1, 1.1, 1],
                            borderColor: ['#34D399', '#10B981', '#34D399'],
                          }
                        : {}
                    }
                    transition={isActive ? { duration: 1.5, repeat: Infinity } : {}}
                  >
                    {isDone ? (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: 'spring', stiffness: 500 }}
                      >
                        <CheckCircle2 className="w-4 h-4 text-white" />
                      </motion.div>
                    ) : isActive ? (
                      <motion.div
                        animate={{ rotate: 360 }}
                        transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
                      >
                        <Loader2 className="w-4 h-4 text-emerald-600" />
                      </motion.div>
                    ) : (
                      <div className="text-ink-faint">{stage.icon}</div>
                    )}
                  </motion.div>
                </div>

                {/* Stage content */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <p
                      className={`text-sm font-medium ${
                        isActive ? 'text-ink' : isDone ? 'text-ink' : 'text-ink-muted'
                      }`}
                    >
                      {stage.label}
                    </p>
                    {isActive && (
                      <motion.div
                        className="flex gap-0.5"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                      >
                        {[0, 1, 2].map((j) => (
                          <motion.div
                            key={j}
                            className="w-1 h-1 rounded-full bg-emerald-500"
                            animate={{ opacity: [0.3, 1, 0.3] }}
                            transition={{
                              duration: 1,
                              repeat: Infinity,
                              delay: j * 0.2,
                            }}
                          />
                        ))}
                      </motion.div>
                    )}
                  </div>
                </div>
              </motion.div>
            )
          })}
        </div>
      </div>
    </div>
  )
}
