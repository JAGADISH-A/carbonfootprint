import { useState } from 'react'
import ConnectedDevices from '@/components/settings/ConnectedDevices'

export default function Settings() {
  const [activeTab, setActiveTab] = useState('mobile')

  const tabs = [
    { id: 'profile', label: 'Profile' },
    { id: 'preferences', label: 'Preferences' },
    { id: 'notifications', label: 'Notifications' },
    { id: 'mobile', label: 'Mobile Companion' },
  ]

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-in fade-in duration-300">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Settings</h1>
        <p className="mt-2 text-sm text-gray-500">
          Manage your account settings and connected devices.
        </p>
      </div>

      <div className="flex flex-col md:flex-row gap-8">
        {/* Sidebar Navigation */}
        <aside className="md:w-64 shrink-0">
          <nav className="space-y-1">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`w-full text-left px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  activeTab === tab.id
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </aside>

        {/* Content Area */}
        <main className="flex-1 bg-white rounded-2xl shadow-sm border border-gray-100 p-6 md:p-8">
          {activeTab === 'profile' && (
            <div>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Profile</h2>
              <p className="text-gray-500">Profile settings coming soon.</p>
            </div>
          )}

          {activeTab === 'preferences' && (
            <div>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Preferences</h2>
              <p className="text-gray-500">Preferences coming soon.</p>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div>
              <h2 className="text-xl font-semibold text-gray-900 mb-4">Notifications</h2>
              <p className="text-gray-500">Notification settings coming soon.</p>
            </div>
          )}

          {activeTab === 'mobile' && (
            <div className="space-y-8">
              <ConnectedDevices />
              
              {/* Future Expansion Sections */}
              <div className="pt-8 border-t border-gray-100 opacity-50 pointer-events-none">
                <h3 className="text-lg font-medium text-gray-900 mb-4">Sync Status</h3>
                <p className="text-sm text-gray-500">Coming soon.</p>
              </div>
              <div className="pt-8 border-t border-gray-100 opacity-50 pointer-events-none">
                <h3 className="text-lg font-medium text-gray-900 mb-4">Permissions</h3>
                <p className="text-sm text-gray-500">Coming soon.</p>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
