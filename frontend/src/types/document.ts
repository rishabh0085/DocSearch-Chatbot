export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED'

export type DocumentResponse = {
  id: number
  fileName: string
  fileType: string
  fileSize: number | null
  pageCount?: number | null
  status: DocumentStatus | null
  createdAt: string | null
  updatedAt: string | null
}

export type DocumentSummarySource = { pageNumber: number; chunkNumber: number }
export type DocumentSummaryResponse = { documentId: number; documentName: string; summary: string; keyPoints: string[]; importantRules: string[]; sources: DocumentSummarySource[] }
