package com.enterprise.search.evaluation;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import com.enterprise.search.repository.DocumentRepository;
import com.enterprise.search.service.RagCitationValidator;
import com.enterprise.search.service.RagResult;
import com.enterprise.search.service.RagService;
import com.enterprise.search.service.SemanticSearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagEvaluationServiceTest {

    @Test
    void evaluatesRetrievedDocumentPageRankAndRecallAtK() {
        Dependencies dependencies = dependencies();
        RagEvaluationCase evaluationCase = answerable("notice", List.of(2));
        DocumentChunkSimilarityProjection unrelatedChunk = chunk(2L, 1);
        DocumentChunkSimilarityProjection relevantChunk = chunk(1L, 2);
        when(dependencies.search.search("notice", 5)).thenReturn(List.of(unrelatedChunk, relevantChunk));
        when(dependencies.documents.findAllById(any())).thenReturn(List.of(
                document(1L, "Notice_Period_Policy.pdf"), document(2L, "Other.pdf")));

        RagEvaluationReport report = dependencies.service.evaluateRetrieval(List.of(evaluationCase));

        RagEvaluationCaseResult result = report.cases().getFirst();
        assertTrue(result.retrievalPassed());
        assertTrue(result.pagePassed());
        assertEquals(2, result.firstRelevantRank());
        assertEquals(1.0, report.retrievalAccuracy());
        assertEquals(1.0, report.recallAtK());
        assertEquals(1.0, report.pageHitRate());
        assertEquals(2.0, report.averageFirstRelevantRank());
        verify(dependencies.ragService, never()).answer(any(), anyInt());
    }

    @Test
    void recordsMissingDocumentAndPageWithoutFailingEvaluation() {
        Dependencies dependencies = dependencies();
        DocumentChunkSimilarityProjection unrelatedChunk = chunk(2L, 3);
        when(dependencies.search.search("notice", 5)).thenReturn(List.of(unrelatedChunk));
        when(dependencies.documents.findAllById(any())).thenReturn(List.of(document(2L, "Other.pdf")));

        RagEvaluationReport report = dependencies.service.evaluateRetrieval(List.of(answerable("notice", List.of(2))));

        assertFalse(report.cases().getFirst().retrievalPassed());
        assertFalse(report.cases().getFirst().pagePassed());
        assertEquals(null, report.cases().getFirst().firstRelevantRank());
        assertEquals(0.0, report.retrievalAccuracy());
    }

    @Test
    void evaluatesGroundedFullRagAnswersAndUsesOneRagExecutionPerCase() {
        Dependencies dependencies = dependencies();
        RagEvaluationCase evaluationCase = answerable("TECHNICAL", List.of(1));
        RagContext context = new RagContext(11L, "Notice_Period_Policy.pdf", 1, 2, "Tech Team 3 Months", 0.9);
        when(dependencies.ragService.answer("TECHNICAL", 5))
                .thenReturn(new RagResult("The technical team has 3 MONTHS. [Source 1]", true, List.of(context)));

        RagEvaluationReport report = dependencies.service.evaluateFullRag(List.of(evaluationCase));

        RagEvaluationCaseResult result = report.cases().getFirst();
        assertTrue(result.answerPassed());
        assertTrue(result.citationPassed());
        assertTrue(result.groundingPassed());
        assertTrue(result.overallPassed());
        assertEquals(1.0, report.answerAccuracy());
        assertEquals(1.0, report.citationValidityRate());
        assertEquals(1.0, report.groundedAnswerRate());
        verify(dependencies.ragService).answer("TECHNICAL", 5);
        verify(dependencies.search, never()).search(any(), anyInt());
    }

    @Test
    void reportsMissingExpectedTextInvalidCitationAndBlankAnswer() {
        Dependencies dependencies = dependencies();
        RagEvaluationCase evaluationCase = answerable("notice", List.of(2));
        RagContext context = new RagContext(11L, "Notice_Period_Policy.pdf", 2, 3, "Non-Tech Team 1 Month", 0.9);
        when(dependencies.ragService.answer("notice", 5))
                .thenReturn(new RagResult(" ", false, List.of(context)));

        RagEvaluationCaseResult result = dependencies.service.evaluateFullRag(List.of(evaluationCase)).cases().getFirst();

        assertFalse(result.answerPassed());
        assertFalse(result.citationPassed());
        assertFalse(result.groundingPassed());
        assertFalse(result.overallPassed());
    }

    @Test
    void evaluatesUnanswerableCasesAndEmptyDatasetsDeterministically() {
        Dependencies dependencies = dependencies();
        RagEvaluationCase unanswerable = new RagEvaluationCase("capital", List.of(), null, List.of(), false);
        when(dependencies.ragService.answer("capital", 5)).thenReturn(new RagResult(
                "The information is not available in the provided documents.", true, List.of()));
        when(dependencies.citations.isValid("The information is not available in the provided documents.", 0)).thenReturn(true);

        RagEvaluationCaseResult result = dependencies.service.evaluateFullRag(List.of(unanswerable)).cases().getFirst();

        assertTrue(result.answerPassed());
        assertTrue(result.citationPassed());
        assertTrue(result.groundingPassed());
        assertTrue(result.overallPassed());
        assertEquals(0, dependencies.service.evaluateRetrieval(List.of()).totalCases());
    }

    private RagEvaluationCase answerable(String question, List<Integer> pages) {
        return new RagEvaluationCase(question, List.of("3 months"), "Notice_Period_Policy.pdf", pages, true);
    }

    private DocumentChunkSimilarityProjection chunk(Long documentId, int page) {
        DocumentChunkSimilarityProjection projection = mock(DocumentChunkSimilarityProjection.class);
        when(projection.getDocumentId()).thenReturn(documentId);
        when(projection.getPageNumber()).thenReturn(page);
        return projection;
    }

    private Document document(Long id, String name) {
        Document document = new Document();
        document.setId(id);
        document.setFileName(name);
        return document;
    }

    private Dependencies dependencies() {
        SemanticSearchService search = mock(SemanticSearchService.class);
        RagService ragService = mock(RagService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        RagCitationValidator citations = mock(RagCitationValidator.class);
        return new Dependencies(new RagEvaluationService(search, ragService, documents, citations, 5),
                search, ragService, documents, citations);
    }

    private record Dependencies(
            RagEvaluationService service,
            SemanticSearchService search,
            RagService ragService,
            DocumentRepository documents,
            RagCitationValidator citations) {
    }
}
