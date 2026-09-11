package com.enterprise.search.service;

import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagAnswerabilityGuardTest {

    private final RagAnswerabilityGuard guard = new RagAnswerabilityGuard(0.65);

    @Test
    void acceptsRelevantHighSimilarityResults() {
        assertTrue(guard.evaluate(List.of(chunk(0.77))).answerable());
    }

    @Test
    void rejectsClearlyIrrelevantResults() {
        assertFalse(guard.evaluate(List.of(chunk(0.40), chunk(0.33))).answerable());
    }

    @Test
    void acceptsTheBoundarySimilarityDeterministically() {
        assertTrue(guard.evaluate(List.of(chunk(0.65))).answerable());
        assertFalse(guard.evaluate(List.of(chunk(0.6499))).answerable());
    }

    @Test
    void rejectsEmptyResultsSafely() {
        assertFalse(guard.evaluate(List.of()).answerable());
    }

    @Test
    void acceptsEvidenceFromMultipleDocumentsWithoutRequiringOneDocument() {
        DocumentChunkSimilarityProjection first = chunk(0.69);
        DocumentChunkSimilarityProjection second = chunk(0.72);
        when(first.getDocumentId()).thenReturn(1L);
        when(second.getDocumentId()).thenReturn(2L);

        assertTrue(guard.evaluate(List.of(first, second)).answerable());
    }

    private DocumentChunkSimilarityProjection chunk(double similarity) {
        DocumentChunkSimilarityProjection result = mock(DocumentChunkSimilarityProjection.class);
        when(result.getSimilarity()).thenReturn(similarity);
        return result;
    }
}
