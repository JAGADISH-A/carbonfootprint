import { Device } from '@/types/device'
import { Shield, RefreshCw, HeartPulse } from 'lucide-react'

interface StatusInfoCardsProps {
  device: Device
}

export default function StatusInfoCards({ device }: StatusInfoCardsProps) {
  const pendingCount = device.pendingUploadCount ?? 0
  const uploadStatus = device.lastUploadStatus || 'None'

  const connectionStatus = (() => {
    if (!device.lastSeenAt) return 'Offline'
    const diffMs = Date.now() - new Date(device.lastSeenAt).getTime()
    return diffMs < 5 * 60 * 1000 ? 'Online' : 'Offline'
  })()

  const healthStatus = (() => {
    if (connectionStatus === 'Online' && uploadStatus === 'SUCCESS') return 'Healthy'
    if (connectionStatus === 'Offline' || uploadStatus === 'FAILED') return 'Warning'
    return 'Healthy'
  })()

  const healthColor = healthStatus === 'Healthy'
    ? 'text-green-600 bg-green-50'
    : 'text-amber-600 bg-amber-50'

  return (
    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
      {/* Permissions */}
      <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <div className="w-7 h-7 rounded-md bg-blue-50 flex items-center justify-center">
            <Shield className="w-3.5 h-3.5 text-blue-600" />
          </div>
          <h4 className="text-xs font-semibold text-gray-900">Permissions</h4>
        </div>
        <div className="space-y-2">
          {['SMS', 'Notifications', 'Battery'].map((perm) => (
            <div key={perm} className="flex items-center justify-between">
              <span className="text-xs text-gray-500">{perm}</span>
              <span className="text-[10px] font-medium text-gray-400">—</span>
            </div>
          ))}
        </div>
      </div>

      {/* Sync */}
      <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <div className="w-7 h-7 rounded-md bg-emerald-50 flex items-center justify-center">
            <RefreshCw className="w-3.5 h-3.5 text-emerald-600" />
          </div>
          <h4 className="text-xs font-semibold text-gray-900">Sync</h4>
        </div>
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500">Pending</span>
            <span className="text-xs font-semibold text-gray-900">{pendingCount}</span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500">Last Upload</span>
            <span className={`text-xs font-semibold ${
              uploadStatus === 'SUCCESS' ? 'text-green-600' : uploadStatus === 'FAILED' ? 'text-red-600' : 'text-gray-900'
            }`}>
              {uploadStatus}
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-xs text-gray-500">Status</span>
            <span className="text-xs font-semibold text-gray-900">{connectionStatus}</span>
          </div>
        </div>
      </div>

      {/* Health */}
      <div className="bg-white border border-gray-100 rounded-xl p-4 shadow-sm">
        <div className="flex items-center gap-2 mb-3">
          <div className="w-7 h-7 rounded-md bg-purple-50 flex items-center justify-center">
            <HeartPulse className="w-3.5 h-3.5 text-purple-600" />
          </div>
          <h4 className="text-xs font-semibold text-gray-900">Health</h4>
        </div>
        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${healthColor}`}>
            {healthStatus}
          </span>
        </div>
        <p className="text-[10px] text-gray-400 mt-2">
          {healthStatus === 'Healthy'
            ? 'Device is connected and syncing properly.'
            : 'Check device connection and sync status.'}
        </p>
      </div>
    </div>
  )
}
