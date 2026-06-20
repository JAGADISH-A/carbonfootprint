import React from 'react'
import { Device } from '@/types/device'

interface DeviceCardProps {
  device: Device
  onRemove: (deviceId: string) => void
  isRemoving: boolean
}

export default function DeviceCard({ device, onRemove, isRemoving }: DeviceCardProps) {
  // Format dates
  const pairedDate = device.createdAt ? new Date(device.createdAt).toLocaleDateString() : 'Unknown'
  const lastSeen = device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : 'Unknown'

  // Placeholder for future status
  const connectionStatus = 'Connected' // We can determine this from lastSeenAt later

  return (
    <div className="bg-white border border-gray-200 rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow">
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">{device.deviceName || 'Unknown Device'}</h3>
          <p className="text-sm text-gray-500">{device.manufacturer} {device.model}</p>
        </div>
        <div className="flex items-center space-x-2">
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
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
      </div>

      {/* Placeholder for future expansion */}
      <div className="mb-6 pt-4 border-t border-gray-100 hidden">
        <p className="text-xs text-gray-500 uppercase tracking-wider mb-2">Permissions & Settings</p>
        {/* Future toggles/status for Battery Optimization, SMS, Notifications, Background Sync */}
      </div>

      <div className="flex justify-end pt-4 border-t border-gray-100">
        <button
          onClick={() => onRemove(device.deviceId)}
          disabled={isRemoving}
          className="text-sm font-medium text-red-600 hover:text-red-800 disabled:opacity-50 transition-colors px-3 py-1.5 rounded-md hover:bg-red-50"
        >
          {isRemoving ? 'Removing...' : 'Remove Device'}
        </button>
      </div>
    </div>
  )
}
