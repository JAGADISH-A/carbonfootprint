import { useState, useRef, useEffect } from 'react'
import { motion } from 'framer-motion'
import { Send } from 'lucide-react'

interface ChatInputProps {
  onSend: (message: string) => void
  disabled?: boolean
  autoFocus?: boolean
}

export default function ChatInput({ onSend, disabled, autoFocus }: ChatInputProps) {
  const [value, setValue] = useState('')
  const textareaRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => {
    if (autoFocus && !disabled && textareaRef.current) {
      textareaRef.current.focus()
    }
  }, [autoFocus, disabled])

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      textareaRef.current.style.height =
        Math.min(textareaRef.current.scrollHeight, 100) + 'px'
    }
  }, [value])

  const handleSend = () => {
    const trimmed = value.trim()
    if (!trimmed || disabled) return
    onSend(trimmed)
    setValue('')
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto'
      textareaRef.current.focus()
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="border-t border-border-light bg-white px-3 py-2">
      <div className="flex items-end gap-2">
        <div className="flex-1 relative">
          <textarea
            ref={textareaRef}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Ask anything about your carbon footprint..."
            disabled={disabled}
            rows={1}
            className="w-full resize-none rounded-lg border border-border bg-surface/80 px-3 py-2 text-[13px] text-ink placeholder:text-ink-faint focus:outline-none focus:border-emerald-400 focus:ring-1 focus:ring-emerald-100 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          />
        </div>

        <motion.button
          onClick={handleSend}
          disabled={!value.trim() || disabled}
          whileHover={{ scale: 1.05 }}
          whileTap={{ scale: 0.92 }}
          className="w-8 h-8 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white flex items-center justify-center flex-shrink-0 disabled:opacity-30 disabled:cursor-not-allowed transition-all shadow-sm"
        >
          <Send className="w-3.5 h-3.5" />
        </motion.button>
      </div>
    </div>
  )
}
