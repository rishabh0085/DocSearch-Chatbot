export type SearchRequest = {
  query: string
  topK: number
}

export type SearchResultResponse = {
  chunkId: number
  documentId: number
  documentName: string
  pageNumber: number
  chunkNumber: number
  content: string
  similarity: number
  cosineDistance: number
}

export type SearchResponse = {
  query: string
  results: SearchResultResponse[]
}
