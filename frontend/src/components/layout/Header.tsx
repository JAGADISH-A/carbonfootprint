import { Menu, Bell, Search } from 'lucide-react'

interface HeaderProps {
  onMenuToggle: () => void
}

export default function Header({ onMenuToggle }: HeaderProps) {
  return (
    <header className="flex items-center gap-4 px-6 py-3 border-b border-surface-border bg-white/70 backdrop-blur-sm">
      <button
        onClick={onMenuToggle}
        className="p-2 rounded-xl hover:bg-surface-warm lg:hidden"
      >
        <Menu className="w-5 h-5 text-ink-muted" />
      </button>

      <div className="flex-1 max-w-md">
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-ink-faint" />
          <input
            type="text"
            placeholder="Search activities, receipts..."
            className="w-full bg-surface-warm border-0 rounded-2xl pl-10 pr-4 py-2.5 text-sm
                       text-ink placeholder:text-ink-faint focus:outline-none focus:ring-2
                       focus:ring-emerald-100 transition-all"
          />
        </div>
      </div>

      <div className="flex items-center gap-2">
        <button className="relative p-2.5 rounded-2xl hover:bg-surface-warm transition-colors">
          <Bell className="w-5 h-5 text-ink-muted" />
          <span className="absolute top-2 right-2 w-2 h-2 bg-emerald-500 rounded-full" />
        </button>

        <div className="w-9 h-9 rounded-2xl bg-emerald-100 flex items-center justify-center ml-1">
          <span className="text-sm font-semibold text-emerald-700">U</span>
        </div>
      </div>
    </header>
  )
}
