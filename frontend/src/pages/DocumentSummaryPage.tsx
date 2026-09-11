import { ArrowLeft, FileText, LoaderCircle, MessageSquare, ShieldCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { TextToSpeechButton } from '../components/chat/TextToSpeechButton'
import { summarizeDocument } from '../services/documentService'
import type { DocumentSummaryResponse } from '../types/document'

function summaryTextForSpeech(summary: DocumentSummaryResponse) {
  const sections = [
    summary.summary.trim() && `Overview.\n${summary.summary}`,
    summary.keyPoints.length > 0 && `Key Points.\n${summary.keyPoints.join('\n')}`,
    summary.importantRules.length > 0 && `Important Rules.\n${summary.importantRules.join('\n')}`,
  ]

  return sections.filter(Boolean).join('\n\n')
}

export function DocumentSummaryPage() {
  const { id } = useParams()
  const [summary, setSummary] = useState<DocumentSummaryResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const documentId = Number(id)
    if (!Number.isInteger(documentId)) {
      setError('This document could not be found.')
      return
    }

    let active = true
    summarizeDocument(documentId)
      .then((result) => { if (active) setSummary(result) })
      .catch((requestError: unknown) => {
        if (active) {
          setError((requestError as { response?: { data?: { message?: string } } })?.response?.data?.message
            ?? "We couldn't create a summary for this document. Please try again.")
        }
      })
    return () => { active = false }
  }, [id])

  return (
    <section className="mx-auto w-full max-w-4xl px-4 py-7 sm:px-6 sm:py-9 lg:px-10">
      <Link to="/documents" className="inline-flex items-center gap-2 text-sm font-semibold text-orange-700 hover:text-orange-800">
        <ArrowLeft className="size-4" />Back to Documents
      </Link>

      {!summary && !error && <div className="mt-10 flex min-h-64 flex-col items-center justify-center rounded-2xl border border-slate-200 bg-white text-center shadow-sm">
        <LoaderCircle className="size-7 animate-spin text-orange-600 motion-reduce:animate-none" />
        <h1 className="mt-4 text-lg font-semibold text-slate-900">Creating your document summary...</h1>
        <p className="mt-2 text-sm text-slate-500">Analyzing the complete indexed document.</p>
      </div>}

      {error && <div className="mt-8 rounded-2xl border border-rose-200 bg-rose-50 p-5 text-rose-800" role="alert">
        <h1 className="font-semibold">Summary unavailable</h1>
        <p className="mt-2 text-sm">{error}</p>
        <Link to="/documents" className="mt-4 inline-flex text-sm font-semibold underline">Back to Documents</Link>
      </div>}

      {summary && <>
        <header className="mt-7 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
            <div className="flex gap-4">
              <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-orange-50 text-orange-700"><FileText className="size-5" /></span>
              <div>
                <p className="text-sm font-semibold text-orange-600">AI Summary</p>
                <h1 className="mt-1 break-words text-2xl font-semibold tracking-tight text-slate-900">{summary.documentName}</h1>
                <p className="mt-2 text-sm text-slate-500">Simple explanation of this document&apos;s complete indexed content.</p>
              </div>
            </div>
            <TextToSpeechButton
              text={summaryTextForSpeech(summary)}
              idleLabel="Listen to Summary"
              speakingLabel="Speaking"
              idleAriaLabel="Listen to summary"
              speakingAriaLabel="Stop reading summary"
              className="mt-0 shrink-0"
            />
          </div>
        </header>

        <article className="mt-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-lg font-semibold text-slate-900">Overview</h2>
          <p className="mt-3 whitespace-pre-wrap text-sm leading-7 text-slate-700">{summary.summary}</p>
        </article>
        <SummarySection title="Key Points" items={summary.keyPoints} />
        <SummarySection title="Important Rules" items={summary.importantRules} />

        <article className="mt-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center gap-2"><ShieldCheck className="size-5 text-orange-600" /><h2 className="text-lg font-semibold text-slate-900">Sources</h2></div>
          <p className="mt-2 text-sm text-slate-500">Pages represented by the indexed content used for this summary.</p>
          <div className="mt-4 flex flex-wrap gap-2">
            {[...new Set(summary.sources.map((source) => source.pageNumber))].map((page) => <span key={page} className="rounded-full bg-orange-50 px-3 py-1.5 text-xs font-semibold text-orange-800">Page {page}</span>)}
          </div>
        </article>

        <div className="mt-6 flex flex-wrap gap-3">
          <Link to="/chat" className="inline-flex items-center gap-2 rounded-xl bg-orange-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-orange-700"><MessageSquare className="size-4" />Ask in Chat</Link>
          <Link to={`/documents/${summary.documentId}/view`} className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:bg-slate-50">View Document</Link>
        </div>
      </>}
    </section>
  )
}

function SummarySection({ title, items }: { title: string; items: string[] }) {
  if (!items.length) return null
  return <article className="mt-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm">
    <h2 className="text-lg font-semibold text-slate-900">{title}</h2>
    <ul className="mt-4 space-y-3 text-sm leading-6 text-slate-700">
      {items.map((item, index) => <li key={`${item}-${index}`} className="flex gap-3"><span className="mt-2 size-1.5 shrink-0 rounded-full bg-orange-600" />{item}</li>)}
    </ul>
  </article>
}
