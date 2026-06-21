import { useState } from 'react'
import { Device } from '@/types/device'
import { syncDevice } from '@/api/services'
import { useDashboard } from '@/api/DashboardContext'
import { Smartphone, RefreshCw, CheckCircle, AlertTriangle } from 'lucide-react'
import { motion } from 'framer-motion'

interface DeviceCardProps {
  device: Device
  onRemove?: (deviceId: string) => void
  isRemoving?: boolean
  compact?: boolean
}

type SyncState = 'idle' | 'requested' | 'syncing' | 'completed' | 'failed'

export default function DeviceCard({ device, onRemove, isRemoving = false, compact = false }: DeviceCardProps) {
  const { refreshAll } = useDashboard()
  const [syncState, setSyncState] = useState<SyncState>('idle')
  const [errorText, setErrorText] = useState('')

  // Format dates
  const pairedDate = device.createdAt ? new Date(device.createdAt).toLocaleDateString() : 'Unknown'
  const lastSeen = device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : 'Unknown'
  const lastSync = device.lastSyncAt ? new Date(device.lastSyncAt).toLocaleString() : 'Never'
  const pendingCount = device.pendingUploadCount ?? 0
  const uploadStatus = device.lastUploadStatus || 'None'

  // Calculate status
  const connectionStatus = (() => {
    if (!device.lastSeenAt) return 'Offline'
    const diffMs = Date.now() - new Date(device.lastSeenAt).getTime()
    return diffMs < 5 * 60 * 1000 ? 'Connected' : 'Offline'
  })()
  const statusColor = connectionStatus === 'Connected' ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'

  const handleSync = async () => {
    setSyncState('requested')
    setErrorText('')
    
    // Drive state from actual sync lifecycle
    setSyncState('syncing')
    try {
      const response = await syncDevice(device.deviceId)
      if (response.success) {
        setSyncState('completed')
        await refreshAll()
        setTimeout(() => {
          setSyncState('idle')
        }, 1500)
      } else {
        setSyncState('failed')
        setErrorText(response.message || 'Sync failed')
      }
    } catch (err: any) {
      setSyncState('failed')
      setErrorText(err.userMessage || 'An error occurred during sync')
    }
  }

  const renderSyncStatus = () => {
    if (syncState === 'requested') {
      return (
        <div className="flex items-center gap-2 text-xs text-blue-600 font-medium py-1 animate-pulse">
          <RefreshCw className="w-3.5 h-3.5 animate-spin" />
          <span>Sync requested...</span>
        </div>
      )
    }
    if (syncState === 'syncing') {
      return (
        <div className="w-full py-1">
          <div className="flex items-center justify-between text-xs text-amber-600 font-medium mb-1">
            <div className="flex items-center gap-1.5">
              <RefreshCw className="w-3.5 h-3.5 animate-spin" />
              <span>Device syncing...</span>
            </div>
            <span>Processing queue</span>
          </div>
          <div className="w-full bg-gray-100 rounded-full h-1 overflow-hidden">
            <motion.div
              className="bg-amber-500 h-1 rounded-full"
              initial={{ width: '0%' }}
              animate={{ width: '100%' }}
              transition={{ duration: 1.5, ease: 'easeInOut' }}
            />
          </div>
        </div>
      )
    }
    if (syncState === 'completed') {
      return (
        <div className="flex items-center gap-2 text-xs text-emerald-600 font-semibold py-1">
          <CheckCircle className="w-3.5 h-3.5" />
          <span>Upload complete! Dashboard updated.</span>
        </div>
      )
    }
    if (syncState === 'failed' || uploadStatus === 'FAILED') {
      return (
        <div className="flex items-center justify-between w-full py-1">
          <div className="flex items-center gap-2 text-xs text-red-600 font-medium">
            <AlertTriangle className="w-3.5 h-3.5" />
            <span>Last upload failed {errorText ? `(${errorText})` : ''}</span>
          </div>
          <button
            onClick={handleSync}
            aria-label="Retry device synchronization"
            className="text-xs font-semibold text-blue-600 hover:text-blue-800 underline transition-colors px-2 py-0.5 rounded hover:bg-blue-50"
          >
            Retry
          </button>
        </div>
      )
    }
    return null
  }

  // COMPACT COMPONENT LAYOUT FOR DASHBOARD
  if (compact) {
    return (
      <div className="bg-white border border-gray-100 rounded-2xl p-4 shadow-sm hover:shadow-md transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-blue-50 flex items-center justify-center flex-shrink-0">
            <Smartphone className="w-5 h-5 text-blue-600" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-semibold text-gray-900">{device.deviceName || 'Companion Device'}</h3>
              <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium ${statusColor}`}>
                {connectionStatus}
              </span>
            </div>
            <p className="text-xs text-gray-500 mt-0.5">
              {device.manufacturer} {device.model}
            </p>
          </div>
        </div>

        <div className="flex-1 max-w-sm">
          {syncState !== 'idle' || uploadStatus === 'FAILED' ? (
            renderSyncStatus()
          ) : (
            <div className="grid grid-cols-3 gap-4 text-[11px] text-gray-600">
              <div>
                <span className="text-gray-400 block uppercase tracking-wider text-[9px]">Last Sync</span>
                <span className="font-semibold">{lastSync.split(',')[0]}</span>
              </div>
              <div>
                <span className="text-gray-400 block uppercase tracking-wider text-[9px]">Pending</span>
                <span className="font-semibold">{pendingCount} activities</span>
              </div>
              <div>
                <span className="text-gray-400 block uppercase tracking-wider text-[9px]">Upload</span>
                <span className={`font-semibold ${uploadStatus === 'SUCCESS' ? 'text-emerald-600' : 'text-gray-700'}`}>
                  {uploadStatus}
                </span>
              </div>
            </div>
          )}
        </div>

        {syncState === 'idle' && uploadStatus !== 'FAILED' && (
          <button
            onClick={handleSync}
            aria-label="Sync companion device now"
            className="px-3.5 py-1.5 bg-blue-50 hover:bg-blue-100 text-blue-700 font-semibold text-xs rounded-lg transition-all flex items-center gap-1.5 self-end sm:self-auto shadow-sm"
          >
            <RefreshCw className="w-3 h-3 animate-none hover:rotate-180 transition-transform duration-500" />
            Sync Now
          </button>
        )}
      </div>
    )
  }

  // DEFAULT SETTINGS LAYOUT
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">{device.deviceName || 'Unknown Device'}</h3>
          <p className="text-sm text-gray-500">{device.manufacturer} {device.model}</p>
        </div>
        <div className="flex items-center space-x-2">
          <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${statusColor}`}>
            {connectionStatus}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 mb-6">
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wider">Android Version</p>
          <p className="text-sm font-medium text-gray-900">{device.androidVersion}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wider">App Version</p>
          <p className="text-sm font-medium text-gray-900">{device.appVersion}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wider">Last Seen</p>
          <p className="text-sm font-medium text-gray-900">{lastSeen}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wider">Paired On</p>
          <p className="text-sm font-medium text-gray-900">{pairedDate}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wider">Last Sync</p>
          <p className="text-sm font-medium text-gray-900">{lastSync}</p>
        </div>
        <div>
          <p className="text-xs text-gray-500 uppercase tracking-wider">Upload Status</p>
          <p className={`text-sm font-medium ${uploadStatus === 'FAILED' ? 'text-red-600' : uploadStatus === 'SUCCESS' ? 'text-green-600' : 'text-gray-900'}`}>{uploadStatus}</p>
        </div>
        <div className="col-span-2">
          <p className="text-xs text-gray-500 uppercase tracking-wider">Pending Uploads</p>
          <p className="text-sm font-medium text-gray-900">
            {pendingCount} {pendingCount === 1 ? 'activity' : 'activities'} queued
          </p>
        </div>
      </div>

      {/* Sync Status / Feedback area */}
      {(syncState !== 'idle' || uploadStatus === 'FAILED') && (
        <div className="mb-6 p-3 bg-gray-50 rounded-xl border border-gray-100">
          {renderSyncStatus()}
        </div>
      )}

      <div className="flex justify-between items-center pt-4 border-t border-gray-100">
        <div>
          {syncState === 'idle' && uploadStatus !== 'FAILED' && (
            <button
              onClick={handleSync}
              aria-label="Sync companion device now"
              className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
            >
              <RefreshCw className="mr-2 -ml-1 h-4 w-4" />
              Sync Now
            </button>
          )}
        </div>
        {onRemove && (
          <button
            onClick={() => onRemove(device.deviceId)}
            disabled={isRemoving}
            className="text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-50 transition-colors px-3 py-1.5 rounded-md hover:bg-red-50"
          >
            {isRemoving ? 'Removing...' : 'Remove Device'}
          </button>
        )}
      </div>
    </div>
  )
}
