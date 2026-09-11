package com.enterprise.search.repository;

import com.enterprise.search.constants.DocumentStatus;
import com.enterprise.search.entity.Document;
import com.enterprise.search.entity.DocumentChunk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@Transactional
class DocumentChunkRepositoryIntegrationTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentChunkRepository documentChunkRepository;

    @Test
    void findsClosestChunksOrdersByCosineDistanceExcludesNullsAndAppliesTopK() {
        Document document = documentRepository.saveAndFlush(document());
        DocumentChunk exactMatch = chunk(document, 1, "exact", unitVector(0));
        DocumentChunk orthogonal = chunk(document, 2, "orthogonal", unitVector(1));
        DocumentChunk withoutEmbedding = chunk(document, 3, "no embedding", null);
        documentChunkRepository.saveAllAndFlush(List.of(exactMatch, orthogonal, withoutEmbedding));

        List<DocumentChunkSimilarityProjection> allResults =
                documentChunkRepository.findSimilarChunks(unitVector(0), Integer.MAX_VALUE);
        List<DocumentChunkSimilarityProjection> testDocumentResults = allResults.stream()
                .filter(result -> result.getDocumentId().equals(document.getId()))
                .toList();

        assertEquals(List.of(exactMatch.getId(), orthogonal.getId()),
                testDocumentResults.stream().map(DocumentChunkSimilarityProjection::getChunkId).toList());
        assertEquals(0.0, testDocumentResults.getFirst().getCosineDistance(), 0.000001);
        assertEquals(1.0, testDocumentResults.getFirst().getSimilarity(), 0.000001);
        assertEquals(1.0, testDocumentResults.get(1).getCosineDistance(), 0.000001);
        assertFalse(testDocumentResults.stream()
                .anyMatch(result -> result.getChunkId().equals(withoutEmbedding.getId())));

        List<DocumentChunkSimilarityProjection> topResult =
                documentChunkRepository.findSimilarChunks(unitVector(0), 1);

        assertEquals(1, topResult.size());
        assertEquals(exactMatch.getId(), topResult.getFirst().getChunkId());
    }

    private Document document() {
        Document document = new Document();
        document.setFileName("similarity-test.pdf");
        document.setFileType("application/pdf");
        document.setStatus(DocumentStatus.READY);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    private DocumentChunk chunk(Document document, int chunkNumber, String content, float[] embedding) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setDocument(document);
        chunk.setPageNumber(1);
        chunk.setChunkNumber(chunkNumber);
        chunk.setContent(content);
        chunk.setEmbedding(embedding);
        chunk.setCreatedAt(LocalDateTime.now());
        return chunk;
    }

    private float[] unitVector(int index) {
        float[] vector = new float[768];
        vector[index] = 1.0F;
        return vector;
    }
}
