interface EmptyDeviceStateProps {
  onPair: () => void
}

export default function EmptyDeviceState({ onPair }: EmptyDeviceStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-6 text-center">
      {/* Phone illustration */}
      <div className="relative mb-6">
        <div className="w-24 h-40 rounded-[1.25rem] border-2 border-gray-200 bg-gray-50 flex items-center justify-center">
          <div className="w-14 h-28 rounded-lg border border-gray-200 bg-white flex flex-col items-center justify-center gap-1.5">
            <svg className="w-6 h-6 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M12 18h.01M8 21h8a2 2 0 002-2V5a2 2 0 00-2-2H8a2 2 0 00-2 2v14a2 2 0 002 2z" />
            </svg>
            <div className="w-6 h-1 rounded-full bg-gray-200" />
            <div className="w-4 h-1 rounded-full bg-gray-200" />
          </div>
        </div>
        {/* Connection line */}
        <div className="absolute -right-8 top-1/2 -translate-y-1/2 flex items-center gap-1">
          <div className="w-3 h-px bg-gray-300" />
          <svg className="w-4 h-4 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8.111 16.404a5.5 5.5 0 017.778 0M12 20h.01m-7.08-7.071c3.904-3.905 10.236-3.905 14.14 0" />
          </svg>
        </div>
      </div>

      <h3 className="text-base font-semibold text-gray-900 mb-1">
        No Android Companion Connected
      </h3>
      <p className="text-sm text-gray-500 max-w-sm mb-6">
        Install the CarbonWise Companion app on your Android device to automatically track carbon emissions from your transactions.
      </p>

      <div className="flex flex-col sm:flex-row gap-3">
        <button
          onClick={onPair}
          className="inline-flex items-center justify-center px-5 py-2.5 text-sm font-medium rounded-lg text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 transition-colors"
        >
          <svg className="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6v6m0 0v6m0-6h6m-6 0H6" />
          </svg>
          Pair Device
        </button>
        <a
          href="#companion-download"
          onClick={(e) => {
            e.preventDefault()
            document.getElementById('companion-download')?.scrollIntoView({ behavior: 'smooth' })
          }}
          className="inline-flex items-center justify-center px-5 py-2.5 text-sm font-medium rounded-lg text-gray-700 bg-gray-100 hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-gray-500 transition-colors"
        >
          <svg className="mr-2 h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
          </svg>
          Download Companion
        </a>
      </div>
    </div>
  )
}
