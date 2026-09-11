package com.enterprise.search.service;

import com.enterprise.search.dto.PageText;
import com.enterprise.search.entity.Document;
import com.enterprise.search.entity.DocumentChunk;
import com.enterprise.search.exception.EmbeddingServiceException;
import com.enterprise.search.repository.DocumentChunkRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds and persists page-aware text chunks for PDF documents.
 */
@Service
public class DocumentChunkService {

    private final DocumentChunkRepository documentChunkRepository;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final EmbeddingService embeddingService;
    private final int chunkSize;
    private final int chunkOverlap;

    public DocumentChunkService(
            DocumentChunkRepository documentChunkRepository,
            PdfTextExtractionService pdfTextExtractionService,
            EmbeddingService embeddingService,
            @Value("${app.document.chunk-size}") int chunkSize,
            @Value("${app.document.chunk-overlap}") int chunkOverlap) {

        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Document chunk size must be greater than zero");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("Document chunk overlap must be non-negative and smaller than chunk size");
        }

        this.documentChunkRepository = documentChunkRepository;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.embeddingService = embeddingService;
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
    }

    /**
     * Replaces existing chunks after extracting page text from the document's PDF file.
     * This method is intentionally internal; no REST endpoint invokes it yet.
     */
    @Transactional
    public List<DocumentChunk> processDocument(Document document) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("A persisted document is required for processing");
        }
        if (!"application/pdf".equalsIgnoreCase(document.getFileType())) {
            throw new IllegalArgumentException("Only PDF documents can be processed at this stage");
        }
        if (document.getFilePath() == null || document.getFilePath().isBlank()) {
            throw new IllegalArgumentException("Document file path is required for processing");
        }

        List<PageText> pages = pdfTextExtractionService.extractPageText(Path.of(document.getFilePath()));
        document.setPageCount(pages.size());
        return replaceChunks(document, pages);
    }

    /**
     * Replaces existing chunks with chunks created from already extracted page text.
     */
    @Transactional
    public List<DocumentChunk> replaceChunks(Document document, List<PageText> pages) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("A persisted document is required for chunking");
        }

        documentChunkRepository.deleteByDocumentId(document.getId());
        List<DocumentChunk> chunks = createChunks(document, pages);
        return chunks.isEmpty() ? List.of() : documentChunkRepository.saveAll(chunks);
    }

    /**
     * Generates missing embeddings for a document's chunks in chunk-number order.
     */
    public void generateEmbeddingsForDocument(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document ID is required for embedding generation");
        }

        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkNumberAsc(documentId);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("No chunks found for document " + documentId);
        }

        for (DocumentChunk chunk : chunks) {
            if (hasValidEmbedding(chunk.getEmbedding())) {
                continue;
            }

            List<Float> embedding = embeddingService.generateEmbedding(chunk.getContent());
            chunk.setEmbedding(toFloatArray(embedding));
            documentChunkRepository.save(chunk);
        }
    }

    /**
     * Creates page-aware chunks without persisting them. Exposed for internal reuse and unit tests.
     */
    public List<DocumentChunk> createChunks(Document document, List<PageText> pages) {
        if (document == null) {
            throw new IllegalArgumentException("Document is required for chunking");
        }
        if (pages == null || pages.isEmpty()) {
            return List.of();
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        int chunkNumber = 1;
        LocalDateTime createdAt = LocalDateTime.now();

        for (PageText page : pages) {
            if (page == null || page.text() == null || page.text().isBlank()) {
                continue;
            }

            for (String content : splitPageText(page.text())) {
                DocumentChunk chunk = new DocumentChunk();
                chunk.setDocument(document);
                chunk.setPageNumber(page.pageNumber());
                chunk.setChunkNumber(chunkNumber++);
                chunk.setContent(content);
                chunk.setCreatedAt(createdAt);
                chunks.add(chunk);
            }
        }
        return List.copyOf(chunks);
    }

    private List<String> splitPageText(String text) {
        List<String> chunks = new ArrayList<>();
        int start = skipWhitespace(text, 0);

        while (start < text.length()) {
            int maximumEnd = Math.min(start + chunkSize, text.length());
            int end = findWordBoundary(text, start, maximumEnd);
            String content = text.substring(start, end).trim();
            if (!content.isEmpty()) {
                chunks.add(content);
            }
            if (end >= text.length()) {
                break;
            }

            int nextStart = Math.max(start + 1, end - chunkOverlap);
            nextStart = moveToWordStart(text, nextStart);
            nextStart = skipWhitespace(text, nextStart);
            start = nextStart > start ? nextStart : end;
        }
        return chunks;
    }

    private int findWordBoundary(String text, int start, int maximumEnd) {
        if (maximumEnd == text.length()) {
            return maximumEnd;
        }
        for (int index = maximumEnd - 1; index > start; index--) {
            if (Character.isWhitespace(text.charAt(index))) {
                return index;
            }
        }
        return maximumEnd;
    }

    private int moveToWordStart(String text, int index) {
        while (index > 0 && !Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return index;
    }

    private int skipWhitespace(String text, int index) {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    private boolean hasValidEmbedding(float[] embedding) {
        return embedding != null && embedding.length == 768;
    }

    private float[] toFloatArray(List<Float> embedding) {
        if (embedding == null) {
            throw new EmbeddingServiceException("Embedding service returned a null embedding");
        }
        if (embedding.size() != 768) {
            throw new EmbeddingServiceException(
                    "Expected embedding dimension 768 but received " + embedding.size());
        }

        float[] values = new float[embedding.size()];
        for (int index = 0; index < embedding.size(); index++) {
            Float value = embedding.get(index);
            if (value == null) {
                throw new EmbeddingServiceException("Embedding service returned a null embedding value");
            }
            values[index] = value;
        }
        return values;
    }
}
