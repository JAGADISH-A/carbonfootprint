import clsx from 'clsx'

interface AvatarProps {
  initials?: string
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export default function Avatar({ initials = 'U', size = 'md', className }: AvatarProps) {
  return (
    <div
      className={clsx(
        'rounded-2xl bg-emerald-100 flex items-center justify-center font-semibold text-emerald-700',
        {
          'w-8 h-8 text-xs': size === 'sm',
          'w-10 h-10 text-sm': size === 'md',
          'w-12 h-12 text-base': size === 'lg',
        },
        className
      )}
    >
      {initials}
    </div>
  )
}
