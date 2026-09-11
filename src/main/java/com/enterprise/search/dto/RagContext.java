package com.enterprise.search.dto;

/**
 * Retrieved document content and source metadata prepared for a RAG prompt.
 */
public record RagContext(
        Long documentId,
        String documentName,
        Integer pageNumber,
        Integer chunkNumber,
        String content,
        Double similarity) {
}
