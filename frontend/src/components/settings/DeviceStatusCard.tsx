import { useState } from 'react'
import { Device } from '@/types/device'
import { syncDevice } from '@/api/services'
import { useDashboard } from '@/api/DashboardContext'
import { RefreshCw, Smartphone, Wifi, WifiOff, Unplug } from 'lucide-react'

interface DeviceStatusCardProps {
  device: Device
  onRemove: (deviceId: string) => void
  isRemoving: boolean
  onPair: () => void
}

type SyncState = 'idle' | 'requested' | 'syncing' | 'completed' | 'failed'

export default function DeviceStatusCard({ device, onRemove, isRemoving, onPair }: DeviceStatusCardProps) {
  const { refreshAll } = useDashboard()
  const [syncState, setSyncState] = useState<SyncState>('idle')

  const connectionStatus = (() => {
    if (!device.lastSeenAt) return 'Offline'
    const diffMs = Date.now() - new Date(device.lastSeenAt).getTime()
    return diffMs < 5 * 60 * 1000 ? 'Online' : 'Offline'
  })()

  const lastSync = device.lastSyncAt
    ? new Date(device.lastSyncAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
    : 'Never'

  const handleSync = async () => {
    setSyncState('syncing')
    try {
      const response = await syncDevice(device.deviceId)
      if (response.success) {
        setSyncState('completed')
        await refreshAll()
        setTimeout(() => setSyncState('idle'), 1500)
      } else {
        setSyncState('failed')
      }
    } catch {
      setSyncState('failed')
    }
  }

  return (
    <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm">
      {/* Top: Device Info + Status */}
      <div className="flex items-start justify-between mb-5">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-lg bg-blue-50 flex items-center justify-center shrink-0">
            <Smartphone className="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <h3 className="text-sm font-semibold text-gray-900">
              {device.deviceName || 'Android Device'}
            </h3>
            <p className="text-xs text-gray-500">
              {device.manufacturer} {device.model}
            </p>
          </div>
        </div>
        <span
          className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${
            connectionStatus === 'Online'
              ? 'bg-green-100 text-green-800'
              : 'bg-gray-100 text-gray-600'
          }`}
        >
          {connectionStatus === 'Online' ? (
            <Wifi className="w-3 h-3" />
          ) : (
            <WifiOff className="w-3 h-3" />
          )}
          {connectionStatus}
        </span>
      </div>

      {/* Details Row */}
      <div className="grid grid-cols-3 gap-4 mb-5">
        <div>
          <p className="text-[10px] text-gray-400 uppercase tracking-wider font-medium">Version</p>
          <p className="text-xs font-semibold text-gray-900 mt-0.5">{device.appVersion}</p>
        </div>
        <div>
          <p className="text-[10px] text-gray-400 uppercase tracking-wider font-medium">Last Sync</p>
          <p className="text-xs font-semibold text-gray-900 mt-0.5">{lastSync}</p>
        </div>
        <div>
          <p className="text-[10px] text-gray-400 uppercase tracking-wider font-medium">Android</p>
          <p className="text-xs font-semibold text-gray-900 mt-0.5">{device.androidVersion}</p>
        </div>
      </div>

      {/* Sync Status */}
      {syncState !== 'idle' && (
        <div className="mb-4 p-3 rounded-lg bg-gray-50 border border-gray-100">
          {syncState === 'requested' && (
            <div className="flex items-center gap-2 text-xs text-blue-600 font-medium">
              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
              <span>Sync requested...</span>
            </div>
          )}
          {syncState === 'syncing' && (
            <div className="flex items-center gap-2 text-xs text-amber-600 font-medium">
              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
              <span>Syncing...</span>
            </div>
          )}
          {syncState === 'completed' && (
            <div className="flex items-center gap-2 text-xs text-emerald-600 font-semibold">
              <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              <span>Sync complete</span>
            </div>
          )}
          {syncState === 'failed' && (
            <div className="flex items-center justify-between">
              <span className="text-xs text-red-600 font-medium">Sync failed</span>
              <button onClick={handleSync} className="text-xs font-semibold text-blue-600 hover:text-blue-800">
                Retry
              </button>
            </div>
          )}
        </div>
      )}

      {/* Actions */}
      <div className="flex items-center gap-2 pt-4 border-t border-gray-100">
        <button
          onClick={onPair}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg text-gray-700 bg-gray-100 hover:bg-gray-200 transition-colors"
        >
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Pair
        </button>
        {syncState === 'idle' && (
          <button
            onClick={handleSync}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg text-white bg-blue-600 hover:bg-blue-700 transition-colors"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            Sync Now
          </button>
        )}
        <div className="flex-1" />
        <button
          onClick={() => onRemove(device.deviceId)}
          disabled={isRemoving}
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium rounded-lg text-red-600 hover:bg-red-50 disabled:opacity-50 transition-colors"
        >
          <Unplug className="w-3.5 h-3.5" />
          {isRemoving ? 'Removing...' : 'Remove'}
        </button>
      </div>
    </div>
  )
}
