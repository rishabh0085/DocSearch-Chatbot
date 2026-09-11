package com.enterprise.search.service;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import com.enterprise.search.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Coordinates semantic retrieval, source-aware context construction, prompt construction,
 * and local LLM generation.
 */
@Service
public class RagService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagService.class);
    public static final String SAFE_REFUSAL = "I don't have information about this topic in the available documents. "
            + "Please ask a question related to the documents uploaded to DocSearch.";

    private final SemanticSearchService semanticSearchService;
    private final DocumentRepository documentRepository;
    private final RagContextBuilder ragContextBuilder;
    private final RagPromptBuilder ragPromptBuilder;
    private final LlmService llmService;
    private final RagCitationValidator ragCitationValidator;
    private final RagAnswerabilityGuard answerabilityGuard;
    private final RagQueryNormalizer ragQueryNormalizer;
    public RagService(
            SemanticSearchService semanticSearchService,
            DocumentRepository documentRepository,
            RagContextBuilder ragContextBuilder,
            RagPromptBuilder ragPromptBuilder,
            LlmService llmService,
            RagCitationValidator ragCitationValidator,
            RagAnswerabilityGuard answerabilityGuard,
            RagQueryNormalizer ragQueryNormalizer) {
        this.semanticSearchService = semanticSearchService;
        this.documentRepository = documentRepository;
        this.ragContextBuilder = ragContextBuilder;
        this.ragPromptBuilder = ragPromptBuilder;
        this.llmService = llmService;
        this.ragCitationValidator = ragCitationValidator;
        this.answerabilityGuard = answerabilityGuard;
        this.ragQueryNormalizer = ragQueryNormalizer;
    }

    public RagResult answer(String question, int topK) {
        if (question == null) {
            throw new IllegalArgumentException("RAG question cannot be null");
        }
        if (question.isBlank()) {
            throw new IllegalArgumentException("RAG question cannot be blank");
        }

        return answerUncached(question, topK);
    }

    private RagResult answerUncached(String question, int topK) {
        String retrievalQuery = ragQueryNormalizer.normalizeForRetrieval(question);
        List<DocumentChunkSimilarityProjection> searchResults = semanticSearchService.search(retrievalQuery, topK);
        RagAnswerabilityGuard.AnswerabilityDecision decision = answerabilityGuard.evaluate(searchResults);
        LOGGER.info("[RAG-PERF] relevanceValidator.relevant={} highestSimilarity={} llmCalled={}",
                decision.answerable(), formatSimilarity(decision.highestSimilarity()), decision.answerable());
        if (!decision.answerable()) {
            LOGGER.info("[RAG] question.outOfScope reason=insufficientRetrievalRelevance");
            long citationValidationStart = System.nanoTime();
            Boolean citationValid = null;
            try {
                citationValid = ragCitationValidator.isValid(SAFE_REFUSAL, 0);
            } finally {
                LOGGER.info("[RAG-PERF] citationValidation.durationMs={} citationValid={}",
                        elapsedMillis(citationValidationStart), citationValid == null ? "unavailable" : citationValid);
            }
            LOGGER.info("[RAG-PERF] llm.durationMs=0");
            return new RagResult(SAFE_REFUSAL, citationValid, List.of());
        }

        long documentLookupStart = System.nanoTime();
        Map<Long, String> documentNames = null;
        try {
            documentNames = findDocumentNames(searchResults);
        } finally {
            LOGGER.info("[RAG-PERF] documentLookup.durationMs={} documentCount={}",
                    elapsedMillis(documentLookupStart), documentNames == null ? "unavailable" : documentNames.size());
        }

        long contextBuildStart = System.nanoTime();
        List<RagContext> contexts = null;
        try {
            contexts = ragContextBuilder.build(searchResults, documentNames);
        } finally {
            LOGGER.info("[RAG-PERF] contextBuild.durationMs={} contextCount={}",
                    elapsedMillis(contextBuildStart), contexts == null ? "unavailable" : contexts.size());
        }

        long promptBuildStart = System.nanoTime();
        List<RagContext> selectedContexts = null;
        String prompt;
        try {
            selectedContexts = ragPromptBuilder.selectContexts(contexts);
            prompt = ragPromptBuilder.buildPrompt(question, selectedContexts);
        } finally {
            LOGGER.info("[RAG-PERF] promptBuild.durationMs={} contextCount={}",
                    elapsedMillis(promptBuildStart), selectedContexts == null ? "unavailable" : selectedContexts.size());
        }

        long llmStart = System.nanoTime();
        String answer;
        try {
            answer = llmService.generate(prompt);
        } finally {
            LOGGER.info("[RAG-PERF] llm.durationMs={}", elapsedMillis(llmStart));
        }

        long citationValidationStart = System.nanoTime();
        Boolean citationValid = null;
        try {
            citationValid = ragCitationValidator.isValid(answer, selectedContexts.size());
        } finally {
            LOGGER.info("[RAG-PERF] citationValidation.durationMs={} citationValid={}",
                    elapsedMillis(citationValidationStart), citationValid == null ? "unavailable" : citationValid);
        }

        return new RagResult(answer, citationValid, selectedContexts);
    }

    private Map<Long, String> findDocumentNames(List<DocumentChunkSimilarityProjection> searchResults) {
        if (searchResults.isEmpty()) {
            return Map.of();
        }

        Set<Long> documentIds = searchResults.stream()
                .map(DocumentChunkSimilarityProjection::getDocumentId)
                .collect(Collectors.toSet());

        return documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Document::getFileName));
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String formatSimilarity(double similarity) {
        return Double.isFinite(similarity) ? String.format(java.util.Locale.ROOT, "%.4f", similarity) : "none";
    }
}
