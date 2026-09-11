import { LoaderCircle, Search, Sparkles, X } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { SearchResultCard } from '../components/search/SearchResultCard'
import { useSearchHistory } from '../context/SearchHistoryContext'
import { searchDocuments, UnexpectedSearchResponseError } from '../services/searchService'
import type { SearchResponse } from '../types/search'

const TOP_K = 5
const suggestions = ['What is the notice period for resignation?', 'What is the notice period for the technical team?']

export function SearchPage() {
  const [searchParams] = useSearchParams()
  const [query, setQuery] = useState('')
  const [response, setResponse] = useState<SearchResponse | null>(null)
  const [isSearching, setIsSearching] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const requestInFlight = useRef(false)
  const { addSearch } = useSearchHistory()

  useEffect(() => { setQuery(searchParams.get('q') ?? '') }, [searchParams])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const trimmedQuery = query.trim()
    if (!trimmedQuery) { setError('Enter a question or search term.'); return }
    if (requestInFlight.current) return
    requestInFlight.current = true
    setQuery(trimmedQuery)
    setError(null)
    setResponse(null)
    setIsSearching(true)
    try {
      const searchResponse = await searchDocuments(trimmedQuery, TOP_K)
      setResponse(searchResponse)
      addSearch(trimmedQuery, searchResponse.results.length)
    } catch (requestError) {
      console.error('Search request failed', requestError)
      setError(requestError instanceof UnexpectedSearchResponseError ? requestError.message : "Couldn't complete the search. Please try again.")
    } finally {
      requestInFlight.current = false
      setIsSearching(false)
    }
  }

  function clearSearch() { setQuery(''); setResponse(null); setError(null) }

  const hasSearched = response !== null
  return <section className="mx-auto w-full max-w-5xl px-4 py-6 sm:px-6 sm:py-8 lg:px-10">
    <p className="text-sm font-semibold text-blue-600">Knowledge base</p><h1 className="mt-1 text-2xl font-semibold tracking-tight text-slate-900 sm:text-3xl">Search</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Find relevant information across your organization&apos;s documents.</p>
    <form className="mt-7 rounded-xl border border-slate-200 bg-white p-4 shadow-sm shadow-slate-200/40 sm:p-5" onSubmit={(event) => void handleSubmit(event)}>
      <label htmlFor="search-query" className="sr-only">Search your knowledge base</label>
      <div className="flex flex-col gap-3 sm:flex-row"><div className="relative min-w-0 flex-1"><Search className="pointer-events-none absolute left-3 top-1/2 size-5 -translate-y-1/2 text-slate-400" aria-hidden="true" /><input id="search-query" value={query} onChange={(event) => setQuery(event.target.value)} disabled={isSearching} placeholder="Search your knowledge base..." className="w-full rounded-lg border border-slate-300 bg-white py-3 pl-10 pr-10 text-sm text-slate-900 outline-none transition-colors placeholder:text-slate-400 focus:border-blue-500 focus:ring-2 focus:ring-blue-100 disabled:cursor-not-allowed disabled:bg-slate-50" />{query && <button type="button" onClick={clearSearch} disabled={isSearching} aria-label="Clear search" className="absolute right-2 top-1/2 rounded-md p-1.5 text-slate-500 -translate-y-1/2 transition-colors hover:bg-slate-100 hover:text-slate-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 disabled:opacity-50"><X className="size-4" aria-hidden="true" /></button>}</div><button type="submit" disabled={isSearching} className="inline-flex items-center justify-center gap-2 rounded-lg bg-blue-600 px-5 py-3 text-sm font-medium text-white shadow-sm shadow-blue-600/20 transition-colors hover:bg-blue-700 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-60">{isSearching ? <><LoaderCircle className="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />Searching...</> : <><Search className="size-4" aria-hidden="true" />Search</>}</button></div>
      {error && <p className="mt-3 rounded-lg bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700" role="alert">{error}</p>}
    </form>
    {!hasSearched && !isSearching && !error && <div className="mt-8 rounded-xl border border-slate-200 bg-white p-6 shadow-sm shadow-slate-200/40 sm:p-8"><span className="flex size-11 items-center justify-center rounded-xl bg-blue-50 text-blue-700 ring-1 ring-blue-100"><Sparkles className="size-5" aria-hidden="true" /></span><h2 className="mt-5 text-lg font-semibold text-slate-900">Search your knowledge base</h2><p className="mt-2 max-w-xl text-sm leading-6 text-slate-600">Ask a question or enter keywords to find relevant information across your documents.</p><div className="mt-6"><p className="text-sm font-semibold text-slate-800">Try an example</p><div className="mt-3 flex flex-wrap gap-2">{suggestions.map((suggestion) => <button key={suggestion} type="button" onClick={() => setQuery(suggestion)} className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-left text-sm text-slate-600 transition-colors hover:border-blue-200 hover:bg-blue-50 hover:text-blue-800 focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-600 focus-visible:ring-offset-2">{suggestion}</button>)}</div></div></div>}
    {isSearching && <div className="mt-8 flex min-h-52 items-center justify-center rounded-xl border border-slate-200 bg-white shadow-sm"><p className="flex items-center gap-2 text-sm text-slate-500" role="status"><LoaderCircle className="size-4 animate-spin motion-reduce:animate-none" aria-hidden="true" />Searching...</p></div>}
    {response && <div className="mt-8"><div className="border-b border-slate-200 pb-4"><h2 className="text-lg font-semibold text-slate-900">Search results</h2><p className="mt-1 break-words text-sm text-slate-600">{response.results.length} {response.results.length === 1 ? 'result' : 'results'} for: <span className="font-medium text-slate-800">&ldquo;{response.query}&rdquo;</span></p></div>{response.results.length === 0 ? <div className="mt-5 rounded-xl border border-slate-200 bg-white p-8 text-center shadow-sm"><h3 className="text-base font-semibold text-slate-900">No relevant documents found</h3><p className="mt-2 text-sm text-slate-600">Try a different search term or question.</p></div> : <div className="mt-5 space-y-4">{response.results.map((result, index) => <SearchResultCard key={result.chunkId} result={result} index={index} />)}</div>}</div>}
  </section>
}
