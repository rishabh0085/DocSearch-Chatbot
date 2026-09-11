package com.enterprise.search.service;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Maps ranked semantic-search projections to source-aware RAG context records.
 * Document names are supplied by the caller after a bulk document lookup.
 */
@Service
public class RagContextBuilder {

    public List<RagContext> build(
            List<DocumentChunkSimilarityProjection> searchResults,
            Map<Long, String> documentNames) {

        if (searchResults == null) {
            throw new IllegalArgumentException("Search results cannot be null");
        }
        if (documentNames == null) {
            throw new IllegalArgumentException("Document names cannot be null");
        }

        return searchResults.stream()
                .map(result -> new RagContext(
                        result.getDocumentId(),
                        documentNames.get(result.getDocumentId()),
                        result.getPageNumber(),
                        result.getChunkNumber(),
                        result.getContent(),
                        result.getSimilarity()))
                .toList();
    }
}
