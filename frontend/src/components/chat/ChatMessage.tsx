import { BadgeCheck, BotMessageSquare, Check, Copy, Pencil, X } from 'lucide-react'
import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import type { ChatMessage as ChatMessageModel } from '../../types/rag'
import { SourceList } from './SourceList'
import { TextToSpeechButton } from './TextToSpeechButton'

type ChatMessageProps = {
  message: ChatMessageModel
  isEditing: boolean
  editText: string
  isLoading: boolean
  onStartEdit: (message: ChatMessageModel) => void
  onEditTextChange: (value: string) => void
  onSaveEdit: (messageId: string) => void
  onCancelEdit: () => void
}

function copyableText(message: ChatMessageModel) {
  return message.role === 'assistant'
    ? message.content.replace(/\s*\[Source\s+\d+\]/gi, '').trim()
    : message.content
}

export function ChatMessage({
  message,
  isEditing,
  editText,
  isLoading,
  onStartEdit,
  onEditTextChange,
  onSaveEdit,
  onCancelEdit,
}: ChatMessageProps) {
  const [isCopied, setIsCopied] = useState(false)
  const copyTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const editInputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => () => {
    if (copyTimeoutRef.current) clearTimeout(copyTimeoutRef.current)
  }, [])

  useEffect(() => {
    if (!isEditing) return
    editInputRef.current?.focus()
    editInputRef.current?.setSelectionRange(editText.length, editText.length)
  }, [isEditing])

  async function copyMessage() {
    if (!navigator.clipboard) return
    try {
      await navigator.clipboard.writeText(copyableText(message))
      setIsCopied(true)
      if (copyTimeoutRef.current) clearTimeout(copyTimeoutRef.current)
      copyTimeoutRef.current = setTimeout(() => setIsCopied(false), 1_500)
    } catch {
      setIsCopied(false)
    }
  }

  function handleEditKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === 'Escape') {
      event.preventDefault()
      onCancelEdit()
      return
    }
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      if (editText.trim() && !isLoading) onSaveEdit(message.id)
    }
  }

  if (message.role === 'user') {
    return (
      <article className="ml-auto max-w-[90%] rounded-2xl rounded-br-md bg-blue-600 px-4 py-3 text-sm leading-6 text-white shadow-sm shadow-blue-600/20 sm:max-w-[75%]">
        {isEditing ? <>
          <textarea
            ref={editInputRef}
            value={editText}
            rows={2}
            disabled={isLoading}
            onChange={(event) => onEditTextChange(event.target.value)}
            onKeyDown={handleEditKeyDown}
            aria-label="Edit message"
            className="w-full resize-y rounded-lg border border-white/50 bg-white px-3 py-2 text-sm leading-6 text-slate-900 outline-none focus:ring-2 focus:ring-orange-300 disabled:cursor-not-allowed disabled:opacity-70"
          />
          <div className="mt-3 flex justify-end gap-2">
            <button type="button" onClick={() => onSaveEdit(message.id)} disabled={!editText.trim() || isLoading} aria-label="Save edited message" title="Save edited message" className="inline-flex size-7 items-center justify-center rounded-md bg-white text-blue-700 hover:bg-blue-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-white disabled:cursor-not-allowed disabled:opacity-50"><Check className="size-4" aria-hidden="true" /></button>
            <button type="button" onClick={onCancelEdit} disabled={isLoading} aria-label="Cancel edit" title="Cancel edit" className="inline-flex size-7 items-center justify-center rounded-md border border-white/50 text-white hover:bg-white/10 focus:outline-none focus-visible:ring-2 focus-visible:ring-white disabled:cursor-not-allowed disabled:opacity-50"><X className="size-4" aria-hidden="true" /></button>
          </div>
        </> : <>
          <p className="whitespace-pre-wrap break-words">{message.content}</p>
          <div className="mt-2 flex justify-end gap-3 text-xs font-medium text-blue-100">
            <button type="button" onClick={() => onStartEdit(message)} disabled={isLoading} aria-label="Edit message" title="Edit message" className="inline-flex items-center gap-1 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white disabled:cursor-not-allowed disabled:opacity-50"><Pencil className="size-3.5" aria-hidden="true" />Edit</button>
            <button type="button" onClick={() => void copyMessage()} aria-label="Copy user message" title="Copy user message" className="inline-flex items-center gap-1 hover:text-white focus:outline-none focus-visible:ring-2 focus-visible:ring-white"><Copy className="size-3.5" aria-hidden="true" />{isCopied ? 'Copied' : 'Copy'}</button>
          </div>
        </>}
      </article>
    )
  }

  return (
    <article className="max-w-4xl rounded-xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/40 sm:p-5">
      <div className="flex items-center gap-2 text-sm font-semibold text-slate-900">
        <span className="flex size-7 items-center justify-center rounded-md bg-blue-50 text-blue-700"><BotMessageSquare className="size-4" aria-hidden="true" /></span>
        DocSearch
      </div>
      <p className="mt-3 whitespace-pre-wrap break-words text-sm leading-7 text-slate-700">{message.content}</p>
      <div className="mt-4 flex flex-wrap items-center gap-3">
        {message.content.trim() && <TextToSpeechButton text={message.content} className="mt-0" />}
        <button type="button" onClick={() => void copyMessage()} aria-label="Copy response" title="Copy response" className="inline-flex items-center gap-1.5 rounded-md border border-slate-200 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-600 transition-colors hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-500 focus-visible:ring-offset-2"><Copy className="size-3.5" aria-hidden="true" />{isCopied ? 'Copied' : 'Copy'}</button>
      </div>
      {message.citationValid && (message.sources?.length ?? 0) > 0 && <p className="mt-4 flex items-center gap-1.5 text-xs font-medium text-emerald-700"><BadgeCheck className="size-4" aria-hidden="true" />Sources verified</p>}
      {(message.sources?.length ?? 0) > 0 && <SourceList sources={message.sources ?? []} />}
    </article>
  )
}
