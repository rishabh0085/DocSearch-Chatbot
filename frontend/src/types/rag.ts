export type RagRequest = { question: string; topK: number }

export type RagSource = {
  documentId: number
  documentName: string
  pageNumber: number
  chunkNumber: number
  similarity: number
  content: string
}

export type RagResponse = {
  question: string
  answer: string
  citationValid: boolean
  sources: RagSource[]
}

export type ChatMessage = {
  id: string
  role: 'user' | 'assistant'
  content: string
  sources?: RagSource[]
  citationValid?: boolean
}
