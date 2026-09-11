package com.enterprise.search.service;

import com.enterprise.search.exception.EmbeddingServiceException;
import com.enterprise.search.repository.DocumentChunkRepository;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SemanticSearchServiceTest {

    @Test
    void generatesAnEmbeddingAndReturnsRepositoryResults() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        List<Float> generatedEmbedding = embedding(0.25F, 0.5F);
        DocumentChunkSimilarityProjection result = mock(DocumentChunkSimilarityProjection.class);
        List<DocumentChunkSimilarityProjection> expectedResults = List.of(result);
        when(embeddingService.generateEmbedding("expense policy")).thenReturn(generatedEmbedding);
        when(repository.findSimilarChunks(any(float[].class), eq(3))).thenReturn(expectedResults);

        List<DocumentChunkSimilarityProjection> results = service(embeddingService, repository)
                .search("expense policy", 3);

        ArgumentCaptor<float[]> embeddingCaptor = ArgumentCaptor.forClass(float[].class);
        verify(repository).findSimilarChunks(embeddingCaptor.capture(), eq(3));
        assertArrayEquals(toArray(generatedEmbedding), embeddingCaptor.getValue());
        assertSame(expectedResults, results);
    }

    @Test
    void rejectsNullQueryWithoutGeneratingAnEmbedding() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(embeddingService, repository).search(null, 1));

        assertEquals("Search query cannot be null or blank", exception.getMessage());
        verifyNoInteractions(embeddingService, repository);
    }

    @Test
    void rejectsBlankQueryWithoutGeneratingAnEmbedding() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(embeddingService, repository).search(" \n\t", 1));

        assertEquals("Search query cannot be null or blank", exception.getMessage());
        verifyNoInteractions(embeddingService, repository);
    }

    @Test
    void rejectsZeroTopKWithoutGeneratingAnEmbedding() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(embeddingService, repository).search("policy", 0));

        assertEquals("topK must be greater than 0", exception.getMessage());
        verifyNoInteractions(embeddingService, repository);
    }

    @Test
    void rejectsNegativeTopKWithoutGeneratingAnEmbedding() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(embeddingService, repository).search("policy", -1));

        assertEquals("topK must be greater than 0", exception.getMessage());
        verifyNoInteractions(embeddingService, repository);
    }

    @Test
    void rejectsTopKAboveTheMaximum() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(embeddingService, repository).search("policy", 51));

        assertEquals("topK must not be greater than 50", exception.getMessage());
        verifyNoInteractions(embeddingService, repository);
    }

    @Test
    void rejectsAnInvalidEmbeddingDimensionBeforeCallingTheRepository() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        when(embeddingService.generateEmbedding("policy")).thenReturn(embedding(1.0F).subList(0, 767));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service(embeddingService, repository).search("policy", 5));

        assertEquals("Expected embedding dimension 768 but received 767", exception.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void returnsAnEmptyResultListWhenNoChunksMatch() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        when(embeddingService.generateEmbedding("policy")).thenReturn(embedding(1.0F));
        when(repository.findSimilarChunks(any(float[].class), eq(5))).thenReturn(List.of());

        List<DocumentChunkSimilarityProjection> results = service(embeddingService, repository).search("policy", 5);

        assertTrue(results.isEmpty());
    }

    @Test
    void propagatesEmbeddingServiceFailureWithoutCallingTheRepository() {
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        EmbeddingServiceException failure = new EmbeddingServiceException("Ollama is unavailable");
        when(embeddingService.generateEmbedding("policy")).thenThrow(failure);

        EmbeddingServiceException thrown = assertThrows(EmbeddingServiceException.class,
                () -> service(embeddingService, repository).search("policy", 5));

        assertSame(failure, thrown);
        verifyNoInteractions(repository);
    }

    private SemanticSearchService service(
            EmbeddingService embeddingService,
            DocumentChunkRepository repository) {
        return new SemanticSearchService(embeddingService, repository);
    }

    private List<Float> embedding(float first, float remaining) {
        return IntStream.range(0, 768)
                .mapToObj(index -> index == 0 ? first : remaining)
                .toList();
    }

    private List<Float> embedding(float value) {
        return embedding(value, value);
    }

    private float[] toArray(List<Float> embedding) {
        float[] values = new float[embedding.size()];
        for (int index = 0; index < embedding.size(); index++) {
            values[index] = embedding.get(index);
        }
        return values;
    }
}
