import { useState } from 'react'
import { CompanionRelease } from '@/types/device'

interface CompanionDownloadCardProps {
  release: CompanionRelease | null
  isLoading: boolean
}

const STEPS = [
  'Download APK',
  'Install APK',
  'Open Companion App',
  'Generate Pairing Code',
  'Grant Permissions',
  'Start Sync',
]

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '—'
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '—'
  try {
    return new Date(dateStr).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
  } catch {
    return dateStr
  }
}

export default function CompanionDownloadCard({ release, isLoading }: CompanionDownloadCardProps) {
  const [showGuide, setShowGuide] = useState(false)

  if (isLoading) {
    return (
      <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm animate-pulse">
        <div className="h-5 bg-gray-200 rounded w-40 mb-3" />
        <div className="h-4 bg-gray-200 rounded w-56 mb-4" />
        <div className="h-10 bg-gray-200 rounded w-36" />
      </div>
    )
  }

  if (!release || !release.available) {
    return (
      <div className="bg-white border border-gray-100 rounded-xl p-6 shadow-sm">
        <p className="text-sm text-gray-500">
          {release === null ? 'Loading companion info...' : 'APK not yet available.'}
        </p>
      </div>
    )
  }

  return (
    <div id="companion-download" className="bg-white border border-gray-100 rounded-xl shadow-sm overflow-hidden">
      <div className="p-6 pb-0">
        <div className="flex items-center justify-between mb-1">
          <h3 className="text-sm font-semibold text-gray-900">Android Companion</h3>
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
            v{release.version}
          </span>
        </div>
        <p className="text-xs text-gray-500 mb-4">
          Track carbon emissions from your transactions automatically.
        </p>

        <div className="flex gap-4 text-xs text-gray-500 mb-4">
          <span>{formatFileSize(release.fileSizeBytes)}</span>
          <span className="text-gray-300">|</span>
          <span>{formatDate(release.releaseDate)}</span>
        </div>

        <a
          href={release.downloadUrl}
          download
          className="w-full inline-flex items-center justify-center px-4 py-2.5 text-sm font-medium rounded-lg text-white bg-emerald-600 hover:bg-emerald-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-emerald-500 transition-colors"
        >
          <svg className="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Download APK
        </a>
      </div>

      <div className="px-6 pt-4 pb-2">
        <button
          onClick={() => setShowGuide(!showGuide)}
          className="w-full flex items-center justify-between text-sm font-medium text-gray-700 hover:text-gray-900 transition-colors py-2"
        >
          <span>Installation Guide</span>
          <svg
            className={`h-4 w-4 text-gray-400 transition-transform ${showGuide ? 'rotate-180' : ''}`}
            fill="none" viewBox="0 0 24 24" stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </button>
      </div>

      {showGuide && (
        <div className="px-6 pb-6">
          <div>
            {STEPS.map((step, i) => (
              <div key={i} className="flex gap-3">
                <div className="flex flex-col items-center">
                  <div className="flex-shrink-0 w-6 h-6 rounded-full border-2 border-gray-200 bg-white flex items-center justify-center">
                    <span className="text-[10px] font-semibold text-gray-400">{i + 1}</span>
                  </div>
                  {i < STEPS.length - 1 && (
                    <div className="w-px flex-1 bg-gray-100 min-h-[20px]" />
                  )}
                </div>
                <div className="pb-4">
                  <span className="text-sm text-gray-700">{step}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {release.releaseNotes && release.releaseNotes.length > 0 && (
        <div className="px-6 pb-6">
          <div className="pt-4 border-t border-gray-100">
            <h4 className="text-xs font-semibold text-gray-900 uppercase tracking-wider mb-2">What's New</h4>
            <ul className="space-y-1.5">
              {release.releaseNotes.map((note, i) => (
                <li key={i} className="flex items-start gap-2 text-xs text-gray-600">
                  <svg className="h-3.5 w-3.5 text-emerald-500 mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  {note}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  )
}
