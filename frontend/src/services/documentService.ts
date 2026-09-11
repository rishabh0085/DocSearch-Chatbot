import type { DocumentResponse, DocumentStatus, DocumentSummaryResponse } from '../types/document'
import api from './api'

const documentStatuses: DocumentStatus[] = ['UPLOADED', 'PROCESSING', 'READY', 'FAILED']

function isDocumentStatus(value: unknown): value is DocumentStatus | null {
  return value === null || (typeof value === 'string' && documentStatuses.includes(value as DocumentStatus))
}

function isDocumentResponse(value: unknown): value is DocumentResponse {
  if (typeof value !== 'object' || value === null) return false
  const document = value as Record<string, unknown>
  return typeof document.id === 'number' && typeof document.fileName === 'string' &&
    typeof document.fileType === 'string' && (typeof document.fileSize === 'number' || document.fileSize === null) &&
    (typeof document.pageCount === 'number' || document.pageCount === null || document.pageCount === undefined) &&
    isDocumentStatus(document.status) && (typeof document.createdAt === 'string' || document.createdAt === null) &&
    (typeof document.updatedAt === 'string' || document.updatedAt === null)
}

export class UnexpectedDocumentResponseError extends Error {
  constructor() {
    super("Couldn't load your documents because the server returned an unexpected response.")
    this.name = 'UnexpectedDocumentResponseError'
  }
}

export async function getDocuments(): Promise<DocumentResponse[]> {
  const response = await api.get<unknown>('/api/documents')
  if (!Array.isArray(response.data) || !response.data.every(isDocumentResponse)) throw new UnexpectedDocumentResponseError()
  return response.data
}

export async function uploadDocument(file: File): Promise<DocumentResponse> {
  const formData = new FormData()
  formData.append('file', file)
  const response = await api.post<unknown>('/api/documents', formData, { headers: { 'Content-Type': undefined } })
  if (!isDocumentResponse(response.data)) throw new UnexpectedDocumentResponseError()
  return response.data
}

export async function deleteDocument(id: number): Promise<void> {
  await api.delete(`/api/documents/${id}`)
}

export async function getDocument(id: number): Promise<DocumentResponse> {
  const response = await api.get<unknown>(`/api/documents/${id}`)
  if (!isDocumentResponse(response.data)) throw new UnexpectedDocumentResponseError()
  return response.data
}

export async function summarizeDocument(id: number): Promise<DocumentSummaryResponse> {
  const response = await api.post<unknown>(`/api/documents/${id}/summary`)
  const value = response.data as Partial<DocumentSummaryResponse>
  if (!value || typeof value.documentId !== 'number' || typeof value.documentName !== 'string' || typeof value.summary !== 'string' || !Array.isArray(value.keyPoints) || !Array.isArray(value.importantRules) || !Array.isArray(value.sources)) throw new UnexpectedDocumentResponseError()
  return value as DocumentSummaryResponse
}

export function getDocumentContentUrl(id: number, download = false) {
  return api.getUri({ url: `/api/documents/${id}/content`, params: download ? { download: true } : undefined })
}
