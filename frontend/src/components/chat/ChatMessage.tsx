import { useState } from 'react'
import { motion } from 'framer-motion'
import Markdown from 'react-markdown'
import { Sparkles, User, Copy, Check } from 'lucide-react'
import type { ChatMessage as ChatMessageType } from '@/types/activity'
import ChatCards from './ChatCards'

interface ChatMessageProps {
  message: ChatMessageType
  isLast?: boolean
}

function formatTime(ts?: number): string {
  if (!ts) return ''
  const d = new Date(ts)
  const h = d.getHours()
  const m = d.getMinutes().toString().padStart(2, '0')
  const ampm = h >= 12 ? 'PM' : 'AM'
  const hour = h % 12 || 12
  return `${hour}:${m} ${ampm}`
}

export default function ChatMessage({ message, isLast }: ChatMessageProps) {
  const isUser = message.role === 'user'
  const [copied, setCopied] = useState(false)
  const hasCards = !isUser && message.cards && message.cards.length > 0

  const handleCopy = async () => {
    await navigator.clipboard.writeText(message.content)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 6 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
      className={`flex gap-2.5 group ${isLast ? '' : 'mb-3'}`}
    >
      {/* Avatar */}
      <div
        className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 ${
          isUser
            ? 'bg-gradient-to-br from-emerald-500 to-emerald-600'
            : 'bg-gradient-to-br from-emerald-600 to-emerald-700'
        }`}
      >
        {isUser ? (
          <User className="w-3.5 h-3.5 text-white" />
        ) : (
          <Sparkles className="w-3.5 h-3.5 text-white" />
        )}
      </div>

      {/* Message body */}
      <div className={`flex-1 min-w-0 ${isUser ? 'flex flex-col items-end' : ''}`}>
        {/* Label + timestamp */}
        <div className={`flex items-center gap-1.5 mb-0.5 ${isUser ? 'flex-row-reverse' : ''}`}>
          <span className="text-[10px] font-semibold text-ink-muted">
            {isUser ? 'You' : 'EcoBuddy'}
          </span>
          {message.timestamp && (
            <span className="text-[9px] text-ink-faint">
              {formatTime(message.timestamp)}
            </span>
          )}
        </div>

        {/* Bubble */}
        <div
          className={`relative rounded-xl px-3 py-2 text-[13px] leading-relaxed max-w-[80%] ${
            isUser
              ? 'bg-emerald-600 text-white rounded-br-md shadow-sm shadow-emerald-200'
              : 'bg-white border border-border-light text-ink rounded-bl-md shadow-sm'
          }`}
        >
          {isUser ? (
            <p className="whitespace-pre-wrap">{message.content}</p>
          ) : (
            <div className="prose prose-sm prose-emerald max-w-none
              prose-p:my-1 prose-p:leading-relaxed
              prose-strong:text-ink
              prose-ul:my-1 prose-li:my-0.5
              prose-headings:my-1.5 prose-headings:text-ink
              prose-code:text-emerald-700 prose-code:bg-emerald-50 prose-code:px-1 prose-code:py-0.5 prose-code:rounded prose-code:text-xs
              [&>*:first-child]:mt-0 [&>*:last-child]:mb-0">
              <Markdown>{message.content}</Markdown>
            </div>
          )}
        </div>

        {/* Cards */}
        {hasCards && (
          <div className="mt-1.5 max-w-[95%]">
            <ChatCards cards={message.cards!} />
          </div>
        )}

        {/* Actions row (AI messages only) */}
        {!isUser && (
          <div className="flex items-center gap-0.5 mt-0.5 opacity-0 group-hover:opacity-100 transition-opacity duration-150">
            <button
              onClick={handleCopy}
              className="flex items-center gap-1 px-1.5 py-0.5 rounded text-[9px] font-medium text-ink-faint hover:text-ink-muted hover:bg-surface transition-colors"
            >
              {copied ? (
                <>
                  <Check className="w-2.5 h-2.5 text-emerald-500" />
                  <span className="text-emerald-600">Copied</span>
                </>
              ) : (
                <>
                  <Copy className="w-2.5 h-2.5" />
                  Copy
                </>
              )}
            </button>
          </div>
        )}
      </div>
    </motion.div>
  )
}
