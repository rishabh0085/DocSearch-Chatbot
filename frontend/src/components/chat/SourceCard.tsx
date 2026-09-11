import { ChevronDown, ChevronUp, FileText } from 'lucide-react'
import { useId, useState } from 'react'
import type { RagSource } from '../../types/rag'

type SourceCardProps = {
  source: RagSource
}

function getRelevance(similarity: number) {
  return Math.min(100, Math.max(0, Math.round(similarity * 100)))
}

export function SourceCard({ source }: SourceCardProps) {
  const [isExpanded, setIsExpanded] = useState(false)
  const contentId = useId()
  const relevance = getRelevance(source.similarity)
  const action = isExpanded ? 'Collapse' : 'Expand'

  return (
    <article className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm shadow-slate-200/30 transition-colors hover:border-slate-300">
      <button
        type="button"
        onClick={() => setIsExpanded((current) => !current)}
        aria-expanded={isExpanded}
        aria-controls={contentId}
        aria-label={`${action} source from ${source.documentName}`}
        className="flex w-full items-start gap-3 p-3.5 text-left transition-colors hover:bg-slate-50 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-600 sm:p-4"
      >
        <span className="mt-0.5 flex size-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-blue-700">
          <FileText className="size-4" aria-hidden="true" />
        </span>
        <span className="min-w-0 flex-1">
          <span className="flex min-w-0 flex-col gap-1 sm:flex-row sm:items-center sm:justify-between sm:gap-3">
            <span className="break-words text-sm font-semibold text-slate-800">{source.documentName}</span>
            <span className="shrink-0 rounded-md bg-blue-50 px-2 py-1 text-xs font-semibold text-blue-700">{relevance}% relevant</span>
          </span>
          <span className="mt-1 block text-xs leading-5 text-slate-500">
            Page {source.pageNumber} <span aria-hidden="true">·</span> Chunk {source.chunkNumber}
          </span>
        </span>
        <span className="mt-1 text-slate-500">
          {isExpanded ? <ChevronUp className="size-5" aria-hidden="true" /> : <ChevronDown className="size-5" aria-hidden="true" />}
        </span>
      </button>

      {isExpanded && (
        <div id={contentId} className="border-t border-slate-100 bg-slate-50 px-3.5 py-4 sm:px-4">
          <p className="text-xs font-semibold uppercase tracking-[0.12em] text-slate-500">Retrieved content</p>
          <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-6 text-slate-700">{source.content}</p>
          <div className="mt-4 border-t border-slate-200 pt-3">
            <div className="flex items-center justify-between gap-3 text-xs">
              <span className="font-medium text-slate-600">Relevance</span>
              <span className="font-semibold text-slate-700">{relevance}%</span>
            </div>
            <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-200" role="progressbar" aria-label={`Relevance for ${source.documentName}`} aria-valuemin={0} aria-valuemax={100} aria-valuenow={relevance}>
              <div className="h-full rounded-full bg-blue-600" style={{ width: `${relevance}%` }} />
            </div>
          </div>
        </div>
      )}
    </article>
  )
}
