import { useRef } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import {
  Home,
  FileUp,
  Sparkles,
  BarChart3,
  History,
  Settings,
  Leaf,
  X,
} from 'lucide-react'
import clsx from 'clsx'

const navItems = [
  { to: '/',         icon: Home,       label: 'Home' },
  { to: '/upload',   icon: FileUp,     label: 'Upload Receipt' },
  { to: '/coach',    icon: Sparkles,   label: 'AI Coach' },
  { to: '/insights', icon: BarChart3,  label: 'Insights' },
  { to: '/history',  icon: History,    label: 'History' },
  { to: '/settings', icon: Settings,   label: 'Settings' },
]

interface SidebarProps {
  open: boolean
  onClose: () => void
}

function NavItem({
  to,
  icon: Icon,
  label,
  isActive,
  onClick,
}: {
  to: string
  icon: React.ComponentType<{ className?: string }>
  label: string
  isActive: boolean
  onClick: () => void
}) {
  const ref = useRef<HTMLAnchorElement>(null)
  return (
    <NavLink
      ref={ref}
      to={to}
      end={to === '/'}
      onClick={onClick}
      className={clsx(
        'relative flex items-center gap-3 px-4 py-3 rounded-2xl text-[13px] font-medium',
        'transition-colors duration-200',
        isActive
          ? 'text-emerald-700'
          : 'text-ink-muted hover:text-ink'
      )}
    >
      {isActive && (
        <motion.div
          layoutId="sidebar-active"
          className="absolute inset-0 bg-emerald-50 rounded-2xl"
          transition={{ type: 'spring', stiffness: 350, damping: 30 }}
        />
      )}
      <Icon
        className={clsx(
          'w-[18px] h-[18px] flex-shrink-0 relative z-10 transition-colors duration-200',
          isActive ? 'text-emerald-600' : 'text-ink-ghost'
        )}
      />
      <span className="relative z-10">{label}</span>
    </NavLink>
  )
}

export default function Sidebar({ open, onClose }: SidebarProps) {
  const location = useLocation()

  return (
    <>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-40 bg-ink/10 backdrop-blur-sm lg:hidden"
            onClick={onClose}
          />
        )}
      </AnimatePresence>

      <aside
        className={clsx(
          'fixed inset-y-0 left-0 z-50 flex w-[260px] flex-col bg-card border-r border-border-light',
          'transition-transform duration-300 ease-out',
          'lg:static lg:translate-x-0',
          open ? 'translate-x-0' : '-translate-x-full'
        )}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-6 py-6">
          <motion.div
            className="w-10 h-10 rounded-2xl bg-emerald-500 flex items-center justify-center shadow-emerald"
            whileHover={{ scale: 1.05, rotate: 5 }}
            transition={{ type: 'spring', stiffness: 400, damping: 15 }}
          >
            <Leaf className="w-5 h-5 text-white" />
          </motion.div>
          <div>
            <span className="text-base font-semibold tracking-tight text-ink block leading-tight">
              CarbonWise
            </span>
            <span className="text-[11px] text-ink-faint leading-tight">Sustainability Platform</span>
          </div>
          <button
            onClick={onClose}
            className="ml-auto p-1.5 rounded-xl hover:bg-surface lg:hidden"
          >
            <X className="w-4 h-4 text-ink-muted" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-2 space-y-0.5">
          {navItems.map((item) => (
            <NavItem
              key={item.to}
              to={item.to}
              icon={item.icon}
              label={item.label}
              isActive={
                item.to === '/'
                  ? location.pathname === '/'
                  : location.pathname.startsWith(item.to)
              }
              onClick={onClose}
            />
          ))}
        </nav>

        {/* Footer tip */}
        <div className="px-4 pb-5">
          <motion.div
            className="rounded-2xl bg-emerald-50 border border-emerald-100 p-4"
            initial={{ opacity: 0, y: 8 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.5, duration: 0.5 }}
          >
            <div className="flex items-center gap-2 mb-1.5">
              <motion.div
                className="w-1.5 h-1.5 rounded-full bg-emerald-500"
                animate={{ scale: [1, 1.3, 1], opacity: [1, 0.7, 1] }}
                transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
              />
              <span className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider">
                Quick Tip
              </span>
            </div>
            <p className="text-xs text-emerald-600/80 leading-relaxed">
              Upload your first receipt to start tracking your carbon footprint with AI-powered insights.
            </p>
          </motion.div>
        </div>
      </aside>
    </>
  )
}
