package com.enterprise.search.service;

import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest {

    @Test
    void processesPdfSynchronouslyAndReturnsTheCompletedDocument() {
        DocumentRepository documents = mock(DocumentRepository.class);
        FileStorageService files = mock(FileStorageService.class);
        DocumentProcessingService processing = mock(DocumentProcessingService.class);
        PdfTextExtractionService pdfTextExtraction = mock(PdfTextExtractionService.class);
        Document uploaded = document(7L, DocumentStatus.UPLOADED);
        Document ready = document(7L, DocumentStatus.READY);
        when(files.storeFile(any())).thenReturn("uploads/notice.pdf");
        when(documents.save(any(Document.class))).thenReturn(uploaded);
        when(documents.findById(7L)).thenReturn(Optional.of(ready));

        var result = new DocumentService(documents, files, processing, pdfTextExtraction).uploadDocument(
                new MockMultipartFile("file", "notice.pdf", "application/pdf", "content".getBytes()));

        verify(processing).processDocument(7L);
        assertEquals(DocumentStatus.READY.name(), result.getStatus());
    }

    @Test
    void backfillsMissingPdfPageCountsWithoutAssigningOneToNonPdfs() {
        DocumentRepository documents = mock(DocumentRepository.class);
        PdfTextExtractionService pdfTextExtraction = mock(PdfTextExtractionService.class);
        Document pdf = document(7L, DocumentStatus.READY);
        pdf.setFilePath("uploads/policy.pdf");
        Document text = document(8L, DocumentStatus.READY);
        text.setFileType("text/plain");
        text.setFilePath("uploads/notes.txt");
        when(documents.findAll()).thenReturn(java.util.List.of(pdf, text));
        when(pdfTextExtraction.getPageCount(java.nio.file.Path.of("uploads/policy.pdf"))).thenReturn(3);
        when(documents.save(pdf)).thenReturn(pdf);

        var responses = new DocumentService(documents, mock(FileStorageService.class), mock(DocumentProcessingService.class), pdfTextExtraction)
                .getAllDocuments();

        assertEquals(3, responses.getFirst().getPageCount());
        assertEquals(null, responses.get(1).getPageCount());
        verify(pdfTextExtraction).getPageCount(java.nio.file.Path.of("uploads/policy.pdf"));
    }

    private Document document(Long id, DocumentStatus status) {
        Document document = new Document();
        document.setId(id);
        document.setFileName("notice.pdf");
        document.setFileType("application/pdf");
        document.setStatus(status);
        return document;
    }
}
