package com.enterprise.search.controller;

import com.enterprise.search.dto.DocumentResponse;
import com.enterprise.search.service.DocumentService;
import com.enterprise.search.service.DocumentSummaryService;
import com.enterprise.search.service.FileStorageService;
import com.enterprise.search.dto.DocumentSummaryResponse;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentSummaryService documentSummaryService;
    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentService documentService, DocumentSummaryService documentSummaryService, DocumentRepository documentRepository, FileStorageService fileStorageService) {
        this.documentService = documentService;
        this.documentSummaryService = documentSummaryService;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/{id}/summary")
    public DocumentSummaryResponse summarizeDocument(@PathVariable Long id) {
        return documentSummaryService.summarize(id);
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> getDocumentContent(@PathVariable Long id, @RequestParam(defaultValue = "false") boolean download) {
        Document document = documentRepository.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));
        Resource resource = fileStorageService.loadFile(document.getFilePath());
        MediaType contentType;
        try { contentType = MediaType.parseMediaType(document.getFileType()); } catch (Exception ignored) { contentType = MediaType.APPLICATION_OCTET_STREAM; }
        ContentDisposition disposition = (download ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(document.getFileName()).build();
        return ResponseEntity.ok().contentType(contentType).header("Content-Disposition", disposition.toString()).body(resource);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file) {

        DocumentResponse document = documentService.uploadDocument(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(document);
    }

    @GetMapping
    public List<DocumentResponse> getAllDocuments() {
        return documentService.getAllDocuments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id) {

        return documentService.getDocumentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id) {

        documentService.deleteDocument(id);

        return ResponseEntity.noContent().build();
    }
}
