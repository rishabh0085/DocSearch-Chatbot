import { FileUp } from 'lucide-react'
import { Link } from 'react-router-dom'
import type { DocumentResponse } from '../../types/document'
import { DocumentCard } from './DocumentCard'
type Props = { documents: DocumentResponse[]; deletingDocumentId: number | null; onDelete: (id: number) => Promise<boolean>; isFiltered?: boolean }
export function DocumentList({ documents, deletingDocumentId, onDelete, isFiltered }: Props) {
  if (documents.length === 0) {
    if (isFiltered) return <div className="rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center"><h2 className="text-base font-semibold text-slate-900">No matching documents</h2><p className="mt-2 text-sm text-slate-500">Try changing your search or filter.</p></div>
    return <div className="rounded-xl border border-dashed border-[#F3D8C8] bg-white px-6 py-12 text-center shadow-sm shadow-orange-100/30"><span className="mx-auto flex size-11 items-center justify-center rounded-xl bg-orange-50 text-orange-600 ring-1 ring-orange-100"><FileUp className="size-5" aria-hidden="true" /></span><h2 className="mt-4 text-lg font-semibold text-slate-900">No documents yet</h2><p className="mx-auto mt-2 max-w-md text-sm leading-6 text-slate-600">Upload your organization&apos;s documents to make them searchable and available to the AI assistant.</p><Link to="/upload" className="mt-5 inline-flex rounded-lg bg-orange-600 px-4 py-2 text-sm font-medium text-white shadow-sm shadow-orange-600/20 transition-colors hover:bg-orange-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-orange-600 focus-visible:ring-offset-2">Upload document</Link></div>
  }
  return <div className="space-y-3">{documents.map((document) => <DocumentCard key={document.id} document={document} isDeleting={deletingDocumentId === document.id} onDelete={onDelete} />)}</div>
}
