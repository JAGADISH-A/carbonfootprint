import type { ReactNode } from 'react'
import { motion } from 'framer-motion'
import clsx from 'clsx'

interface ButtonProps {
  children: ReactNode
  variant?: 'primary' | 'secondary' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  className?: string
  onClick?: () => void
  disabled?: boolean
  type?: 'button' | 'submit'
}

export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  className,
  onClick,
  disabled,
  type = 'button',
}: ButtonProps) {
  return (
    <motion.button
      type={type}
      onClick={onClick}
      disabled={disabled}
      whileHover={disabled ? {} : { scale: 1.03, y: -1 }}
      whileTap={disabled ? {} : { scale: 0.97 }}
      transition={{ type: 'spring', stiffness: 400, damping: 17 }}
      className={clsx(
        'inline-flex items-center justify-center gap-2 font-medium rounded-2xl transition-all duration-200',
        'focus:outline-none focus:ring-2 focus:ring-offset-2',
        {
          'bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm hover:shadow-md focus:ring-emerald-500': variant === 'primary',
          'bg-emerald-50 hover:bg-emerald-100 text-emerald-700 focus:ring-emerald-500': variant === 'secondary',
          'text-ink-muted hover:text-ink hover:bg-emerald-50/50 focus:ring-emerald-500': variant === 'ghost',
          'px-3 py-1.5 text-xs': size === 'sm',
          'px-5 py-2.5 text-sm': size === 'md',
          'px-6 py-3 text-base': size === 'lg',
          'opacity-50 cursor-not-allowed': disabled,
        },
        className
      )}
    >
      {children}
    </motion.button>
  )
}
