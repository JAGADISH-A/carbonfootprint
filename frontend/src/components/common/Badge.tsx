import clsx from 'clsx'

type BadgeVariant = 'green' | 'muted' | 'amber' | 'blue'

interface BadgeProps {
  children: React.ReactNode
  variant?: BadgeVariant
  className?: string
}

const variantStyles: Record<BadgeVariant, string> = {
  green: 'bg-emerald-100 text-emerald-700',
  muted: 'bg-surface-warm text-ink-muted',
  amber: 'bg-amber-100 text-amber-700',
  blue: 'bg-blue-100 text-blue-700',
}

export default function Badge({ children, variant = 'muted', className }: BadgeProps) {
  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium',
        variantStyles[variant],
        className
      )}
    >
      {children}
    </span>
  )
}
