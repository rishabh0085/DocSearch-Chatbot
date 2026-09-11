import { BotMessageSquare } from 'lucide-react'
import type { ChatMessage as ChatMessageModel } from '../../types/rag'
import { ChatMessage } from './ChatMessage'

type ChatMessageListProps = {
  messages: ChatMessageModel[]
  isLoading: boolean
  editingMessageId: string | null
  editText: string
  onStartEdit: (message: ChatMessageModel) => void
  onEditTextChange: (value: string) => void
  onSaveEdit: (messageId: string) => void
  onCancelEdit: () => void
}

export function ChatMessageList({
  messages,
  isLoading,
  editingMessageId,
  editText,
  onStartEdit,
  onEditTextChange,
  onSaveEdit,
  onCancelEdit,
}: ChatMessageListProps) {
  return <div className="mx-auto flex w-full max-w-4xl flex-col gap-5 px-4 py-6 sm:px-6 sm:py-8">
    {messages.map((message) => <ChatMessage
      key={message.id}
      message={message}
      isEditing={message.id === editingMessageId}
      editText={editText}
      isLoading={isLoading}
      onStartEdit={onStartEdit}
      onEditTextChange={onEditTextChange}
      onSaveEdit={onSaveEdit}
      onCancelEdit={onCancelEdit}
    />)}
    {isLoading && <article className="max-w-sm rounded-xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/40"><div className="flex items-center gap-2 text-sm font-semibold text-slate-900"><span className="flex size-7 items-center justify-center rounded-md bg-blue-50 text-blue-700"><BotMessageSquare className="size-4" aria-hidden="true" /></span>DocSearch</div><div className="mt-3 flex items-center gap-2 text-sm text-slate-500" role="status"><span className="size-2 animate-pulse rounded-full bg-blue-500 motion-reduce:animate-none" aria-hidden="true" />Thinking...</div></article>}
  </div>
}
