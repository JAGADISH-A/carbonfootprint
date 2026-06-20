import React, { useState, useEffect } from 'react'
import { generatePairingCode } from '@/api/services'
import { PairingCodeResponse } from '@/types/device'

interface ConnectDeviceModalProps {
  isOpen: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function ConnectDeviceModal({ isOpen, onClose, onSuccess }: ConnectDeviceModalProps) {
  const [step, setStep] = useState(1)
  const [pairingData, setPairingData] = useState<PairingCodeResponse | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [timeLeft, setTimeLeft] = useState<number>(0)
  const [isExpired, setIsExpired] = useState(false)

  // Countdown timer logic
  useEffect(() => {
    if (step === 2 && pairingData && timeLeft > 0) {
      const timer = setInterval(() => {
        setTimeLeft((prev) => {
          if (prev <= 1) {
            clearInterval(timer)
            setIsExpired(true)
            return 0
          }
          return prev - 1
        })
      }, 1000)
      return () => clearInterval(timer)
    }
  }, [step, pairingData, timeLeft])

  const handleGenerateCode = async () => {
    setIsLoading(true)
    setError(null)
    setIsExpired(false)
    try {
      const data = await generatePairingCode()
      if (data.success && data.data) {
        setPairingData(data.data)
        setTimeLeft(data.data.expiresInSeconds)
        setStep(2)
      } else {
        setError(data.message || 'Failed to generate code')
      }
    } catch (err) {
      setError('An error occurred. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${s.toString().padStart(2, '0')}`
  }

  if (!isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/50 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-xl w-full max-w-md overflow-hidden animate-in fade-in zoom-in duration-200">
        <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
          <h2 className="text-xl font-semibold text-gray-900">Connect New Device</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="p-6">
          {error && (
            <div className="mb-4 p-3 bg-red-50 text-red-700 text-sm rounded-lg border border-red-100">
              {error}
            </div>
          )}

          {step === 1 && (
            <div className="space-y-6">
              <div className="space-y-4 text-gray-600">
                <p>Follow these steps to connect your Android device:</p>
                <ol className="list-decimal pl-5 space-y-2 text-sm">
                  <li>Download and install the CarbonFootprint companion app.</li>
                  <li>Open the app on your Android device.</li>
                  <li>Tap on "Pair Device" on the welcome screen.</li>
                  <li>Click below to generate a pairing code.</li>
                </ol>
              </div>
              <button
                onClick={handleGenerateCode}
                disabled={isLoading}
                className="w-full flex justify-center py-2.5 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 transition-colors"
              >
                {isLoading ? 'Generating...' : 'Generate Pairing Code'}
              </button>
            </div>
          )}

          {step === 2 && pairingData && (
            <div className="space-y-6 text-center">
              <p className="text-sm text-gray-500">Enter this code in your Android app</p>
              
              <div className={`p-6 rounded-xl border-2 transition-colors ${isExpired ? 'bg-gray-50 border-gray-200' : 'bg-blue-50 border-blue-100'}`}>
                <div className={`text-4xl font-mono font-bold tracking-widest ${isExpired ? 'text-gray-400 line-through' : 'text-blue-700'}`}>
                  {pairingData.pairingCode}
                </div>
              </div>

              {!isExpired ? (
                <div className="flex items-center justify-center space-x-2 text-sm text-gray-500">
                  <svg className="w-4 h-4 text-orange-500 animate-pulse" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>Code expires in <span className="font-semibold text-gray-900">{formatTime(timeLeft)}</span></span>
                </div>
              ) : (
                <div className="space-y-4 animate-in slide-in-from-bottom-2">
                  <div className="flex items-center justify-center space-x-2 text-sm text-red-600">
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                    </svg>
                    <span className="font-medium">Pairing code expired</span>
                  </div>
                  <button
                    onClick={handleGenerateCode}
                    disabled={isLoading}
                    className="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-lg shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
                  >
                    {isLoading ? 'Generating...' : 'Generate New Code'}
                  </button>
                </div>
              )}

              <div className="pt-4 border-t border-gray-100">
                <button
                  onClick={() => {
                    onSuccess()
                    onClose()
                  }}
                  className="text-sm text-blue-600 hover:text-blue-800 font-medium transition-colors"
                >
                  I've paired my device
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
