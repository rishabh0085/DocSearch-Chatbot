package com.enterprise.search.repository;

import com.enterprise.search.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentIdOrderByChunkNumberAsc(Long documentId);

    void deleteByDocumentId(Long documentId);

    @Query(value = """
            SELECT dc.id AS "chunkId",
                   dc.document_id AS "documentId",
                   dc.page_number AS "pageNumber",
                   dc.chunk_number AS "chunkNumber",
                   dc.content AS "content",
                   dc.created_at AS "createdAt",
                   dc.embedding <=> CAST(:queryEmbedding AS vector) AS "cosineDistance",
                   1 - (dc.embedding <=> CAST(:queryEmbedding AS vector)) AS "similarity"
            FROM document_chunks dc
            WHERE dc.embedding IS NOT NULL
            ORDER BY dc.embedding <=> CAST(:queryEmbedding AS vector) ASC
            LIMIT :topK
            """, nativeQuery = true)
    List<DocumentChunkSimilarityProjection> findSimilarChunksByVectorLiteral(
            @Param("queryEmbedding") String queryEmbedding,
            @Param("topK") int topK);

    default List<DocumentChunkSimilarityProjection> findSimilarChunks(float[] queryEmbedding, int topK) {
        return findSimilarChunksByVectorLiteral(toVectorLiteral(queryEmbedding), topK);
    }

    private static String toVectorLiteral(float[] embedding) {
        StringBuilder vectorLiteral = new StringBuilder("[");
        for (int index = 0; index < embedding.length; index++) {
            if (index > 0) {
                vectorLiteral.append(',');
            }
            vectorLiteral.append(embedding[index]);
        }
        return vectorLiteral.append(']').toString();
    }
}
