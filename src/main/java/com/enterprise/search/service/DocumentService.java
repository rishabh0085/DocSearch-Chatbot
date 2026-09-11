package com.enterprise.search.service;

import com.enterprise.search.entity.Document;
import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.dto.DocumentResponse;
import com.enterprise.search.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class DocumentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProcessingService documentProcessingService;
    private final PdfTextExtractionService pdfTextExtractionService;

    public DocumentService(
            DocumentRepository documentRepository,
            FileStorageService fileStorageService,
            DocumentProcessingService documentProcessingService,
            PdfTextExtractionService pdfTextExtractionService) {

        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.documentProcessingService = documentProcessingService;
        this.pdfTextExtractionService = pdfTextExtractionService;
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll().stream()
                .map(this::backfillPdfPageCount)
                .map(DocumentResponse::from)
                .toList();
    }

    public Optional<DocumentResponse> getDocumentById(Long id) {
        return documentRepository.findById(id)
                .map(this::backfillPdfPageCount)
                .map(DocumentResponse::from);
    }

    public void deleteDocument(Long id) {
        documentRepository.deleteById(id);
    }

    public DocumentResponse uploadDocument(MultipartFile file) {

        // 1. Validate file
        validateFile(file);

        // 2. Store file physically
        String filePath = fileStorageService.storeFile(file);

        // 3. Create document metadata
        Document document = new Document();

        document.setFileName(file.getOriginalFilename());
        document.setFileType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setFilePath(filePath);
        document.setStatus(DocumentStatus.UPLOADED);

        LocalDateTime now = LocalDateTime.now();

        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        // 4. Process PDFs locally before responding, so upload status reflects the completed workflow.
        Document savedDocument = documentRepository.save(document);
        if ("application/pdf".equalsIgnoreCase(savedDocument.getFileType())) {
            documentProcessingService.processDocument(savedDocument.getId());
            savedDocument = documentRepository.findById(savedDocument.getId()).orElse(savedDocument);
        }
        return DocumentResponse.from(savedDocument);
    }

    private void validateFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException(
                    "File type could not be determined"
            );
        }

        Set<String> allowedTypes = Set.of(
                "application/pdf",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "text/plain"
        );

        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only PDF, DOCX and TXT files are allowed"
            );
        }
    }

    private Document backfillPdfPageCount(Document document) {
        if (!"application/pdf".equalsIgnoreCase(document.getFileType()) || document.getPageCount() != null
                || document.getFilePath() == null || document.getFilePath().isBlank()) {
            return document;
        }

        try {
            document.setPageCount(pdfTextExtractionService.getPageCount(Path.of(document.getFilePath())));
            return documentRepository.save(document);
        } catch (RuntimeException exception) {
            LOGGER.warn("Could not backfill page count for document {}", document.getId(), exception);
            return document;
        }
    }
}
