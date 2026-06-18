import clsx from 'clsx'

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

export default function LoadingSpinner({ size = 'md', className }: LoadingSpinnerProps) {
  return (
    <div className={clsx('flex items-center justify-center', className)}>
      <div
        className={clsx(
          'animate-spin rounded-full border-2 border-emerald-200 border-t-emerald-600',
          {
            'w-5 h-5': size === 'sm',
            'w-8 h-8': size === 'md',
            'w-12 h-12': size === 'lg',
          }
        )}
      />
    </div>
  )
}
