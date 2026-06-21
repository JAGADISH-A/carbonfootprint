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

const DEFAULT_SUGGESTIONS = [
  'Why is my carbon footprint high?',
  'Which purchase produced the most CO₂?',
  'Suggest greener alternatives',
  'How can I reduce emissions this week?',
  'Explain this calculation',
  'Compare with previous receipts',
  'Give me an action plan',
]

const SUGGESTION_BLOCK_RE = /```suggested_questions\s*\n\[.*?\]\s*\n```/gs

function stripSuggestionBlocks(text: string): string {
  return text.replace(SUGGESTION_BLOCK_RE, '').trim()
}

function extractSuggestionsFromText(text: string): string[] {
  const match = text.match(/```suggested_questions\s*\n(\[.*?\])\s*\n```/s)
  if (!match) return []
  try {
    const parsed = JSON.parse(match[1])
    if (Array.isArray(parsed)) return parsed.filter((s): s is string => typeof s === 'string' && s.trim().length > 0)
  } catch { /* not valid JSON */ }
  return []
}

function parseSuggestions(raw: string[] | undefined): string[] {
  if (!raw || raw.length === 0) return []
  const result: string[] = []
  for (const item of raw) {
    if (typeof item !== 'string') continue
    const trimmed = item.trim()
    if (!trimmed) continue
    if (trimmed.startsWith('[')) {
      try {
        const parsed = JSON.parse(trimmed)
        if (Array.isArray(parsed)) {
          for (const p of parsed) {
            if (typeof p === 'string' && p.trim()) result.push(p.trim())
          }
          continue
        }
      } catch {
        // not valid JSON — treat as plain string
      }
    }
    result.push(trimmed)
  }
  return result.length > 0 ? result : []
}

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
          const rawReply = response.data.reply || "I couldn't generate a response."
          const cleanReply = stripSuggestionBlocks(rawReply)
          const apiSuggestions = response.data!.suggestedQuestions ?? []
          const textSuggestions = extractSuggestionsFromText(rawReply)
          const mergedSuggestions = parseSuggestions(apiSuggestions.length > 0 ? apiSuggestions : textSuggestions)
          setMessages((prev) => [
            ...prev,
            {
              role: 'assistant',
              content: cleanReply,
              timestamp: Date.now(),
              cards: response.data!.cards,
              suggestedQuestions: mergedSuggestions.length > 0 ? mergedSuggestions : undefined,
            },
          ])
        } else {
          const errorMsg = stripSuggestionBlocks(response.message || 'Something went wrong. Please try again.')
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

  const lastAssistantMsg = !loading && messages.length >= 1
    ? [...messages].reverse().find((m) => m.role === 'assistant')
    : null

  const dynamicSuggestions = parseSuggestions(lastAssistantMsg?.suggestedQuestions)
  const showDynamicSuggestions = !loading && dynamicSuggestions.length > 0 && messages.length > 1
  const showInitialSuggestions = !loading && messages.length <= 1
  const canRegenerate =
    !loading &&
    messages.length >= 2 &&
    messages[messages.length - 1].role === 'assistant'

  return (
    <div className="flex flex-col h-full rounded-xl overflow-hidden" style={{ background: '#F8FAFC' }}>
      {/* Messages area */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        <div className="max-w-[800px] mx-auto space-y-4">
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
      </div>

      {/* Dynamic suggestions from backend */}
      <AnimatePresence>
        {showDynamicSuggestions && (
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 3, transition: { duration: 0.15 } }}
            transition={{ duration: 0.3, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
            className="px-4 pb-2"
          >
            <div className="max-w-[900px] mx-auto">
              <div className="flex items-center gap-1.5 mb-2">
                <Sparkles className="w-3 h-3 text-emerald-500" />
                <span className="text-[11px] font-medium text-ink-muted uppercase tracking-wider">
                  Suggested follow-ups
                </span>
              </div>
              <div className="flex flex-wrap gap-2">
                {dynamicSuggestions.map((q, i) => (
                  <motion.button
                    key={`${q}-${i}`}
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ duration: 0.2, delay: 0.35 + i * 0.04 }}
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={() => handleSend(q)}
                    disabled={!enabled}
                    className="px-3 py-1.5 text-[13px] font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/60 rounded-full transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {q}
                  </motion.button>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Initial suggestions (shown only on first load) */}
      <AnimatePresence>
        {showInitialSuggestions && (
          <motion.div
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 3, transition: { duration: 0.15 } }}
            transition={{ duration: 0.3, delay: 0.4, ease: [0.22, 1, 0.36, 1] }}
            className="px-4 pb-2"
          >
            <div className="max-w-[900px] mx-auto">
              <div className="flex items-center gap-1.5 mb-2">
                <Sparkles className="w-3 h-3 text-emerald-500" />
                <span className="text-[11px] font-medium text-ink-muted uppercase tracking-wider">
                  Suggested
                </span>
              </div>
              <div className="flex flex-wrap gap-2">
                {DEFAULT_SUGGESTIONS.map((q, i) => (
                  <motion.button
                    key={q}
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ duration: 0.2, delay: 0.5 + i * 0.03 }}
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    onClick={() => handleSend(q)}
                    disabled={!enabled}
                    className="px-3 py-1.5 text-[13px] font-medium text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200/60 rounded-full transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
                  >
                    {q}
                  </motion.button>
                ))}
              </div>
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
            className="px-4 overflow-hidden"
          >
            <div className="max-w-[900px] mx-auto flex justify-center pb-2">
              <motion.button
                onClick={handleRegenerate}
                whileHover={{ scale: 1.04 }}
                whileTap={{ scale: 0.96 }}
                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-[12px] font-medium text-ink-muted hover:text-emerald-600 bg-white border border-border-light hover:border-emerald-200 transition-colors shadow-sm"
              >
                <RefreshCw className="w-3 h-3" />
                Regenerate
              </motion.button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Input area */}
      <div className="px-4 pb-3 pt-1">
        <div className="max-w-[900px] mx-auto">
          <ChatInput
            key={inputKey.current}
            onSend={handleSend}
            disabled={!enabled || loading}
            autoFocus
          />
        </div>
      </div>
    </div>
  )
}
