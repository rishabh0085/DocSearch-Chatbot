package com.enterprise.search.evaluation;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import com.enterprise.search.repository.DocumentRepository;
import com.enterprise.search.service.RagCitationValidator;
import com.enterprise.search.service.RagResult;
import com.enterprise.search.service.RagService;
import com.enterprise.search.service.SemanticSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Evaluates retrieval separately from full RAG execution. Retrieval metrics apply only to
 * cases with an expected source document; unanswerable cases are evaluated for answer behavior.
 */
@Service
public class RagEvaluationService {

    private final SemanticSearchService semanticSearchService;
    private final RagService ragService;
    private final DocumentRepository documentRepository;
    private final RagCitationValidator citationValidator;
    private final int topK;

    public RagEvaluationService(
            SemanticSearchService semanticSearchService,
            RagService ragService,
            DocumentRepository documentRepository,
            RagCitationValidator citationValidator,
            @Value("${rag.evaluation.top-k:5}") int topK) {

        if (topK <= 0) {
            throw new IllegalArgumentException("RAG evaluation topK must be greater than 0");
        }
        this.semanticSearchService = semanticSearchService;
        this.ragService = ragService;
        this.documentRepository = documentRepository;
        this.citationValidator = citationValidator;
        this.topK = topK;
    }

    public RagEvaluationReport evaluateRetrieval(List<RagEvaluationCase> evaluationCases) {
        requireCases(evaluationCases);
        List<RagEvaluationCaseResult> results = new ArrayList<>();
        for (RagEvaluationCase evaluationCase : evaluationCases) {
            List<DocumentChunkSimilarityProjection> chunks = semanticSearchService.search(evaluationCase.question(), topK);
            Map<Long, String> names = findDocumentNames(chunks);
            results.add(evaluateRetrievalCase(evaluationCase, chunks.stream()
                    .map(chunk -> new RetrievedChunk(names.get(chunk.getDocumentId()), chunk.getPageNumber()))
                    .toList()));
        }
        return toReport(results, false);
    }

    /** Executes exactly one existing RAG flow per case; retrieval is assessed from its returned contexts. */
    public RagEvaluationReport evaluateFullRag(List<RagEvaluationCase> evaluationCases) {
        requireCases(evaluationCases);
        List<RagEvaluationCaseResult> results = new ArrayList<>();
        for (RagEvaluationCase evaluationCase : evaluationCases) {
            RagResult ragResult = ragService.answer(evaluationCase.question(), topK);
            List<RetrievedChunk> chunks = ragResult.contexts().stream()
                    .map(context -> new RetrievedChunk(context.documentName(), context.pageNumber()))
                    .toList();
            RagEvaluationCaseResult retrieval = evaluateRetrievalCase(evaluationCase, chunks);
            results.add(evaluateAnswerCase(evaluationCase, ragResult, retrieval));
        }
        return toReport(results, true);
    }

    private RagEvaluationCaseResult evaluateRetrievalCase(
            RagEvaluationCase evaluationCase, List<RetrievedChunk> chunks) {

        List<String> documents = chunks.stream().map(RetrievedChunk::documentName).toList();
        List<Integer> pages = chunks.stream().map(RetrievedChunk::pageNumber).toList();
        if (!evaluationCase.shouldAnswerFromContext()) {
            return new RagEvaluationCaseResult(evaluationCase.question(), true, false, true, false, false,
                    false, false, null, documents, pages, "Unanswerable case: retrieval is not scored.");
        }

        int firstRelevantIndex = firstRelevantIndex(evaluationCase.expectedSourceDocument(), documents);
        boolean retrievalPassed = firstRelevantIndex >= 0;
        boolean pageScored = !evaluationCase.expectedSourcePages().isEmpty();
        boolean pagePassed = !pageScored
                || chunks.stream()
                .filter(chunk -> evaluationCase.expectedSourceDocument().equals(chunk.documentName()))
                .map(RetrievedChunk::pageNumber)
                .anyMatch(evaluationCase.expectedSourcePages()::contains);
        Integer firstRelevantRank = retrievalPassed ? firstRelevantIndex + 1 : null;
        String notes = retrievalPassed ? "Expected document retrieved." : "Expected document was not retrieved.";
        return new RagEvaluationCaseResult(evaluationCase.question(), retrievalPassed, pageScored, pagePassed, false, false,
                false, false, firstRelevantRank, documents, pages, notes);
    }

