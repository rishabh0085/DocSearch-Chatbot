import { SendHorizontal } from 'lucide-react'
import type { FormEvent, KeyboardEvent } from 'react'

type ChatComposerProps = {
  value: string
  isLoading: boolean
  onChange: (value: string) => void
  onSubmit: () => void
}

export function ChatComposer({ value, isLoading, onChange, onSubmit }: ChatComposerProps) {
  const canSubmit = value.trim().length > 0 && !isLoading
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (canSubmit) onSubmit()
  }
  function handleKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== 'Enter' || event.shiftKey || event.altKey) return

    event.preventDefault()
    if (canSubmit) onSubmit()
  }
  return (
    <form onSubmit={submit} className="border-t border-[#F3D8C8] bg-white/95 px-4 py-3 backdrop-blur sm:px-6 sm:py-4">
      <label className="sr-only" htmlFor="chat-question">Ask a question about your documents</label>
      <div className="mx-auto flex max-w-4xl items-end gap-2 rounded-xl border border-[#F3D8C8] bg-white p-2 shadow-sm shadow-orange-100/30 transition-colors focus-within:border-blue-600 focus-within:ring-2 focus-within:ring-blue-100">
        <textarea id="chat-question" value={value} rows={1} disabled={isLoading} onChange={(event) => onChange(event.target.value)} onKeyDown={handleKeyDown} placeholder="Ask a question about your documents..." className="max-h-32 min-h-10 flex-1 resize-y border-0 bg-transparent px-2 py-2 text-sm leading-6 text-slate-900 outline-none placeholder:text-slate-400 disabled:cursor-not-allowed disabled:text-slate-500" />
        <button type="submit" disabled={!canSubmit} className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-blue-600 text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:bg-slate-200 disabled:text-slate-400" aria-label="Send question">
          <SendHorizontal className="size-4" aria-hidden="true" />
        </button>
      </div>
      <p className="mx-auto mt-2 max-w-4xl px-1 text-xs text-slate-400">Press Enter to send · Shift + Enter for a new line</p>
    </form>
  )
}
