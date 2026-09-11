package com.enterprise.search.service;

import com.enterprise.search.dto.PageText;
import com.enterprise.search.entity.Document;
import com.enterprise.search.entity.DocumentChunk;
import com.enterprise.search.repository.DocumentChunkRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocumentChunkServiceTest {

    private final DocumentChunkService chunkService = new DocumentChunkService(null, null, null, 20, 5);

    @Test
    void smallTextCreatesOneChunk() {
        List<DocumentChunk> chunks = chunkService.createChunks(document(), List.of(new PageText(1, "Short text")));

        assertEquals(1, chunks.size());
        assertEquals("Short text", chunks.getFirst().getContent());
    }

    @Test
    void largeTextCreatesMultipleChunksWithoutBreakingWords() {
        List<DocumentChunk> chunks = chunkService.createChunks(document(),
                List.of(new PageText(1, "alpha bravo charlie delta echo foxtrot")));

        assertTrue(chunks.size() > 1);
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getContent().length() <= 20));
        assertTrue(chunks.stream().noneMatch(chunk -> chunk.getContent().startsWith("ravo")));
    }

    @Test
    void preservesPageNumbersAndUsesSequentialChunkNumbersAcrossPages() {
        List<DocumentChunk> chunks = chunkService.createChunks(document(), List.of(
                new PageText(1, "alpha bravo charlie delta echo"),
                new PageText(2, "foxtrot golf hotel india juliet")));

        assertEquals(List.of(1, 1, 2, 2), chunks.stream().map(DocumentChunk::getPageNumber).toList());
        assertEquals(List.of(1, 2, 3, 4), chunks.stream().map(DocumentChunk::getChunkNumber).toList());
    }

    @Test
    void overlapsChunksUsingConfiguredOverlap() {
        List<DocumentChunk> chunks = chunkService.createChunks(document(),
                List.of(new PageText(1, "alpha bravo charlie delta echo foxtrot")));

        assertTrue(chunks.get(1).getContent().startsWith("charlie"));
        assertTrue(chunks.getFirst().getContent().endsWith("charlie"));
    }

    @Test
    void emptyPageTextDoesNotCreateChunks() {
        List<DocumentChunk> chunks = chunkService.createChunks(document(), List.of(
                new PageText(1, "   \n\t"),
                new PageText(2, "usable text")));

        assertEquals(1, chunks.size());
        assertEquals(2, chunks.getFirst().getPageNumber());
        assertFalse(chunks.getFirst().getContent().isBlank());
    }

    @Test
    void recordsThePdfPageCountWhileReusingExtractedPageText() {
        DocumentChunkRepository repository = mock(DocumentChunkRepository.class);
        PdfTextExtractionService extractionService = mock(PdfTextExtractionService.class);
        Document document = document();
        document.setFileType("application/pdf");
        document.setFilePath("uploads/policy.pdf");
        when(extractionService.extractPageText(java.nio.file.Path.of("uploads/policy.pdf")))
                .thenReturn(List.of(new PageText(1, "First page"), new PageText(2, "Second page")));

        new DocumentChunkService(repository, extractionService, null, 20, 5).processDocument(document);

        assertEquals(2, document.getPageCount());
    }

    private Document document() {
        Document document = new Document();
        document.setId(1L);
        return document;
    }
}
