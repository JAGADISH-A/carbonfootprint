import { useState, useCallback, useRef, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import {
  Upload,
  CheckCircle2,
  ScanLine,
  Brain,
  Leaf,
  Sparkles,
  Zap,
} from 'lucide-react'
import { uploadReceipt, getCarbonAnalytics } from '@/api/services'
import type { TopEmissionActivity } from '@/types/activity'

type Phase = 'idle' | 'dragging' | 'uploading' | 'scanning' | 'extracting' | 'calculating' | 'success' | 'error'

const processingSteps = [
  { phase: 'uploading' as const, icon: Upload, label: 'Uploading file', detail: 'Securely sending to server...' },
  { phase: 'scanning' as const, icon: ScanLine, label: 'Scanning receipt', detail: 'OCR is reading your document...' },
  { phase: 'extracting' as const, icon: Brain, label: 'AI extraction', detail: 'Identifying merchant, items, and amounts...' },
  { phase: 'calculating' as const, icon: Leaf, label: 'Carbon calculation', detail: 'Computing your environmental impact...' },
]

function ProcessingAnimation({ currentPhase }: { currentPhase: Phase }) {
  const currentIdx = processingSteps.findIndex((s) => s.phase === currentPhase)
  const currentStep = processingSteps[currentIdx]

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="flex flex-col items-center py-8"
    >
      {/* Animated scanner ring */}
      <div className="relative w-28 h-28 mb-8">
        {/* Outer rotating ring */}
        <motion.svg
          className="absolute inset-0 w-full h-full"
          viewBox="0 0 120 120"
          animate={{ rotate: 360 }}
          transition={{ duration: 3, repeat: Infinity, ease: 'linear' }}
        >
          <circle cx="60" cy="60" r="54" fill="none" stroke="#E8F0E8" strokeWidth="2" />
          <motion.circle
            cx="60"
            cy="60"
            r="54"
            fill="none"
            stroke="url(#scanGradient)"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeDasharray="80 250"
            initial={{ strokeDashoffset: 0 }}
            animate={{ strokeDashoffset: -330 }}
            transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
          />
          <defs>
            <linearGradient id="scanGradient" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#10B981" />
              <stop offset="50%" stopColor="#34D399" />
              <stop offset="100%" stopColor="#10B981" />
            </linearGradient>
          </defs>
        </motion.svg>

        {/* Pulsing inner circle */}
        <motion.div
          className="absolute inset-3 rounded-full bg-emerald-50 flex items-center justify-center"
          animate={{
            scale: [1, 1.04, 1],
            boxShadow: [
              '0 0 0 0 rgba(16,185,129,0)',
              '0 0 0 8px rgba(16,185,129,0.08)',
              '0 0 0 0 rgba(16,185,129,0)',
            ],
          }}
          transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
        >
          <AnimatePresence mode="wait">
            <motion.div
              key={currentPhase}
              initial={{ opacity: 0, scale: 0.5, rotate: -20 }}
              animate={{ opacity: 1, scale: 1, rotate: 0 }}
              exit={{ opacity: 0, scale: 0.5, rotate: 20 }}
              transition={{ duration: 0.3, type: 'spring', stiffness: 300 }}
            >
              {currentStep && <currentStep.icon className="w-8 h-8 text-emerald-600" />}
            </motion.div>
          </AnimatePresence>
        </motion.div>
      </div>

      {/* Step labels */}
      <div className="space-y-3 w-full max-w-xs">
        {processingSteps.map((step, i) => {
          const isActive = step.phase === currentPhase
          const isDone = currentIdx > i
          const StepIcon = step.icon

          return (
            <motion.div
              key={step.phase}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: i * 0.08 }}
              className={`flex items-center gap-3 px-4 py-2.5 rounded-xl transition-all duration-300 ${
                isActive
                  ? 'bg-emerald-50 border border-emerald-200'
                  : isDone
                  ? 'opacity-60'
                  : 'opacity-40'
              }`}
            >
              <div className={`w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 ${
                isDone
                  ? 'bg-emerald-500'
                  : isActive
                  ? 'bg-emerald-100'
                  : 'bg-surface'
              }`}>
                {isDone ? (
                  <motion.div
                    initial={{ scale: 0 }}
                    animate={{ scale: 1 }}
                    transition={{ type: 'spring', stiffness: 500 }}
                  >
                    <CheckCircle2 className="w-4 h-4 text-white" />
                  </motion.div>
                ) : (
                  <StepIcon className={`w-3.5 h-3.5 ${isActive ? 'text-emerald-600' : 'text-ink-faint'}`} />
                )}
              </div>
              <div className="flex-1 min-w-0">
                <p className={`text-sm font-medium ${isActive ? 'text-ink' : 'text-ink-muted'}`}>
                  {step.label}
                </p>
                {isActive && (
                  <motion.p
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    className="text-xs text-ink-faint mt-0.5"
                  >
                    {step.detail}
                  </motion.p>
                )}
              </div>
              {isActive && (
                <motion.div
                  className="w-1.5 h-1.5 rounded-full bg-emerald-500"
                  animate={{ scale: [1, 1.4, 1], opacity: [1, 0.6, 1] }}
                  transition={{ duration: 1, repeat: Infinity }}
                />
              )}
            </motion.div>
          )
        })}
      </div>
    </motion.div>
  )
}

