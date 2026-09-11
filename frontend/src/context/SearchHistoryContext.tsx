import { createContext, useContext, useMemo, useState } from 'react'
import type { PropsWithChildren } from 'react'
import type { SearchHistoryItem } from '../types/searchHistory'

type SearchHistoryContextValue = {
  history: SearchHistoryItem[]
  addSearch: (query: string, resultCount: number) => void
  clearHistory: () => void
}

const SearchHistoryContext = createContext<SearchHistoryContextValue | null>(null)
const HISTORY_LIMIT = 20

function createHistoryId() {
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export function SearchHistoryProvider({ children }: PropsWithChildren) {
  const [history, setHistory] = useState<SearchHistoryItem[]>([])

  const value = useMemo<SearchHistoryContextValue>(() => ({
    history,
    addSearch(query, resultCount) {
      const timestamp = new Date().toISOString()
      setHistory((current) => {
        const latest = current[0]
        if (latest?.query === query) {
          return [{ ...latest, timestamp, resultCount }, ...current.slice(1)]
        }
        return [{ id: createHistoryId(), query, timestamp, resultCount }, ...current].slice(0, HISTORY_LIMIT)
      })
    },
    clearHistory() {
      setHistory([])
    },
  }), [history])

  return <SearchHistoryContext.Provider value={value}>{children}</SearchHistoryContext.Provider>
}

export function useSearchHistory() {
  const context = useContext(SearchHistoryContext)
  if (!context) throw new Error('useSearchHistory must be used within SearchHistoryProvider')
  return context
}
