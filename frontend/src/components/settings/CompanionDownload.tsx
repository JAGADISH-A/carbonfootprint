import { useEffect, useState } from 'react'
import { CompanionRelease } from '@/types/device'
import { getCompanionRelease } from '@/api/services'

interface CompanionDownloadProps {
  connectedDeviceVersion?: string
}

function formatFileSize(bytes: number): string {
  if (bytes === 0) return 'Unknown'
  const mb = bytes / (1024 * 1024)
  return `${mb.toFixed(1)} MB`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return 'Unknown'
  try {
    return new Date(dateStr).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    })
  } catch {
    return dateStr
  }
}

function isNewerVersion(latest: string, current: string): boolean {
  if (!latest || !current) return false
  const parse = (v: string) => v.split('.').map(Number)
  const [lMaj, lMin, lPat] = parse(latest)
  const [cMaj, cMin, cPat] = parse(current)
  if (lMaj !== cMaj) return lMaj > cMaj
  if (lMin !== cMin) return lMin > cMin
  return lPat > cPat
}

export default function CompanionDownload({ connectedDeviceVersion }: CompanionDownloadProps) {
  const [release, setRelease] = useState<CompanionRelease | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showGuide, setShowGuide] = useState(false)

  useEffect(() => {
    const ctrl = new AbortController()
    getCompanionRelease(ctrl.signal)
      .then((res) => {
        if (res.success && res.data) {
          setRelease(res.data)
        } else {
          setError(res.message || 'Failed to load release info.')
        }
      })
      .catch(() => setError('Unable to fetch companion release info.'))
      .finally(() => setIsLoading(false))
    return () => ctrl.abort()
  }, [])

  const updateAvailable =
    release?.available &&
    connectedDeviceVersion &&
    isNewerVersion(release.version, connectedDeviceVersion)

  if (isLoading) {
    return (
      <div className="animate-pulse bg-white border border-gray-100 rounded-xl p-6 shadow-sm">
        <div className="h-5 bg-gray-200 rounded w-48 mb-3"></div>
        <div className="h-4 bg-gray-200 rounded w-64 mb-2"></div>
        <div className="h-10 bg-gray-200 rounded w-40 mt-4"></div>
      </div>
    )
  }

  if (error || !release) {
    return (
      <div className="bg-amber-50 border border-amber-200 rounded-xl p-6">
        <p className="text-sm text-amber-700">
          {error || 'Companion release information is currently unavailable.'}
        </p>
      </div>
    )
  }

  if (!release.available) {
    return (
      <div className="bg-gray-50 border border-dashed border-gray-300 rounded-xl p-6 text-center">
        <svg className="mx-auto h-10 w-10 text-gray-400 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
        </svg>
        <p className="text-sm text-gray-600">
          The Android Companion APK is not yet available. Check back soon.
        </p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      {/* Update Available Banner */}
      {updateAvailable && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 flex items-center gap-3">
          <svg className="h-5 w-5 text-blue-600 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          <div className="flex-1">
            <p className="text-sm font-medium text-blue-900">Update Available</p>
            <p className="text-xs text-blue-700">
              Version {release.version} is newer than your connected device ({connectedDeviceVersion}).
            </p>
          </div>
        </div>
      )}

      {/* Download Card */}
      <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm">
        <div className="flex items-start justify-between">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">Android Companion</h3>
            <p className="text-sm text-gray-500 mt-1">
              Pair your Android device to automatically track carbon emissions from transactions.
            </p>
          </div>
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            v{release.version}
          </span>
        </div>

        {/* Metadata */}
        <div className="mt-4 flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-500">
          <span className="flex items-center gap-1.5">
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            {formatFileSize(release.fileSizeBytes)}
          </span>
          <span className="flex items-center gap-1.5">
            <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" />
            </svg>
            {formatDate(release.releaseDate)}
          </span>
        </div>

        {/* Download Button */}
        <div className="mt-5 flex flex-wrap gap-3">
          <a
            href={release.downloadUrl}
            download
            className="inline-flex items-center px-5 py-2.5 border border-transparent text-sm font-medium rounded-lg shadow-sm text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors"
          >
            <svg className="mr-2 -ml-1 h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
            </svg>
            Download Android Companion
          </a>
          <button
            onClick={() => setShowGuide(!showGuide)}
            className="inline-flex items-center px-4 py-2.5 border border-gray-300 text-sm font-medium rounded-lg text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
          >
            {showGuide ? 'Hide Instructions' : 'Installation Guide'}
          </button>
        </div>

        {/* Installation Guide */}
        {showGuide && (
          <div className="mt-5 bg-gray-50 rounded-lg p-5 border border-gray-100">
            <h4 className="text-sm font-semibold text-gray-900 mb-3">Installation Steps</h4>
            <ol className="space-y-3 text-sm text-gray-700">
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold">1</span>
                <span>Tap <strong>Download Android Companion</strong> above and wait for the APK to finish downloading.</span>
              </li>
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold">2</span>
                <span>If prompted, enable <strong>Install unknown apps</strong> for your browser or file manager in Android Settings.</span>
              </li>
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold">3</span>
                <span>Open the downloaded APK file and tap <strong>Install</strong>.</span>
              </li>
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold">4</span>
                <span>Open <strong>CarbonWise Companion</strong> from your app drawer.</span>
              </li>
              <li className="flex gap-3">
                <span className="flex-shrink-0 w-6 h-6 flex items-center justify-center rounded-full bg-emerald-100 text-emerald-700 text-xs font-bold">5</span>
                <span>Tap <strong>Pair Device</strong> on this page and enter the pairing code shown in the app.</span>
              </li>
            </ol>
          </div>
        )}

        {/* Release Notes */}
        {release.releaseNotes && release.releaseNotes.length > 0 && (
          <div className="mt-5 pt-4 border-t border-gray-100">
            <h4 className="text-sm font-semibold text-gray-900 mb-2">What's New</h4>
            <ul className="space-y-1.5 text-sm text-gray-600">
              {release.releaseNotes.map((note, i) => (
                <li key={i} className="flex items-start gap-2">
                  <svg className="h-4 w-4 text-emerald-500 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  {note}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </div>
  )
}
