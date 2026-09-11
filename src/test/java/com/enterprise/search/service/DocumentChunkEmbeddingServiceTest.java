package com.enterprise.search.service;

import com.enterprise.search.entity.DocumentChunk;
import com.enterprise.search.exception.EmbeddingServiceException;
import com.enterprise.search.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentChunkEmbeddingServiceTest {

    @Test
    void generatesAndStoresEmbeddingsForAllChunksInChunkOrder() {
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        DocumentChunk first = chunk(1, "first chunk");
        DocumentChunk second = chunk(2, "second chunk");
        when(repository.findByDocumentIdOrderByChunkNumberAsc(5L)).thenReturn(List.of(first, second));
        when(embeddingService.generateEmbedding("first chunk")).thenReturn(embedding(1.0F));
        when(embeddingService.generateEmbedding("second chunk")).thenReturn(embedding(2.0F));

        service(repository, embeddingService).generateEmbeddingsForDocument(5L);

        assertArrayEquals(vector(1.0F), first.getEmbedding());
        assertArrayEquals(vector(2.0F), second.getEmbedding());
        InOrder inOrder = inOrder(embeddingService);
        inOrder.verify(embeddingService).generateEmbedding("first chunk");
        inOrder.verify(embeddingService).generateEmbedding("second chunk");
        verify(repository).save(first);
        verify(repository).save(second);
    }

    @Test
    void skipsChunksWithExistingValidEmbeddings() {
        DocumentChunk embedded = chunk(1, "already embedded");
        embedded.setEmbedding(vector(9.0F));
        DocumentChunk pending = chunk(2, "pending");
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        when(repository.findByDocumentIdOrderByChunkNumberAsc(5L)).thenReturn(List.of(embedded, pending));
        when(embeddingService.generateEmbedding("pending")).thenReturn(embedding(2.0F));

        service(repository, embeddingService).generateEmbeddingsForDocument(5L);

        verify(embeddingService, never()).generateEmbedding("already embedded");
        verify(repository, never()).save(embedded);
        assertArrayEquals(vector(2.0F), pending.getEmbedding());
    }

    @Test
    void failsClearlyWhenDocumentHasNoChunks() {
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        when(repository.findByDocumentIdOrderByChunkNumberAsc(5L)).thenReturn(List.of());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(repository, embeddingService).generateEmbeddingsForDocument(5L));

        assertEquals("No chunks found for document 5", exception.getMessage());
    }

    @Test
    void propagatesEmbeddingServiceFailureWithoutMarkingChunkEmbedded() {
        DocumentChunk failedChunk = chunk(1, "cannot embed");
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        EmbeddingServiceException failure = new EmbeddingServiceException("Ollama is unavailable");
        when(repository.findByDocumentIdOrderByChunkNumberAsc(5L)).thenReturn(List.of(failedChunk));
        when(embeddingService.generateEmbedding("cannot embed")).thenThrow(failure);

        EmbeddingServiceException thrown = assertThrows(EmbeddingServiceException.class,
                () -> service(repository, embeddingService).generateEmbeddingsForDocument(5L));

        assertEquals(failure, thrown);
        assertNull(failedChunk.getEmbedding());
        verify(repository, never()).save(failedChunk);
    }

    @Test
    void isIdempotentAfterChunksAreEmbedded() {
        DocumentChunk chunk = chunk(1, "only once");
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        EmbeddingService embeddingService = mock(EmbeddingService.class);
        when(repository.findByDocumentIdOrderByChunkNumberAsc(5L)).thenReturn(List.of(chunk));
        when(embeddingService.generateEmbedding("only once")).thenReturn(embedding(3.0F));
        DocumentChunkService service = service(repository, embeddingService);

        service.generateEmbeddingsForDocument(5L);
        service.generateEmbeddingsForDocument(5L);

        verify(embeddingService).generateEmbedding("only once");
        verify(repository).save(chunk);
    }

    private DocumentChunkService service(
            DocumentChunkRepository repository,
            EmbeddingService embeddingService) {
        return new DocumentChunkService(repository, null, embeddingService, 1000, 100);
    }

    private DocumentChunk chunk(int chunkNumber, String content) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkNumber(chunkNumber);
        chunk.setContent(content);
        return chunk;
    }

    private List<Float> embedding(float value) {
        return IntStream.range(0, 768).mapToObj(index -> value).toList();
    }

    private float[] vector(float value) {
        float[] vector = new float[768];
        java.util.Arrays.fill(vector, value);
        return vector;
    }
}
