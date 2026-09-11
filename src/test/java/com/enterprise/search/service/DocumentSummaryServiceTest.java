package com.enterprise.search.service;

import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.entity.Document;
import com.enterprise.search.entity.DocumentChunk;
import com.enterprise.search.repository.DocumentChunkRepository;
import com.enterprise.search.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentSummaryServiceTest {
    @Test
    void summarizesAllChunksForOnlyTheSelectedReadyDocumentInOrder() {
        DocumentRepository documents = mock(DocumentRepository.class); DocumentChunkRepository chunks = mock(DocumentChunkRepository.class); LlmService llm = mock(LlmService.class);
        Document document = document(7L, DocumentStatus.READY);
        when(documents.findById(7L)).thenReturn(Optional.of(document));
        when(chunks.findByDocumentIdOrderByChunkNumberAsc(7L)).thenReturn(List.of(chunk(document, 2, 2, "Second policy section."), chunk(document, 1, 1, "First policy section.")));
        when(llm.generate(anyString())).thenReturn("Summary:\nA complete policy.\nKey Points:\n- First point\nImportant Rules:\n- Get approval");

        var response = new DocumentSummaryService(documents, chunks, llm).summarize(7L);

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class); verify(llm).generate(prompt.capture());
        assertTrue(prompt.getValue().indexOf("First policy section.") < prompt.getValue().indexOf("Second policy section."));
        assertEquals(7L, response.documentId()); assertEquals(List.of("First point"), response.keyPoints());
        assertEquals(List.of(1, 2), response.sources().stream().map(source -> source.pageNumber()).toList());
        verify(chunks).findByDocumentIdOrderByChunkNumberAsc(7L); verify(chunks, never()).findByDocumentIdOrderByChunkNumberAsc(8L);
    }
    @Test void rejectsMissingDocument() { DocumentRepository documents = mock(DocumentRepository.class); when(documents.findById(9L)).thenReturn(Optional.empty()); assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> new DocumentSummaryService(documents, mock(DocumentChunkRepository.class), mock(LlmService.class)).summarize(9L)); }
    @Test void rejectsDocumentThatIsNotReady() { DocumentRepository documents = mock(DocumentRepository.class); when(documents.findById(7L)).thenReturn(Optional.of(document(7L, DocumentStatus.PROCESSING))); assertThrows(IllegalStateException.class, () -> new DocumentSummaryService(documents, mock(DocumentChunkRepository.class), mock(LlmService.class)).summarize(7L)); }
    @Test void rejectsDocumentWithNoChunks() { DocumentRepository documents = mock(DocumentRepository.class); DocumentChunkRepository chunks = mock(DocumentChunkRepository.class); when(documents.findById(7L)).thenReturn(Optional.of(document(7L, DocumentStatus.READY))); when(chunks.findByDocumentIdOrderByChunkNumberAsc(7L)).thenReturn(List.of()); assertThrows(IllegalStateException.class, () -> new DocumentSummaryService(documents, chunks, mock(LlmService.class)).summarize(7L)); }
    private Document document(Long id, DocumentStatus status) { Document document = new Document(); document.setId(id); document.setFileName("Leave-and-Holiday-Policy.pdf"); document.setStatus(status); return document; }
    private DocumentChunk chunk(Document document, int page, int number, String content) { DocumentChunk chunk = new DocumentChunk(); chunk.setDocument(document); chunk.setPageNumber(page); chunk.setChunkNumber(number); chunk.setContent(content); return chunk; }
}