    private RagEvaluationCaseResult evaluateAnswerCase(
            RagEvaluationCase evaluationCase, RagResult ragResult, RagEvaluationCaseResult retrieval) {

        String answer = ragResult.answer();
        boolean answerPresent = answer != null && !answer.isBlank();
        boolean answerPassed;
        boolean citationPassed;
        boolean groundingPassed;
        if (evaluationCase.shouldAnswerFromContext()) {
            answerPassed = answerPresent && evaluationCase.expectedAnswerContains().stream()
                    .allMatch(expected -> containsIgnoreCase(answer, expected));
            citationPassed = ragResult.citationValid();
            groundingPassed = answerPresent && citationPassed && !ragResult.contexts().isEmpty()
                    && retrieval.retrievalPassed() && retrieval.pagePassed();
        } else {
            answerPassed = answerPresent && (containsIgnoreCase(answer, RagService.SAFE_REFUSAL)
                    || containsIgnoreCase(answer, "information is not available in the provided documents"));
            citationPassed = citationValidator.isValid(answer, 0);
            groundingPassed = answerPassed && citationPassed;
        }
        boolean overallPassed = retrieval.retrievalPassed() && answerPassed && citationPassed && groundingPassed;
        String notes = retrieval.notes() + " Answer=" + answerPassed + ", citation=" + citationPassed
                + ", grounding=" + groundingPassed + ".";
        return new RagEvaluationCaseResult(evaluationCase.question(), retrieval.retrievalPassed(), retrieval.pageScored(), retrieval.pagePassed(),
                answerPassed, citationPassed, groundingPassed, overallPassed, retrieval.firstRelevantRank(),
                retrieval.retrievedDocuments(), retrieval.retrievedPages(), notes);
    }

    private RagEvaluationReport toReport(List<RagEvaluationCaseResult> results, boolean includeAnswerMetrics) {
        if (results.isEmpty()) {
            return new RagEvaluationReport(0, 0, 0, 0, 0, 0, 0, 0, 0, List.of());
        }
        List<RagEvaluationCaseResult> retrievalCases = results.stream()
                .filter(result -> !result.notes().startsWith("Unanswerable case"))
                .toList();
        double retrievalAccuracy = rate(retrievalCases, RagEvaluationCaseResult::retrievalPassed);
        double pageHitRate = rate(retrievalCases.stream()
                .filter(RagEvaluationCaseResult::pageScored).toList(), RagEvaluationCaseResult::pagePassed);
        double averageRank = retrievalCases.stream().map(RagEvaluationCaseResult::firstRelevantRank)
                .filter(rank -> rank != null).mapToInt(Integer::intValue).average().orElse(0);
        double answerAccuracy = includeAnswerMetrics ? rate(results, RagEvaluationCaseResult::answerPassed) : 0;
        double citationRate = includeAnswerMetrics ? rate(results, RagEvaluationCaseResult::citationPassed) : 0;
        double groundingRate = includeAnswerMetrics ? rate(results, RagEvaluationCaseResult::groundingPassed) : 0;
        int successfulRetrievalCases = (int) retrievalCases.stream()
                .filter(RagEvaluationCaseResult::retrievalPassed).count();
        return new RagEvaluationReport(results.size(), successfulRetrievalCases, retrievalAccuracy, retrievalAccuracy, pageHitRate, averageRank,
                answerAccuracy, citationRate, groundingRate, List.copyOf(results));
    }

    private Map<Long, String> findDocumentNames(List<DocumentChunkSimilarityProjection> chunks) {
        Set<Long> ids = chunks.stream().map(DocumentChunkSimilarityProjection::getDocumentId).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Map.of();
        }
        return documentRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Document::getId, Document::getFileName));
    }

    private int firstRelevantIndex(String expectedDocument, List<String> documents) {
        for (int index = 0; index < documents.size(); index++) {
            if (expectedDocument.equals(documents.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private double rate(Collection<RagEvaluationCaseResult> results,
                        Function<RagEvaluationCaseResult, Boolean> passed) {
        return results.isEmpty() ? 0 : results.stream().filter(passed::apply).count() / (double) results.size();
    }

    private boolean containsIgnoreCase(String value, String expected) {
        return value.toLowerCase(Locale.ROOT).contains(expected.toLowerCase(Locale.ROOT));
    }

    private void requireCases(List<RagEvaluationCase> evaluationCases) {
        if (evaluationCases == null) {
            throw new IllegalArgumentException("RAG evaluation cases cannot be null");
        }
    }

    private record RetrievedChunk(String documentName, Integer pageNumber) {
    }
}
