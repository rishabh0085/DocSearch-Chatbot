import { ArrowRight, Search } from 'lucide-react'
import type { SearchHistoryItem as SearchHistoryItemType } from '../../types/searchHistory'

type SearchHistoryItemProps = { item: SearchHistoryItemType; onSearchAgain: (query: string) => void }

function relativeTime(timestamp: string) {
  const difference = Math.max(0, Date.now() - new Date(timestamp).getTime())
  const minutes = Math.floor(difference / 60_000)
  if (minutes < 1) return 'Just now'
  if (minutes < 60) return `${minutes} min ago`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} hr ago`
  const days = Math.floor(hours / 24)
  return `${days} day${days === 1 ? '' : 's'} ago`
}

export function SearchHistoryItem({ item, onSearchAgain }: SearchHistoryItemProps) {
  return <article className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/40 transition-colors hover:border-slate-300 sm:flex-row sm:items-center sm:justify-between sm:p-6">
    <div className="min-w-0"><h2 className="break-words text-base font-semibold text-slate-900">{item.query}</h2><p className="mt-2 text-sm text-slate-500">{item.resultCount} {item.resultCount === 1 ? 'result' : 'results'} <span aria-hidden="true">·</span> <time dateTime={item.timestamp}>{relativeTime(item.timestamp)}</time></p></div>
    <button type="button" onClick={() => onSearchAgain(item.query)} className="inline-flex shrink-0 items-center justify-center gap-2 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:border-blue-200 hover:bg-blue-50 hover:text-blue-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2"><Search className="size-4" aria-hidden="true" />Search again<ArrowRight className="size-4" aria-hidden="true" /></button>
  </article>
}
