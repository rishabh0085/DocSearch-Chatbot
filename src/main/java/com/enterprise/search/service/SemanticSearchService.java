package com.enterprise.search.service;

import com.enterprise.search.repository.DocumentChunkRepository;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Coordinates query embedding generation with pgvector chunk similarity search.
 */
@Service
public class SemanticSearchService {

    private static final int EMBEDDING_DIMENSION = 768;
    private static final int MAX_TOP_K = 50;
    private static final Logger LOGGER = LoggerFactory.getLogger(SemanticSearchService.class);

    private final EmbeddingService embeddingService;
    private final DocumentChunkRepository documentChunkRepository;

    public SemanticSearchService(
            EmbeddingService embeddingService,
            DocumentChunkRepository documentChunkRepository) {

        this.embeddingService = embeddingService;
        this.documentChunkRepository = documentChunkRepository;
    }

    public List<DocumentChunkSimilarityProjection> search(String query, int topK) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Search query cannot be null or blank");
        }
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be greater than 0");
        }
        if (topK > MAX_TOP_K) {
            throw new IllegalArgumentException("topK must not be greater than " + MAX_TOP_K);
        }

        long embeddingStart = System.nanoTime();
        List<Float> embedding;
        try {
            embedding = embeddingService.generateEmbedding(query);
        } finally {
            LOGGER.info("[RAG-PERF] embedding.durationMs={}", elapsedMillis(embeddingStart));
        }

        long vectorSearchStart = System.nanoTime();
        List<DocumentChunkSimilarityProjection> results = null;
        try {
            results = documentChunkRepository.findSimilarChunks(toFloatArray(embedding), topK);
            return results;
        } finally {
            LOGGER.info("[RAG-PERF] vectorSearch.durationMs={} resultCount={}",
                    elapsedMillis(vectorSearchStart), results == null ? "unavailable" : results.size());
        }
    }

    private float[] toFloatArray(List<Float> embedding) {
        if (embedding == null) {
            throw new IllegalStateException("Expected embedding dimension 768 but received null");
        }
        if (embedding.size() != EMBEDDING_DIMENSION) {
            throw new IllegalStateException(
                    "Expected embedding dimension 768 but received " + embedding.size());
        }

        float[] values = new float[EMBEDDING_DIMENSION];
        for (int index = 0; index < EMBEDDING_DIMENSION; index++) {
            Float value = embedding.get(index);
            if (value == null) {
                throw new IllegalStateException("Search query embedding contained a null value");
            }
            values[index] = value;
        }
        return values;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