function SuccessCard({ activity, onNavigate }: { activity: TopEmissionActivity; onNavigate: () => void }) {
  const [showCoach, setShowCoach] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setShowCoach(true), 1500)
    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    const timer = setTimeout(onNavigate, 6000)
    return () => clearTimeout(timer)
  }, [onNavigate])

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ type: 'spring', stiffness: 200, damping: 20 }}
      className="flex flex-col items-center py-6"
    >
      {/* Success icon with celebration */}
      <div className="relative mb-6">
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ type: 'spring', stiffness: 300, damping: 15, delay: 0.1 }}
          className="w-20 h-20 rounded-full bg-emerald-50 flex items-center justify-center"
        >
          <motion.div
            initial={{ scale: 0, rotate: -45 }}
            animate={{ scale: 1, rotate: 0 }}
            transition={{ delay: 0.3, type: 'spring', stiffness: 400 }}
          >
            <CheckCircle2 className="w-10 h-10 text-emerald-500" />
          </motion.div>
        </motion.div>

        {/* Floating sparkles */}
        {[0, 1, 2, 3, 4, 5].map((i) => (
          <motion.div
            key={i}
            className="absolute"
            initial={{ opacity: 0, scale: 0 }}
            animate={{
              opacity: [0, 1, 0],
              scale: [0, 1, 0.5],
              x: Math.cos((i * 60 * Math.PI) / 180) * 50,
              y: Math.sin((i * 60 * Math.PI) / 180) * 50,
            }}
            transition={{ delay: 0.4 + i * 0.08, duration: 0.8, ease: 'easeOut' }}
          >
            <Sparkles className="w-3 h-3 text-amber-400" />
          </motion.div>
        ))}
      </div>

      <motion.h2
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.4 }}
        className="text-xl font-bold text-ink mb-1"
      >
        Receipt Processed <span className="inline-block ml-1">🌿</span>
      </motion.h2>

      {/* Result card */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.6, duration: 0.5 }}
        className="w-full max-w-sm mt-6 bg-card rounded-3xl border border-border-light shadow-card p-6"
      >
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <span className="text-sm text-ink-muted">Merchant</span>
            <span className="text-sm font-semibold text-ink">{activity.merchant || 'Unknown'}</span>
          </div>
          <div className="h-px bg-border-light" />
          <div className="flex items-center justify-between">
            <span className="text-sm text-ink-muted">Category</span>
            <span className="badge-green">{activity.category.charAt(0) + activity.category.slice(1).toLowerCase()}</span>
          </div>
          <div className="h-px bg-border-light" />
          <div className="flex items-center justify-between">
            <span className="text-sm text-ink-muted">Carbon</span>
            <span className="text-lg font-bold text-ink">{Number(activity.carbonKg ?? 0).toFixed(2)} kg CO₂e</span>
          </div>
        </div>
      </motion.div>

      {/* AI Coach generating */}
      <AnimatePresence>
        {showCoach && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-5 flex items-center gap-2 text-sm text-emerald-600"
          >
            <motion.div
              animate={{ rotate: [0, 360] }}
              transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
            >
              <Sparkles className="w-4 h-4" />
            </motion.div>
            AI Coach is generating recommendations...
          </motion.div>
        )}
      </AnimatePresence>

      {/* Countdown to redirect */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 2 }}
        className="mt-6 flex items-center gap-2 text-xs text-ink-faint"
      >
        <span>Redirecting to dashboard</span>
        <motion.div
          className="flex gap-1"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
        >
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              className="w-1 h-1 rounded-full bg-ink-faint"
              animate={{ opacity: [0.3, 1, 0.3] }}
              transition={{ duration: 1.2, repeat: Infinity, delay: i * 0.2 }}
            />
          ))}
        </motion.div>
      </motion.div>
    </motion.div>
  )
}

