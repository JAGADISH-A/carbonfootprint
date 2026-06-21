import { Menu, Bell } from 'lucide-react'
import { useDashboard } from '@/api/DashboardContext'

interface HeaderProps {
  onMenuToggle: () => void
}

export default function Header({ onMenuToggle }: HeaderProps) {
  const { devices } = useDashboard()
  const deviceCount = devices?.length ?? 0
  const initials = 'C'

  return (
    <header className="flex items-center gap-4 px-6 py-3 border-b border-border-light bg-white/70 backdrop-blur-sm">
      <button
        onClick={onMenuToggle}
        className="p-2 rounded-xl hover:bg-surface-warm lg:hidden transition-colors"
      >
        <Menu className="w-5 h-5 text-ink-muted" />
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-2">
        <button className="relative p-2.5 rounded-2xl hover:bg-surface transition-colors">
          <Bell className="w-5 h-5 text-ink-muted" />
          {deviceCount > 0 && (
            <span className="absolute top-2 right-2 w-2 h-2 bg-emerald-500 rounded-full" />
          )}
        </button>

        <div className="w-9 h-9 rounded-2xl bg-emerald-100 flex items-center justify-center ml-1">
          <span className="text-sm font-semibold text-emerald-700">{initials}</span>
        </div>
      </div>
    </header>
  )
}
