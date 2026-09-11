import { Files } from 'lucide-react'
import type { RagSource } from '../../types/rag'
import { SourceCard } from './SourceCard'

type SourceListProps = {
  sources: RagSource[]
}

export function SourceList({ sources }: SourceListProps) {
  return (
    <section className="mt-5 border-t border-slate-100 pt-4" aria-label="Supporting sources">
      <div className="flex items-center gap-2">
        <span className="flex size-7 items-center justify-center rounded-md bg-slate-100 text-slate-600">
          <Files className="size-4" aria-hidden="true" />
        </span>
        <div>
          <h3 className="text-sm font-semibold text-slate-800">Sources ({sources.length})</h3>
          <p className="text-xs text-slate-500">Supporting retrieved passages</p>
        </div>
      </div>
      {sources.length === 0 ? (
        <p className="mt-3 text-sm text-slate-500">No supporting sources were returned.</p>
      ) : (
        <ol className="mt-3 space-y-2.5">
          {sources.map((source, index) => <li key={`${source.documentId}-${source.chunkNumber}-${index}`}><SourceCard source={source} /></li>)}
        </ol>
      )}
    </section>
  )
}
