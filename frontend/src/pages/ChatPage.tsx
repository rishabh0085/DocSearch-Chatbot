import { ArrowRight, MessageSquarePlus, Sparkles } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { ChatComposer } from '../components/chat/ChatComposer'
import { ChatMessageList } from '../components/chat/ChatMessageList'
import { askRagQuestion, UnexpectedRagResponseError } from '../services/ragService'
import type { ChatMessage } from '../types/rag'

const suggestedQuestions = [
  'What is the notice period for resignation?',
  'What is the notice period for the technical team?',
  'What is the notice period for the non-technical team?',
  'What does the company policy say about resignation?',
]

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [question, setQuestion] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [errorMessage, setErrorMessage] = useState<string | null>(null)
  const [failedQuestion, setFailedQuestion] = useState<string | null>(null)
  const [failedEditMessageId, setFailedEditMessageId] = useState<string | null>(null)
  const [editingMessageId, setEditingMessageId] = useState<string | null>(null)
  const [editText, setEditText] = useState('')
  const requestInFlight = useRef(false)
  const conversationEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    conversationEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' })
  }, [messages, isLoading, errorMessage])

  async function submitQuestion(questionToSubmit: string, editedMessageId?: string) {
    const trimmedQuestion = questionToSubmit.trim()
    if (!trimmedQuestion || requestInFlight.current) return

    requestInFlight.current = true
    if (!editedMessageId) setQuestion('')
    setErrorMessage(null)
    setFailedQuestion(null)
    setFailedEditMessageId(null)
    setMessages((currentMessages) => {
      if (!editedMessageId) {
        return [...currentMessages, { id: createMessageId(), role: 'user', content: trimmedQuestion }]
      }

      const messageIndex = currentMessages.findIndex((message) => message.id === editedMessageId)
      if (messageIndex < 0) return currentMessages
      return currentMessages.slice(0, messageIndex + 1).map((message) => (
        message.id === editedMessageId ? { ...message, content: trimmedQuestion } : message
      ))
    })
    setIsLoading(true)
    try {
      const response = await askRagQuestion(trimmedQuestion)
      setMessages((currentMessages) => [...currentMessages, {
        id: createMessageId(),
        role: 'assistant',
        content: response.answer,
        sources: response.sources,
        citationValid: response.citationValid,
      }])
    } catch (error) {
      console.error('RAG request failed', error)
      setFailedQuestion(trimmedQuestion)
      setFailedEditMessageId(editedMessageId ?? null)
      setErrorMessage(error instanceof UnexpectedRagResponseError
        ? error.message
        : "Sorry, I couldn't process that question right now. Please try again.")
    } finally {
      requestInFlight.current = false
      setIsLoading(false)
    }
  }

  function startEditing(message: ChatMessage) {
    if (message.role !== 'user' || isLoading) return
    setEditingMessageId(message.id)
    setEditText(message.content)
  }

  function cancelEditing() {
    setEditingMessageId(null)
    setEditText('')
  }

  function saveEditedMessage(messageId: string) {
    if (!editText.trim() || isLoading) return
    const editedQuestion = editText
    cancelEditing()
    void submitQuestion(editedQuestion, messageId)
  }

  function startNewChat() {
    setMessages([])
    setQuestion('')
    setErrorMessage(null)
    setFailedQuestion(null)
    setFailedEditMessageId(null)
    cancelEditing()
  }

  return (
    <section className="flex h-[calc(100vh-68px)] min-h-[32rem] flex-col overflow-hidden bg-[#FFFCFA] sm:h-[calc(100vh-72px)]">
      <header className="flex items-center justify-between gap-4 border-b border-[#F3D8C8] bg-white px-4 py-4 sm:px-6">
        <div className="min-w-0"><p className="text-xs font-semibold uppercase tracking-[0.12em] text-blue-600">AI workspace</p><h1 className="mt-1 text-lg font-semibold tracking-tight text-slate-900">AI Assistant</h1><p className="mt-0.5 truncate text-sm text-slate-500">Answers grounded in your organization&apos;s knowledge.</p></div>
        <button type="button" onClick={startNewChat} disabled={isLoading} className="inline-flex shrink-0 items-center gap-2 rounded-lg bg-blue-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60">
          <MessageSquarePlus className="size-4" aria-hidden="true" /><span className="hidden sm:inline">New Chat</span><span className="sm:hidden">New</span>
        </button>
      </header>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {messages.length === 0 && !isLoading ? (
          <div className="mx-auto flex min-h-full w-full max-w-3xl flex-col justify-center px-4 py-10 sm:px-6">
            <div className="relative overflow-hidden rounded-2xl border border-[#F3D8C8] bg-gradient-to-br from-white via-white to-[#FFF8F3] p-6 shadow-md shadow-orange-100/40 sm:p-8">
              <div className="pointer-events-none absolute -right-14 -top-14 size-40 rounded-full bg-[#FFF0E6]/70 blur-2xl" aria-hidden="true" />
              <span className="relative flex size-11 items-center justify-center rounded-xl bg-blue-100 text-blue-600 ring-1 ring-blue-200"><Sparkles className="size-5" aria-hidden="true" /></span>
              <p className="mt-5 text-sm font-semibold text-blue-600">DocSearch</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">How can I help you?</h2>
              <p className="mt-3 max-w-xl text-sm leading-6 text-slate-600">Ask questions about your organization&apos;s documents and get answers grounded in your indexed knowledge.</p>
              <div className="relative mt-7"><p className="text-sm font-semibold text-slate-800">Suggested questions</p><div className="mt-3 grid gap-2 sm:grid-cols-2">
                {suggestedQuestions.map((suggestion) => <button key={suggestion} type="button" onClick={() => setQuestion(suggestion)} className="group flex items-center justify-between gap-3 rounded-lg border border-[#F3D8C8] bg-white px-3 py-3 text-left text-sm leading-5 text-slate-600 shadow-sm shadow-orange-100/20 transition-all hover:-translate-y-0.5 hover:border-blue-200 hover:bg-blue-50 hover:text-slate-800 hover:shadow-sm focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"><span>{suggestion}</span><ArrowRight className="size-4 shrink-0 text-blue-600 opacity-0 transition-opacity group-hover:opacity-100" aria-hidden="true" /></button>)}
              </div></div>
            </div>
          </div>
        ) : <ChatMessageList messages={messages} isLoading={isLoading} editingMessageId={editingMessageId} editText={editText} onStartEdit={startEditing} onEditTextChange={setEditText} onSaveEdit={saveEditedMessage} onCancelEdit={cancelEditing} />}
        {errorMessage && <div className="mx-auto w-full max-w-4xl px-4 pb-6 sm:px-6"><div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 shadow-sm" role="alert"><p className="font-medium">{errorMessage}</p>{failedQuestion && <button type="button" onClick={() => void submitQuestion(failedQuestion, failedEditMessageId ?? undefined)} className="mt-2 font-medium text-rose-800 underline underline-offset-2 hover:text-rose-950 focus:outline-none focus-visible:ring-2 focus-visible:ring-rose-600 focus-visible:ring-offset-2">Try again</button>}</div></div>}
        <div ref={conversationEndRef} />
      </div>
      <ChatComposer value={question} isLoading={isLoading} onChange={setQuestion} onSubmit={() => void submitQuestion(question)} />
    </section>
  )
}
