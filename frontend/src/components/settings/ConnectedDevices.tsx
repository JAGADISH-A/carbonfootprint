import React, { useEffect, useState } from 'react'
import { Device } from '@/types/device'
import { getConnectedDevices, removeDevice } from '@/api/services'
import DeviceCard from './DeviceCard'
import ConnectDeviceModal from './ConnectDeviceModal'

export default function ConnectedDevices() {
  const [devices, setDevices] = useState<Device[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [removingId, setRemovingId] = useState<string | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)

  const fetchDevices = async () => {
    setIsLoading(true)
    setError(null)
    try {
      const response = await getConnectedDevices()
      if (response.success && response.data) {
        setDevices(response.data)
      } else {
        setError(response.message || 'Failed to load connected devices.')
      }
    } catch (err) {
      setError('An error occurred while fetching devices.')
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchDevices()
  }, [])

  const handleRemoveDevice = async (deviceId: string) => {
    if (!window.confirm('Are you sure you want to remove this device? It will be disconnected immediately.')) {
      return
    }

    setRemovingId(deviceId)
    try {
      const response = await removeDevice(deviceId)
      if (response.success) {
        setDevices((prev) => prev.filter((d) => d.deviceId !== deviceId))
        // Optional: show a toast notification here
      } else {
        alert(response.message || 'Failed to remove device.')
      }
    } catch (err) {
      alert('An error occurred while removing the device.')
    } finally {
      setRemovingId(null)
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <div>
          <h2 className="text-xl font-semibold text-gray-900">Connected Devices</h2>
          <p className="text-sm text-gray-500 mt-1">Manage your paired Android companion devices.</p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="inline-flex items-center px-4 py-2 border border-transparent text-sm font-medium rounded-md shadow-sm text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
        >
          <svg className="mr-2 -ml-1 h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Connect New Device
        </button>
      </div>

      {error && (
        <div className="p-4 bg-red-50 text-red-700 rounded-lg border border-red-100">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="space-y-4">
          {[1, 2].map((i) => (
            <div key={i} className="animate-pulse bg-white border border-gray-100 rounded-xl p-5 shadow-sm">
              <div className="flex justify-between items-start mb-4">
                <div className="space-y-2">
                  <div className="h-5 bg-gray-200 rounded w-32"></div>
                  <div className="h-4 bg-gray-200 rounded w-24"></div>
                </div>
                <div className="h-5 bg-gray-200 rounded-full w-20"></div>
              </div>
              <div className="grid grid-cols-2 gap-4 mb-6">
                {[1, 2, 3, 4].map((j) => (
                  <div key={j} className="space-y-2">
                    <div className="h-3 bg-gray-200 rounded w-20"></div>
                    <div className="h-4 bg-gray-200 rounded w-28"></div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      ) : devices.length > 0 ? (
        <div className="grid grid-cols-1 gap-6">
          {devices.map((device) => (
            <DeviceCard
              key={device.deviceId}
              device={device}
              onRemove={handleRemoveDevice}
              isRemoving={removingId === device.deviceId}
            />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 bg-white border border-dashed border-gray-300 rounded-xl">
          <svg className="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
          <h3 className="mt-2 text-sm font-medium text-gray-900">No devices connected</h3>
          <p className="mt-1 text-sm text-gray-500">Get started by pairing your Android device.</p>
          <div className="mt-6">
            <button
              onClick={() => setIsModalOpen(true)}
              className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-blue-700 bg-blue-100 hover:bg-blue-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
            >
              <svg className="-ml-1 mr-2 h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
              </svg>
              Connect Device
            </button>
          </div>
        </div>
      )}

      <ConnectDeviceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => {
          // Refresh devices list when successfully paired
          fetchDevices()
        }}
      />
    </div>
  )
}
