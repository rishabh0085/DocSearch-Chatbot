package com.enterprise.search.service;

import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.entity.Document;
import com.enterprise.search.exception.DocumentProcessingException;
import com.enterprise.search.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentProcessingServiceTest {

    @Test
    void processesDocumentFromUploadedToReady() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkService documentChunkService = mock(DocumentChunkService.class);
        Document document = uploadedDocument(7L);
        List<DocumentStatus> savedStatuses = captureSavedStatuses(documentRepository);
        when(documentRepository.findById(7L)).thenReturn(Optional.of(document));

        service(documentRepository, documentChunkService).processDocument(7L);

        assertEquals(List.of(DocumentStatus.PROCESSING, DocumentStatus.READY), savedStatuses);
        assertEquals(DocumentStatus.READY, document.getStatus());
        assertNotNull(document.getUpdatedAt());
        InOrder inOrder = inOrder(documentChunkService);
        inOrder.verify(documentChunkService).processDocument(document);
        inOrder.verify(documentChunkService).generateEmbeddingsForDocument(7L);
        verify(documentRepository, times(2)).save(document);
    }

    @Test
    void marksDocumentFailedAndPropagatesProcessingFailure() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkService documentChunkService = mock(DocumentChunkService.class);
        Document document = uploadedDocument(7L);
        List<DocumentStatus> savedStatuses = captureSavedStatuses(documentRepository);
        RuntimeException failure = new IllegalStateException("PDF could not be read");
        when(documentRepository.findById(7L)).thenReturn(Optional.of(document));
        doThrow(failure).when(documentChunkService).processDocument(document);

        DocumentProcessingException exception = assertThrows(DocumentProcessingException.class,
                () -> service(documentRepository, documentChunkService).processDocument(7L));

        assertEquals("Document processing failed for document 7: PDF could not be read", exception.getMessage());
        assertEquals(failure, exception.getCause());
        assertEquals(List.of(DocumentStatus.PROCESSING, DocumentStatus.FAILED), savedStatuses);
        assertEquals(DocumentStatus.FAILED, document.getStatus());
        verify(documentChunkService, never()).generateEmbeddingsForDocument(7L);
    }

    @Test
    void rejectsAMissingDocumentClearly() {
        DocumentRepository documentRepository = mock(DocumentRepository.class);
        DocumentChunkService documentChunkService = mock(DocumentChunkService.class);
        when(documentRepository.findById(7L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service(documentRepository, documentChunkService).processDocument(7L));

        assertEquals("Document not found with ID 7", exception.getMessage());
    }

    private DocumentProcessingService service(
            DocumentRepository documentRepository,
            DocumentChunkService documentChunkService) {
        return new DocumentProcessingService(documentRepository, documentChunkService);
    }

    private List<DocumentStatus> captureSavedStatuses(DocumentRepository documentRepository) {
        List<DocumentStatus> savedStatuses = new ArrayList<>();
        doAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            savedStatuses.add(document.getStatus());
            return document;
        }).when(documentRepository).save(org.mockito.ArgumentMatchers.any(Document.class));
        return savedStatuses;
    }

    private Document uploadedDocument(Long id) {
        Document document = new Document();
        document.setId(id);
        document.setStatus(DocumentStatus.UPLOADED);
        return document;
    }
}
