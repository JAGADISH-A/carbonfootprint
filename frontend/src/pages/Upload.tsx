import { useState, useCallback, useRef } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { Upload, Zap, ArrowRight } from 'lucide-react'
import { uploadReceipt, getCarbonAnalytics } from '@/api/services'
import type { TopEmissionActivity } from '@/types/activity'
import {
  ProcessingTimeline,
  LivePreview,
  SuccessCelebration,
  buildDetectedFields,
} from '@/components/upload'
import type { WorkflowStage } from '@/components/upload'

type Phase = 'idle' | 'dragging' | 'processing' | 'success' | 'error'

const processingMessages: Record<WorkflowStage, string> = {
  idle: '',
  uploading: 'Securely sending your receipt...',
  reading: 'Reading receipt...',
  understanding: 'Identifying products...',
  extracting: 'Extracting details...',
  calculating: 'Estimating emissions...',
  confidence: 'Verifying results...',
  saving: 'Saving to your profile...',
  success: '',
  error: '',
}

export default function UploadPage() {
  const navigate = useNavigate()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [phase, setPhase] = useState<Phase>('idle')
  const [dragActive, setDragActive] = useState(false)
  const [fileName, setFileName] = useState('')
  const [result, setResult] = useState<TopEmissionActivity | null>(null)
  const [error, setError] = useState('')

  // Workflow stages
  const [currentStage, setCurrentStage] = useState<WorkflowStage>('idle')
  const [completedStages, setCompletedStages] = useState<WorkflowStage[]>([])

  // Live preview data
  const [previewData, setPreviewData] = useState({
    store: null as string | null,
    items: [] as string[],
    category: null as string | null,
    date: null as string | null,
    amount: null as number | null,
    carbon: null as number | null,
  })

  const advanceStage = useCallback((stage: WorkflowStage) => {
    setCurrentStage(stage)
    setCompletedStages((prev) => [...prev, stage])
  }, [])

  const processFile = useCallback(async (file: File) => {
    setFileName(file.name)
    setPhase('processing')
    setCurrentStage('uploading')
    setCompletedStages([])
    setPreviewData({
      store: null,
      items: [],
      category: null,
      date: null,
      amount: null,
      carbon: null,
    })

    try {
      // Stage 1: Upload
      const uploadPromise = uploadReceipt(file)

      // Simulate OCR reading with staged progress
      setTimeout(() => {
        advanceStage('reading')
        // Simulate detected store after reading
        setTimeout(() => {
          setPreviewData((prev) => ({
            ...prev,
            store: file.name.replace(/\.[^.]+$/, '').replace(/[_-]/g, ' '),
          }))
        }, 800)
      }, 1000)

      // Stage 3: Understanding
      setTimeout(() => {
        advanceStage('understanding')
        // Simulate detected category
        setTimeout(() => {
          setPreviewData((prev) => ({
            ...prev,
            category: 'OTHER',
            items: ['Purchase'],
          }))
        }, 600)
      }, 2500)

      // Stage 4: Extracting
      setTimeout(() => {
        advanceStage('extracting')
        // Simulate detected date and amount
        setTimeout(() => {
          setPreviewData((prev) => ({
            ...prev,
            date: new Date().toISOString(),
            amount: 0,
          }))
        }, 500)
      }, 4000)

      // Stage 5: Calculating
      setTimeout(() => {
        advanceStage('calculating')
      }, 5500)

      // Stage 6: Confidence check
      setTimeout(() => {
        advanceStage('confidence')
      }, 7000)

      // Stage 7: Saving
      setTimeout(() => {
        advanceStage('saving')
      }, 8500)

      const uploadResult = await uploadPromise

      if (!uploadResult.success) {
        throw new Error(uploadResult.message || 'Upload failed')
      }

      // Fetch analytics to get the latest activity
      await new Promise((resolve) => setTimeout(resolve, 2000))
      const analytics = await getCarbonAnalytics()
      const activities = analytics.data?.topActivities ?? []

      if (activities.length > 0) {
        const latestActivity = activities[0]
        setResult(latestActivity)
        setPreviewData((prev) => ({
          ...prev,
          store: latestActivity.merchant || prev.store,
          category: latestActivity.category,
          carbon: Number(latestActivity.carbonKg ?? 0),
        }))
      } else {
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
      setCurrentStage('success')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong')
      setPhase('error')
      setCurrentStage('error')
    }
  }, [advanceStage])

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
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }, [])

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
    setCurrentStage('idle')
    setCompletedStages([])
    setPreviewData({
      store: null,
      items: [],
      category: null,
      date: null,
      amount: null,
      carbon: null,
    })
  }, [])

  const handleNavigate = useCallback(() => {
    navigate('/')
  }, [navigate])

  const isProcessing = phase === 'processing'
  const detectedFields = buildDetectedFields(previewData)

  return (
    <div className="max-w-2xl mx-auto">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
        className="text-center mb-8"
      >
        {/* Animated illustration */}
        <div className="relative w-40 h-28 mx-auto mb-5">
          {/* Receipt */}
          <motion.div
            className="absolute left-2 top-0 w-14 h-22 bg-card rounded-xl border border-border-light shadow-card flex flex-col items-center justify-center gap-1.5 overflow-hidden"
            animate={
              isProcessing
                ? { x: [0, 20, 60], opacity: [1, 1, 0], rotate: [0, 0, 5] }
                : {}
            }
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
              className="absolute left-4 w-10 h-0.5 bg-gradient-to-r from-transparent via-emerald-400 to-transparent rounded-full"
              initial={{ top: 0, opacity: 0 }}
              animate={{ top: [0, 80, 0], opacity: [0, 0.8, 0] }}
              transition={{ duration: 1.5, repeat: Infinity, ease: 'easeInOut' }}
            />
          )}

          {/* Arrow */}
          <motion.div
            className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2"
            animate={
              isProcessing ? { scale: [1, 1.2, 1], rotate: [0, 10, 0] } : {}
            }
            transition={{ duration: 1, repeat: Infinity }}
          >
            <Zap className="w-5 h-5 text-emerald-400" />
          </motion.div>

          {/* Leaf (result) */}
          <motion.div
            className="absolute right-2 top-1 w-14 h-18 bg-emerald-50 rounded-2xl border border-emerald-100 flex items-center justify-center"
            initial={{ opacity: 0.4, scale: 0.9 }}
            animate={
              phase === 'success'
                ? {
                    opacity: 1,
                    scale: 1,
                    boxShadow: '0 4px 20px rgba(16,185,129,0.15)',
                  }
                : isProcessing
                ? { opacity: [0.4, 0.8, 0.4], scale: [0.95, 1, 0.95] }
                : {}
            }
            transition={{ duration: 1.5, repeat: isProcessing ? Infinity : 0 }}
          >
            <span className="text-2xl">🌿</span>
          </motion.div>
        </div>

        <h1 className="text-2xl sm:text-3xl font-bold text-ink tracking-tight mb-2">
          {phase === 'success' ? 'All done!' : 'Turn receipts into impact'}
        </h1>
        <p className="text-ink-muted text-sm max-w-md mx-auto">
          {phase === 'success'
            ? 'Your receipt has been processed and carbon impact calculated.'
            : 'Upload a receipt and watch our AI extract data, calculate emissions, and build your carbon profile in real-time.'}
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
              className={`relative rounded-2xl border-2 border-dashed p-12 text-center cursor-pointer
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
                className="w-14 h-14 rounded-2xl bg-emerald-50 flex items-center justify-center mx-auto mb-4
                           group-hover:bg-emerald-100 transition-colors duration-300"
                animate={dragActive ? { scale: [1, 1.1, 1], y: [0, -4, 0] } : {}}
                transition={{ duration: 0.6, repeat: dragActive ? Infinity : 0 }}
              >
                <Upload
                  className={`w-6 h-6 transition-colors duration-300 ${
                    dragActive
                      ? 'text-emerald-600'
                      : 'text-emerald-500 group-hover:text-emerald-600'
                  }`}
                />
              </motion.div>

              <h3 className="text-lg font-semibold text-ink mb-2">
                {dragActive ? 'Release to upload' : 'Drop your receipt here'}
              </h3>
              <p className="text-sm text-ink-muted mb-4">
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
                  className="absolute inset-0 rounded-2xl bg-gradient-to-r from-emerald-100/0 via-emerald-200/30 to-emerald-100/0"
                  animate={{ x: ['-100%', '100%'] }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
                />
              )}
            </div>

            {/* How it works */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3 }}
              className="mt-6 text-center"
            >
              <p className="text-[11px] text-ink-faint mb-3">How it works</p>
              <div className="flex items-center justify-center gap-4 text-xs text-ink-muted">
                <div className="flex items-center gap-1.5">
                  <div className="w-5 h-5 rounded-full bg-emerald-50 flex items-center justify-center">
                    <span className="text-[10px]">1</span>
                  </div>
                  <span>Upload</span>
                </div>
                <ArrowRight className="w-3 h-3 text-ink-faint" />
                <div className="flex items-center gap-1.5">
                  <div className="w-5 h-5 rounded-full bg-emerald-50 flex items-center justify-center">
                    <span className="text-[10px]">2</span>
                  </div>
                  <span>AI scans</span>
                </div>
                <ArrowRight className="w-3 h-3 text-ink-faint" />
                <div className="flex items-center gap-1.5">
                  <div className="w-5 h-5 rounded-full bg-emerald-50 flex items-center justify-center">
                    <span className="text-[10px]">3</span>
                  </div>
                  <span>Get insights</span>
                </div>
              </div>
            </motion.div>
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
            <div className="text-center mb-4">
              <span className="text-xs text-ink-faint font-medium">{fileName}</span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Timeline */}
              <div>
                <ProcessingTimeline
                  currentStage={currentStage}
                  completedStages={completedStages}
                />
              </div>

              {/* Live preview */}
              <div>
                <LivePreview fields={detectedFields} visible={true} />

                {/* Processing message */}
                <motion.div
                  className="mt-4 text-center"
                  animate={{ opacity: [0.5, 1, 0.5] }}
                  transition={{ duration: 2, repeat: Infinity }}
                >
                  <p className="text-xs text-ink-muted">
                    {processingMessages[currentStage] || 'Processing...'}
                  </p>
                </motion.div>
              </div>
            </div>
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
            <SuccessCelebration activity={result} onAutoRedirect={handleNavigate} />
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