export default function UploadPage() {
  const navigate = useNavigate()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [phase, setPhase] = useState<Phase>('idle')
  const [dragActive, setDragActive] = useState(false)
  const [fileName, setFileName] = useState('')
  const [result, setResult] = useState<TopEmissionActivity | null>(null)
  const [error, setError] = useState('')

  const processFile = useCallback(async (file: File) => {
    setFileName(file.name)
    setPhase('uploading')

    // Simulate processing phases for visual feedback
    const timers: ReturnType<typeof setTimeout>[] = []

    try {
      // Start upload
      const uploadPromise = uploadReceipt(file)

      // Show scanning phase after 800ms
      timers.push(setTimeout(() => setPhase('scanning'), 800))
      timers.push(setTimeout(() => setPhase('extracting'), 2000))
      timers.push(setTimeout(() => setPhase('calculating'), 3200))

      const uploadResult = await uploadPromise

      if (!uploadResult.success) {
        throw new Error(uploadResult.message || 'Upload failed')
      }

      // Wait a moment for backend to finish processing, then fetch analytics
      await new Promise((resolve) => setTimeout(resolve, 1500))

      const analytics = await getCarbonAnalytics()
      const activities = analytics.data?.topActivities ?? []

      if (activities.length > 0) {
        setResult(activities[0])
      } else {
        // No activity data returned, show generic success
        setResult({
          activityId: 'unknown',
          merchant: file.name.replace(/\.[^.]+$/, ''),
          category: 'OTHER',
          carbonKg: 0,
          description: null,
          occurredAt: new Date().toISOString(),
        })
      }

      setPhase('success')
    } catch (err) {
      timers.forEach(clearTimeout)
      setError(err instanceof Error ? err.message : 'Something went wrong')
      setPhase('error')
    }
  }, [])

  const handleFile = useCallback((file: File) => {
    if (file.size > 10 * 1024 * 1024) {
      setError('File too large. Maximum size is 10MB.')
      setPhase('error')
      return
    }
    processFile(file)
  }, [processFile])

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
      if (phase === 'idle') setPhase('idle')
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }, [phase])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)
    if (e.dataTransfer.files?.[0]) {
      handleFile(e.dataTransfer.files[0])
    }
  }, [handleFile])

  const handleFileInput = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files?.[0]) {
      handleFile(e.target.files[0])
    }
  }, [handleFile])

  const reset = useCallback(() => {
    setPhase('idle')
    setResult(null)
    setError('')
    setFileName('')
  }, [])

  const handleNavigate = useCallback(() => {
    navigate('/')
  }, [navigate])

  const isProcessing = phase === 'uploading' || phase === 'scanning' || phase === 'extracting' || phase === 'calculating'

  return (
    <div className="max-w-2xl mx-auto">
      {/* Header illustration */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        className="text-center mb-10"
      >
        {/* Animated illustration */}
        <div className="relative w-40 h-32 mx-auto mb-6">
          {/* Receipt */}
          <motion.div
            className="absolute left-4 top-0 w-16 h-24 bg-card rounded-xl border border-border-light shadow-card flex flex-col items-center justify-center gap-1.5 overflow-hidden"
            animate={isProcessing ? { x: [0, 20, 60], opacity: [1, 1, 0], rotate: [0, 0, 5] } : {}}
            transition={{ duration: 1.5, ease: 'easeInOut' }}
          >
            <div className="w-8 h-1 bg-surface rounded-full" />
            <div className="w-10 h-1 bg-surface rounded-full" />
            <div className="w-6 h-1 bg-surface rounded-full" />
            <div className="w-9 h-1 bg-surface rounded-full" />
            <div className="w-7 h-1 bg-surface rounded-full" />
          </motion.div>

          {/* Scanning beam */}
          {(isProcessing || phase === 'success') && (
            <motion.div
              className="absolute left-6 w-12 h-0.5 bg-gradient-to-r from-transparent via-emerald-400 to-transparent rounded-full"
              initial={{ top: 0, opacity: 0 }}
              animate={{ top: [0, 90, 0], opacity: [0, 0.8, 0] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
            />
          )}

          {/* Arrow / conversion */}
          <motion.div
            className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2"
            animate={isProcessing ? { scale: [1, 1.2, 1], rotate: [0, 10, 0] } : {}}
            transition={{ duration: 1, repeat: Infinity }}
          >
            <Zap className="w-5 h-5 text-emerald-400" />
          </motion.div>

          {/* Leaf (result) */}
          <motion.div
            className="absolute right-4 top-2 w-16 h-20 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-center"
            initial={{ opacity: 0.4, scale: 0.9 }}
            animate={
              phase === 'success'
                ? { opacity: 1, scale: 1, boxShadow: '0 4px 20px rgba(16,185,129,0.15)' }
                : isProcessing
                ? { opacity: [0.4, 0.8, 0.4], scale: [0.95, 1, 0.95] }
                : {}
            }
            transition={{ duration: 1.5, repeat: isProcessing ? Infinity : 0 }}
          >
            <Leaf className={`w-8 h-8 ${phase === 'success' ? 'text-emerald-500' : 'text-emerald-300'}`} />
          </motion.div>
        </div>

        <h1 className="text-2xl sm:text-3xl font-bold text-ink tracking-tight mb-2">
          {phase === 'success' ? 'All done!' : 'Turn receipts into impact'}
        </h1>
        <p className="text-ink-muted text-sm max-w-md mx-auto">
          {phase === 'success'
            ? 'Your receipt has been processed and carbon impact calculated.'
            : 'Upload a receipt and our AI will extract the data, calculate your carbon footprint, and suggest ways to reduce it.'}
        </p>
      </motion.div>

      {/* Main content area */}
      <AnimatePresence mode="wait">
        {/* Idle / Dragging state */}
        {(phase === 'idle' || phase === 'dragging') && (
          <motion.div
            key="dropzone"
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.4 }}
          >
            <div
              onDragEnter={handleDrag}
              onDragLeave={handleDrag}
              onDragOver={handleDrag}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`relative rounded-3xl border-2 border-dashed p-14 text-center cursor-pointer
                transition-all duration-300 group ${
                  dragActive
                    ? 'border-emerald-400 bg-emerald-50/80 scale-[1.01]'
                    : 'border-border hover:border-emerald-300 hover:bg-emerald-50/30'
                }`}
            >
              <input
                ref={fileInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp,application/pdf"
                className="hidden"
                onChange={handleFileInput}
              />

              {/* Upload icon */}
              <motion.div
                className="w-16 h-16 rounded-3xl bg-emerald-50 flex items-center justify-center mx-auto mb-5
                           group-hover:bg-emerald-100 transition-colors duration-300"
                animate={dragActive ? { scale: [1, 1.1, 1], y: [0, -4, 0] } : {}}
                transition={{ duration: 0.6, repeat: dragActive ? Infinity : 0 }}
              >
                <Upload className={`w-7 h-7 transition-colors duration-300 ${
                  dragActive ? 'text-emerald-600' : 'text-emerald-500 group-hover:text-emerald-600'
                }`} />
              </motion.div>

              <h3 className="text-lg font-semibold text-ink mb-2">
                {dragActive ? 'Release to upload' : 'Drop your receipt here'}
              </h3>
              <p className="text-sm text-ink-muted mb-5">
                or <span className="text-emerald-600 font-medium">browse</span> to choose a file
              </p>

              <div className="flex items-center justify-center gap-2 text-xs text-ink-faint">
                <span>JPG</span>
                <span className="w-1 h-1 rounded-full bg-ink-faint" />
                <span>PNG</span>
                <span className="w-1 h-1 rounded-full bg-ink-faint" />
                <span>PDF</span>
                <span className="w-1 h-1 rounded-full bg-ink-faint" />
                <span>Up to 10MB</span>
              </div>

              {/* Drag overlay shimmer */}
              {dragActive && (
                <motion.div
                  className="absolute inset-0 rounded-3xl bg-gradient-to-r from-emerald-100/0 via-emerald-200/30 to-emerald-100/0"
                  animate={{ x: ['-100%', '100%'] }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
                />
              )}
            </div>
          </motion.div>
        )}

        {/* Processing state */}
        {isProcessing && (
          <motion.div
            key="processing"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="card"
          >
            <div className="text-center mb-2">
              <span className="text-xs text-ink-faint font-medium">{fileName}</span>
            </div>
            <ProcessingAnimation currentPhase={phase} />
          </motion.div>
        )}

        {/* Success state */}
        {phase === 'success' && result && (
          <motion.div
            key="success"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="card"
          >
            <SuccessCard activity={result} onNavigate={handleNavigate} />
          </motion.div>
        )}

        {/* Error state */}
        {phase === 'error' && (
          <motion.div
            key="error"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            className="card text-center py-10"
          >
            <div className="w-14 h-14 rounded-2xl bg-red-50 flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">⚠️</span>
            </div>
            <h3 className="text-lg font-semibold text-ink mb-1">Upload failed</h3>
            <p className="text-sm text-ink-muted mb-6 max-w-sm mx-auto">{error}</p>
            <motion.button
              onClick={reset}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              className="btn-primary"
            >
              Try again
            </motion.button>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  )
}
