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
    <div className="max-w-5xl mx-auto">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-ink tracking-tight">Settings</h1>
        <p className="mt-1.5 text-sm text-ink-muted">
          Manage your account settings and connected devices.
        </p>
      </div>

      <div className="flex flex-col md:flex-row gap-6">
        {/* Sidebar Navigation */}
        <aside className="md:w-56 shrink-0">
          <nav className="space-y-1">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`w-full text-left px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${
                  activeTab === tab.id
                    ? 'bg-emerald-50 text-emerald-700 shadow-sm'
                    : 'text-ink-muted hover:bg-surface hover:text-ink'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </nav>
        </aside>

        {/* Content Area */}
        <main className="flex-1 card">
          {activeTab === 'profile' && (
            <div>
              <h2 className="text-lg font-semibold text-ink mb-4">Profile</h2>
              <div className="flex items-center gap-4 mb-6">
                <div className="w-14 h-14 rounded-2xl bg-emerald-100 flex items-center justify-center">
                  <span className="text-xl font-bold text-emerald-700">C</span>
                </div>
                <div>
                  <p className="text-sm font-semibold text-ink">Carbon User</p>
                  <p className="text-xs text-ink-muted">Free Plan</p>
                </div>
              </div>
              <div className="space-y-5">
                <div>
                  <label className="block text-xs font-medium text-ink-muted mb-1.5">Display Name</label>
                  <input
                    type="text"
                    value="Carbon User"
                    disabled
                    className="input opacity-60 cursor-not-allowed"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-ink-muted mb-1.5">Email</label>
                  <input
                    type="email"
                    value="user@carbonwise.app"
                    disabled
                    className="input opacity-60 cursor-not-allowed"
                  />
                </div>
                <p className="text-xs text-ink-faint italic">Profile editing will be available in a future update.</p>
              </div>
            </div>
          )}

          {activeTab === 'preferences' && (
            <div>
              <h2 className="text-lg font-semibold text-ink mb-4">Preferences</h2>
              <div className="space-y-6">
                <div>
                  <label className="block text-xs font-medium text-ink-muted mb-1.5">Units</label>
                  <select className="input" disabled>
                    <option>Kilograms (kg CO₂e)</option>
                  </select>
                  <p className="text-xs text-ink-faint mt-1">Additional unit options coming soon.</p>
                </div>
                <div>
                  <label className="block text-xs font-medium text-ink-muted mb-1.5">Timezone</label>
                  <input
                    type="text"
                    value="Auto-detected"
                    disabled
                    className="input opacity-60 cursor-not-allowed"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-ink-muted mb-1.5">Language</label>
                  <select className="input" disabled>
                    <option>English</option>
                  </select>
                  <p className="text-xs text-ink-faint mt-1">Additional languages coming soon.</p>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'notifications' && (
            <div>
              <h2 className="text-lg font-semibold text-ink mb-4">Notifications</h2>
              <div className="space-y-5">
                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="text-sm font-medium text-ink">Weekly Summary</p>
                    <p className="text-xs text-ink-muted">Receive a weekly carbon footprint summary</p>
                  </div>
                  <div className="w-10 h-5 bg-gray-200 rounded-full opacity-50 cursor-not-allowed" />
                </div>
                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="text-sm font-medium text-ink">High Emission Alerts</p>
                    <p className="text-xs text-ink-muted">Get notified when emissions exceed your average</p>
                  </div>
                  <div className="w-10 h-5 bg-gray-200 rounded-full opacity-50 cursor-not-allowed" />
                </div>
                <div className="flex items-center justify-between py-2">
                  <div>
                    <p className="text-sm font-medium text-ink">Coach Tips</p>
                    <p className="text-xs text-ink-muted">Receive personalized sustainability tips</p>
                  </div>
                  <div className="w-10 h-5 bg-gray-200 rounded-full opacity-50 cursor-not-allowed" />
                </div>
                <p className="text-xs text-ink-faint italic">Notification settings will be available in a future update.</p>
              </div>
            </div>
          )}

          {activeTab === 'mobile' && (
            <ConnectedDevices />
          )}
        </main>
      </div>
    </div>
  )
}
