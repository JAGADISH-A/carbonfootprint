import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { getCarbonAnalytics, getCarbonInsights, getAICoach, getConnectedDevices } from './services'
import type { CarbonAnalyticsResponse, CarbonInsightResponse, AICarbonCoachResponse } from '@/types/activity'
import type { Device } from '@/types/device'

interface DashboardContextType {
  analytics: CarbonAnalyticsResponse | null
  insights: CarbonInsightResponse | null
  coach: AICarbonCoachResponse | null
  devices: Device[] | null
  loading: boolean
  error: boolean
  lastUpdated: number
  refreshAll: () => Promise<void>
  refreshAnalytics: () => Promise<void>
  refreshInsights: () => Promise<void>
  refreshCoach: () => Promise<void>
  refreshDevices: () => Promise<void>
  triggerUpdate: () => void
}

const DashboardContext = createContext<DashboardContextType | undefined>(undefined)

export function DashboardProvider({ children }: { children: React.ReactNode }) {
  const [analytics, setAnalytics] = useState<CarbonAnalyticsResponse | null>(null)
  const [insights, setInsights] = useState<CarbonInsightResponse | null>(null)
  const [coach, setCoach] = useState<AICarbonCoachResponse | null>(null)
  const [devices, setDevices] = useState<Device[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [lastUpdated, setLastUpdated] = useState<number>(Date.now())

  const triggerUpdate = useCallback(() => {
    setLastUpdated(Date.now())
  }, [])

  const refreshAnalytics = useCallback(async () => {
    try {
      const res = await getCarbonAnalytics()
      if (res.success && res.data) {
        setAnalytics(res.data)
      }
    } catch (err) {
      console.error('Failed to fetch analytics', err)
      setError(true)
    }
  }, [])

  const refreshInsights = useCallback(async () => {
    try {
      const res = await getCarbonInsights()
      if (res.success && res.data) {
        setInsights(res.data)
      }
    } catch (err) {
      console.error('Failed to fetch insights', err)
      setError(true)
    }
  }, [])

  const refreshCoach = useCallback(async () => {
    try {
      const res = await getAICoach()
      if (res.success && res.data) {
        setCoach(res.data)
      }
    } catch (err) {
      console.error('Failed to fetch coach', err)
      setError(true)
    }
  }, [])

  const refreshDevices = useCallback(async () => {
    try {
      const res = await getConnectedDevices()
      if (res.success && res.data) {
        setDevices(res.data)
      }
    } catch (err) {
      console.error('Failed to fetch devices', err)
      setError(true)
    }
  }, [])

  const refreshAll = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      await Promise.all([
        refreshAnalytics(),
        refreshInsights(),
        refreshCoach(),
        refreshDevices(),
      ])
      setLastUpdated(Date.now())
    } catch (err) {
      setError(true)
    } finally {
      setLoading(false)
    }
  }, [refreshAnalytics, refreshInsights, refreshCoach, refreshDevices])

  // Initial load
  useEffect(() => {
    refreshAll()
  }, [refreshAll])

  // Refocus strategy: debounced refresh when tab regains focus
  useEffect(() => {
    let timeoutId: ReturnType<typeof setTimeout>
    const handleFocus = () => {
      clearTimeout(timeoutId)
      timeoutId = setTimeout(() => {
        refreshAll()
      }, 500)
    }
    window.addEventListener('focus', handleFocus)
    return () => {
      clearTimeout(timeoutId)
      window.removeEventListener('focus', handleFocus)
    }
  }, [refreshAll])

  return (
    <DashboardContext.Provider
      value={{
        analytics,
        insights,
        coach,
        devices,
        loading,
        error,
        lastUpdated,
        refreshAll,
        refreshAnalytics,
        refreshInsights,
        refreshCoach,
        refreshDevices,
        triggerUpdate,
      }}
    >
      {children}
    </DashboardContext.Provider>
  )
}

export function useDashboard() {
  const context = useContext(DashboardContext)
  if (!context) {
    throw new Error('useDashboard must be used within a DashboardProvider')
  }
  return context
}
