package com.enterprise.search.repository;

import java.time.LocalDateTime;

/**
 * A document chunk and its pgvector cosine-distance search scores.
 */
public interface DocumentChunkSimilarityProjection {

    Long getChunkId();

    Long getDocumentId();

    Integer getPageNumber();

    Integer getChunkNumber();

    String getContent();

    LocalDateTime getCreatedAt();

    Double getCosineDistance();

    Double getSimilarity();
}
