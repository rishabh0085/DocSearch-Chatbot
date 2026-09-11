package com.enterprise.search.service;

import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Validates semantic retrieval relevance before a document question can reach the LLM.
 * The repository returns similarity as {@code 1 - cosine distance}; the highest ranked
 * chunk must meet the configured threshold.
 */
@Service
public class RagAnswerabilityGuard {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagAnswerabilityGuard.class);
    private final double minimumSimilarity;

    @Autowired
    public RagAnswerabilityGuard(@Value("${rag.relevance.min-similarity:0.65}") double minimumSimilarity) {
        if (minimumSimilarity < -1 || minimumSimilarity > 1) {
            throw new IllegalArgumentException("RAG relevance minimum similarity must be between -1 and 1");
        }
        this.minimumSimilarity = minimumSimilarity;
    }

    public AnswerabilityDecision evaluate(List<DocumentChunkSimilarityProjection> searchResults) {
        if (searchResults == null) {
            throw new IllegalArgumentException("Search results cannot be null");
        }

        double highestSimilarity = searchResults.stream()
                .map(DocumentChunkSimilarityProjection::getSimilarity)
                .filter(similarity -> similarity != null && Double.isFinite(similarity))
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(Double.NaN);
        boolean relevant = Double.isFinite(highestSimilarity) && highestSimilarity >= minimumSimilarity;
        LOGGER.info("[RAG] relevance.checked highestSimilarity={} minimumSimilarity={} relevant={}",
                highestSimilarity, minimumSimilarity, relevant);
        return new AnswerabilityDecision(relevant, highestSimilarity);
    }

    public record AnswerabilityDecision(boolean answerable, double highestSimilarity) {
    }
}
