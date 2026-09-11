import type { RagRequest, RagResponse, RagSource } from '../types/rag'
import api from './api'

function isRagSource(value: unknown): value is RagSource {
  if (typeof value !== 'object' || value === null) return false
  const source = value as Record<string, unknown>
  return typeof source.documentId === 'number' && typeof source.documentName === 'string' &&
    typeof source.pageNumber === 'number' && typeof source.chunkNumber === 'number' &&
    typeof source.similarity === 'number' && typeof source.content === 'string'
}

function isRagResponse(value: unknown): value is RagResponse {
  if (typeof value !== 'object' || value === null) return false
  const response = value as Record<string, unknown>
  return typeof response.question === 'string' && typeof response.answer === 'string' &&
    response.answer.trim().length > 0 && typeof response.citationValid === 'boolean' &&
    Array.isArray(response.sources) && response.sources.every(isRagSource)
}

export class UnexpectedRagResponseError extends Error {
  constructor(message = 'The AI assistant returned an unexpected response.', cause?: unknown) {
    super(message, cause === undefined ? undefined : { cause })
    this.name = 'UnexpectedRagResponseError'
  }
}

export async function askRagQuestion(question: string, topK = 5): Promise<RagResponse> {
  const request: RagRequest = { question, topK }
  const response = await api.post<unknown>('/api/rag/ask', request)
  if (!isRagResponse(response.data)) throw new UnexpectedRagResponseError()
  return response.data
}
