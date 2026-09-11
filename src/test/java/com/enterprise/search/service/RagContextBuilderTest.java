package com.enterprise.search.service;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagContextBuilderTest {

    private final RagContextBuilder builder = new RagContextBuilder();

    @Test
    void convertsRankedProjectionsToSourceAwareContextsInTheirOriginalOrder() {
        DocumentChunkSimilarityProjection first = projection(3L, 2, 3,
                "Non-tech team notice period is one month.", 0.7707);
        DocumentChunkSimilarityProjection second = projection(4L, 1, 2,
                "Employee handbook notice details.", 0.7122);

        List<RagContext> contexts = builder.build(
                List.of(first, second),
                Map.of(3L, "Notice_Period_Policy.pdf", 4L, "Employee_Handbook.pdf"));

        assertEquals(2, contexts.size());
        assertEquals(new RagContext(3L, "Notice_Period_Policy.pdf", 2, 3,
                "Non-tech team notice period is one month.", 0.7707), contexts.get(0));
        assertEquals(new RagContext(4L, "Employee_Handbook.pdf", 1, 2,
                "Employee handbook notice details.", 0.7122), contexts.get(1));
    }

    @Test
    void returnsAnEmptyContextListForEmptySearchResults() {
        List<RagContext> contexts = builder.build(List.of(), Map.of());

        assertTrue(contexts.isEmpty());
    }

    @Test
    void rejectsMissingSearchResultsOrDocumentNames() {
        assertThrows(IllegalArgumentException.class, () -> builder.build(null, Map.of()));
        assertThrows(IllegalArgumentException.class, () -> builder.build(List.of(), null));
    }

    private DocumentChunkSimilarityProjection projection(
            Long documentId,
            int pageNumber,
            int chunkNumber,
            String content,
            double similarity) {

        DocumentChunkSimilarityProjection projection = mock(DocumentChunkSimilarityProjection.class);
        when(projection.getDocumentId()).thenReturn(documentId);
        when(projection.getPageNumber()).thenReturn(pageNumber);
        when(projection.getChunkNumber()).thenReturn(chunkNumber);
        when(projection.getContent()).thenReturn(content);
        when(projection.getSimilarity()).thenReturn(similarity);
        return projection;
    }
}
