import type { SearchRequest, SearchResponse, SearchResultResponse } from '../types/search'
import api from './api'

function isSearchResultResponse(value: unknown): value is SearchResultResponse {
  if (typeof value !== 'object' || value === null) return false
  const result = value as Record<string, unknown>
  return typeof result.chunkId === 'number' && Number.isFinite(result.chunkId) &&
    typeof result.documentId === 'number' && Number.isFinite(result.documentId) &&
    typeof result.documentName === 'string' && typeof result.pageNumber === 'number' && Number.isFinite(result.pageNumber) &&
    typeof result.chunkNumber === 'number' && Number.isFinite(result.chunkNumber) && typeof result.content === 'string' &&
    typeof result.similarity === 'number' && Number.isFinite(result.similarity) &&
    typeof result.cosineDistance === 'number' && Number.isFinite(result.cosineDistance)
}

function isSearchResponse(value: unknown): value is SearchResponse {
  if (typeof value !== 'object' || value === null) return false
  const response = value as Record<string, unknown>
  return typeof response.query === 'string' && Array.isArray(response.results) && response.results.every(isSearchResultResponse)
}

export class UnexpectedSearchResponseError extends Error {
  constructor() {
    super("Couldn't display the search results because the server returned an unexpected response.")
    this.name = 'UnexpectedSearchResponseError'
  }
}

export async function searchDocuments(query: string, topK = 5): Promise<SearchResponse> {
  const request: SearchRequest = { query, topK }
  const response = await api.post<unknown>('/api/search', request)
  if (!isSearchResponse(response.data)) throw new UnexpectedSearchResponseError()
  return response.data
}
