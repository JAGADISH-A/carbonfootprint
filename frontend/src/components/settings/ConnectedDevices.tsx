import { useEffect, useState } from 'react'
import { Device, CompanionRelease } from '@/types/device'
import { getConnectedDevices, removeDevice, getCompanionRelease } from '@/api/services'
import ConnectDeviceModal from './ConnectDeviceModal'
import EmptyDeviceState from './EmptyDeviceState'
import CompanionDownloadCard from './CompanionDownloadCard'
import DeviceStatusCard from './DeviceStatusCard'
import StatusInfoCards from './StatusInfoCards'

export default function ConnectedDevices() {
  const [devices, setDevices] = useState<Device[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [removingId, setRemovingId] = useState<string | null>(null)
  const [isModalOpen, setIsModalOpen] = useState(false)
  const [release, setRelease] = useState<CompanionRelease | null>(null)
  const [releaseLoading, setReleaseLoading] = useState(true)

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
    } catch {
      setError('An error occurred while fetching devices.')
    } finally {
      setIsLoading(false)
    }
  }

  const fetchRelease = async () => {
    setReleaseLoading(true)
    try {
      const res = await getCompanionRelease()
      if (res.success && res.data) {
        setRelease(res.data)
      }
    } catch {
      // silently fail — CompanionDownloadCard handles null
    } finally {
      setReleaseLoading(false)
    }
  }

  useEffect(() => {
    fetchDevices()
    fetchRelease()
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
      } else {
        alert(response.message || 'Failed to remove device.')
      }
    } catch {
      alert('An error occurred while removing the device.')
    } finally {
      setRemovingId(null)
    }
  }

  return (
    <div className="space-y-6">
      {error && (
        <div className="p-3 bg-red-50 text-red-700 rounded-lg border border-red-100 text-sm">
          {error}
        </div>
      )}

      {isLoading ? (
        <div className="grid grid-cols-1 lg:grid-cols-[35%_1fr] gap-6">
          <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm animate-pulse">
            <div className="h-5 bg-gray-200 rounded w-40 mb-3" />
            <div className="h-4 bg-gray-200 rounded w-56 mb-4" />
            <div className="h-10 bg-gray-200 rounded w-36" />
          </div>
          <div className="space-y-4">
            <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm animate-pulse">
              <div className="h-5 bg-gray-200 rounded w-48 mb-4" />
              <div className="grid grid-cols-3 gap-4">
                {[1, 2, 3].map((i) => (
                  <div key={i} className="h-4 bg-gray-200 rounded" />
                ))}
              </div>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="bg-white border border-gray-100 rounded-xl p-4 shadow-sm animate-pulse">
                  <div className="h-4 bg-gray-200 rounded w-20 mb-3" />
                  <div className="space-y-2">
                    <div className="h-3 bg-gray-200 rounded w-full" />
                    <div className="h-3 bg-gray-200 rounded w-3/4" />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : devices.length === 0 ? (
        <>
          <EmptyDeviceState onPair={() => setIsModalOpen(true)} />
          <CompanionDownloadCard release={release} isLoading={releaseLoading} />
        </>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-[35%_1fr] gap-6">
          <CompanionDownloadCard release={release} isLoading={releaseLoading} />
          <div className="space-y-4">
            {devices.map((device) => (
              <DeviceStatusCard
                key={device.deviceId}
                device={device}
                onRemove={handleRemoveDevice}
                isRemoving={removingId === device.deviceId}
                onPair={() => setIsModalOpen(true)}
              />
            ))}
            <StatusInfoCards device={devices[0]} />
          </div>
        </div>
      )}

      <ConnectDeviceModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={() => fetchDevices()}
      />
    </div>
  )
}
