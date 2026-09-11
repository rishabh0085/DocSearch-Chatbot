package com.enterprise.search.service;

import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.entity.Document;
import com.enterprise.search.exception.DocumentProcessingException;
import com.enterprise.search.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Coordinates the synchronous PDF extraction, chunking, and embedding workflow.
 */
@Service
public class DocumentProcessingService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkService documentChunkService;

    public DocumentProcessingService(
            DocumentRepository documentRepository,
            DocumentChunkService documentChunkService) {

        this.documentRepository = documentRepository;
        this.documentChunkService = documentChunkService;
    }

    public void processDocument(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID is required for processing");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found with ID " + documentId));

        if (document.getStatus() == DocumentStatus.READY || document.getStatus() == DocumentStatus.PROCESSING) {
            return; // Repeated local requests do not duplicate extraction or embeddings.
        }

        document.setStatus(DocumentStatus.PROCESSING);
        document.setUpdatedAt(LocalDateTime.now());
        document = documentRepository.save(document);

        try {
            documentChunkService.processDocument(document);
            documentChunkService.generateEmbeddingsForDocument(documentId);

            document.setStatus(DocumentStatus.READY);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);
        } catch (RuntimeException exception) {
            document.setStatus(DocumentStatus.FAILED);
            document.setUpdatedAt(LocalDateTime.now());
            documentRepository.save(document);

            throw new DocumentProcessingException(
                    "Document processing failed for document " + documentId + ": " + exception.getMessage(),
                    exception);
        }
    }
}
