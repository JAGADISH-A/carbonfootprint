import { useState, useRef, useEffect, useCallback } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Sparkles, RefreshCw } from 'lucide-react'
import { ChatMessage as ChatMessageType } from '@/types/activity'
import { sendChatMessage } from '@/api/services'
import { ChatMessage, ChatInput, TypingIndicator } from '@/components/chat'

const INITIAL_MESSAGE: ChatMessageType = {
  role: 'assistant',
  content:
    "I've analyzed your uploaded receipts. Ask me anything about your emissions, purchases, sustainability, or ways to improve.",
  timestamp: Date.now(),
}

const SUGGESTED_QUESTIONS = [
  'Why is my carbon footprint high?',
  'Which purchase produced the most CO₂?',
  'Suggest greener alternatives',
  'How can I reduce emissions this week?',
  'Explain this calculation',
  'Compare with previous receipts',
  'Give me an action plan',
]

interface CoachChatProps {
  enabled: boolean
}

export default function CoachChat({ enabled }: CoachChatProps) {
  const [messages, setMessages] = useState<ChatMessageType[]>([INITIAL_MESSAGE])
  const [loading, setLoading] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const abortRef = useRef<AbortController | null>(null)
  const inputKey = useRef(0)

  const scrollToBottom = useCallback(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [])

  useEffect(() => {
    scrollToBottom()
  }, [messages, loading, scrollToBottom])

  useEffect(() => {
    if (!enabled && messages.length === 1) {
      setMessages([{ ...INITIAL_MESSAGE, timestamp: Date.now() }])
    }
  }, [enabled, messages.length])

  const sendMessage = useCallback(
    async (msgs: ChatMessageType[]) => {
      setLoading(true)
      abortRef.current?.abort()
      const controller = new AbortController()
      abortRef.current = controller

      try {
        const response = await sendChatMessage(
          { messages: msgs },
          controller.signal
        )

        if (response.success && response.data) {
          const reply = response.data.reply || "I couldn't generate a response."
          setMessages((prev) => [
            ...prev,
            {
              role: 'assistant',
              content: reply,
              timestamp: Date.now(),
              cards: response.data!.cards,
            },
          ])
        } else {
          // Backend returned 200 but success=false — show backend message
          const errorMsg = response.message || 'Something went wrong. Please try again.'
          setMessages((prev) => [
            ...prev,
            {
              role: 'assistant',
              content: errorMsg,
              timestamp: Date.now(),
            },
          ])
        }
      } catch (err: unknown) {
        if (err instanceof Error && err.name === 'AbortError') return

        // Extract meaningful error from Axios or network error
        let errorMessage = 'Connection error. Please check your network and try again.'
        const axiosErr = err as Record<string, unknown>
        if (axiosErr.userMessage) {
          errorMessage = String(axiosErr.userMessage)
        } else if (err instanceof Error && err.message) {
          if (err.message.includes('timeout')) {
            errorMessage = 'The request timed out. The AI is taking longer than usual — please try again.'
          } else if (err.message.includes('Network Error')) {
            errorMessage = 'Network error. Please check your connection and try again.'
          }
        }

        setMessages((prev) => [
          ...prev,
          {
            role: 'assistant',
            content: errorMessage,
            timestamp: Date.now(),
          },
        ])
      } finally {
        setLoading(false)
      }
    },
    []
  )

  const handleSend = async (content: string) => {
    const userMessage: ChatMessageType = {
      role: 'user',
      content,
      timestamp: Date.now(),
    }
    const updatedMessages = [...messages, userMessage]
    setMessages(updatedMessages)
    await sendMessage(updatedMessages)
  }

  const handleRegenerate = async () => {
    if (loading) return
    let lastUserIdx = -1
    for (let i = messages.length - 1; i >= 0; i--) {
      if (messages[i].role === 'user') {
        lastUserIdx = i
        break
      }
    }
    if (lastUserIdx === -1) return
    const msgsUpToLastUser = messages.slice(0, lastUserIdx + 1)
    setMessages(msgsUpToLastUser)
    await sendMessage(msgsUpToLastUser)
  }

  const showSuggestions = !loading && messages.length <= 1
  const canRegenerate =
    !loading &&
    messages.length >= 2 &&
    messages[messages.length - 1].role === 'assistant'

  return (
    <div className="flex flex-col h-full bg-surface/50 rounded-xl border border-border-light overflow-hidden">
      {/* Messages area */}
      <div className="flex-1 overflow-y-auto px-3 py-3">
        {messages.map((msg, i) => (
          <ChatMessage
            key={`${i}-${msg.timestamp}`}
            message={msg}
            isLast={i === messages.length - 1 && !loading}
          />
        ))}
        <AnimatePresence>
          {loading && <TypingIndicator />}
        </AnimatePresence>
        <div ref={messagesEndRef} />
      </div>

      {/* Suggestion chips */}
      <AnimatePresence>
        {showSuggestions && (
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 3, transition: { duration: 0.15 } }}
            transition={{ duration: 0.3, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
            className="px-3 pb-2"
          >
            <div className="flex items-center gap-1 mb-1.5">
              <Sparkles className="w-2.5 h-2.5 text-emerald-500" />
              <span className="text-[9px] font-medium text-ink-muted uppercase tracking-wider">
                Suggested
              </span>
            </div>
            <div className="flex flex-wrap gap-1">
              {SUGGESTED_QUESTIONS.map((q, i) => (
                <motion.button
                  key={q}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ duration: 0.2, delay: 0.5 + i * 0.03 }}
                  whileHover={{ scale: 1.03 }}
                  whileTap={{ scale: 0.97 }}
                  onClick={() => handleSend(q)}
                  disabled={!enabled}
                  className="px-2.5 py-1 text-[11px] font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/60 rounded-full transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                >
                  {q}
                </motion.button>
              ))}
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Regenerate */}
      <AnimatePresence>
        {canRegenerate && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: 'auto' }}
            exit={{ opacity: 0, height: 0 }}
            className="px-3 overflow-hidden"
          >
            <div className="flex justify-center pb-1.5">
              <motion.button
                onClick={handleRegenerate}
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.96 }}
                className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-[10px] font-medium text-ink-muted hover:text-emerald-600 bg-white border border-border-light hover:border-emerald-200 transition-colors"
              >
                <RefreshCw className="w-2.5 h-2.5" />
                Regenerate
              </motion.button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Input area */}
      <ChatInput
        key={inputKey.current}
        onSend={handleSend}
        disabled={!enabled || loading}
        autoFocus
      />
    </div>
  )
}
