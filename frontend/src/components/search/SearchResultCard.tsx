import { ChevronDown, ChevronUp, FileText } from 'lucide-react'
import { useId, useState } from 'react'
import type { SearchResultResponse } from '../../types/search'

type SearchResultCardProps = { result: SearchResultResponse; index: number }

const PREVIEW_LENGTH = 500

export function SearchResultCard({ result, index }: SearchResultCardProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const contentId = useId()
  const isLongContent = result.content.length > PREVIEW_LENGTH
  const content = isExpanded || !isLongContent ? result.content : `${result.content.slice(0, PREVIEW_LENGTH).trimEnd()}...`
  const relevance = Math.round(Math.min(1, Math.max(0, result.similarity)) * 100)

  return (
    <article className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm shadow-slate-200/40 transition-colors hover:border-slate-300 sm:p-6" aria-labelledby={`search-result-${index}`}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="min-w-0">
          <div className="flex items-center gap-2 text-blue-700"><FileText className="size-4 shrink-0" aria-hidden="true" /><h3 id={`search-result-${index}`} className="break-words text-base font-semibold text-slate-900">{result.documentName}</h3></div>
          <p className="mt-2 text-sm text-slate-500">Page {result.pageNumber} <span aria-hidden="true">·</span> Chunk {result.chunkNumber}</p>
        </div>
        <div className="shrink-0 rounded-lg bg-blue-50 px-3 py-2 text-right ring-1 ring-blue-100" aria-label={`Relevance ${relevance}%`}>
          <p className="text-xs font-medium text-slate-600">Relevance</p><p className="mt-0.5 text-sm font-semibold text-blue-700">{relevance}%</p>
        </div>
      </div>
      <p id={contentId} className="mt-5 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">{content}</p>
      {isLongContent && <button type="button" onClick={() => setIsExpanded((expanded) => !expanded)} aria-expanded={isExpanded} aria-controls={contentId} className="mt-3 inline-flex items-center gap-1 text-sm font-medium text-blue-700 transition-colors hover:text-blue-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">
        {isExpanded ? <>Show less <ChevronUp className="size-4" aria-hidden="true" /></> : <>Show more <ChevronDown className="size-4" aria-hidden="true" /></>}
      </button>}
    </article>
  )
}
