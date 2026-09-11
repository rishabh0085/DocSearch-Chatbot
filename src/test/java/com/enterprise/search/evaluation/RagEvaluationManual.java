package com.enterprise.search.evaluation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * Manually run with {@code mvn test -Dtest=RagEvaluationManual}. It intentionally does not end
 * in "Test", so ordinary Maven test runs do not require PostgreSQL or Ollama evaluation calls.
 */
@SpringBootTest
class RagEvaluationManual {

    @Autowired
    private RagEvaluationService evaluationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void printsRepeatableRetrievalAndFullRagReports() throws Exception {
        List<RagEvaluationCase> cases = loadCases();
        print("RETRIEVAL EVALUATION", evaluationService.evaluateRetrieval(cases));
        print("FULL RAG EVALUATION", evaluationService.evaluateFullRag(cases));
    }

    private List<RagEvaluationCase> loadCases() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/rag-evaluation.json")) {
            if (input == null) {
                throw new IllegalStateException("rag-evaluation.json was not found");
            }
            return objectMapper.readValue(input, new TypeReference<>() { });
        }
    }

    private void print(String title, RagEvaluationReport report) {
        System.out.printf("%n==== %s ====%n", title);
        System.out.printf("Total cases: %d%n", report.totalCases());
        System.out.printf("Successful retrieval cases: %d%n", report.successfulRetrievalCases());
        System.out.printf("Retrieval Accuracy / Recall@K: %.1f%%%n", report.retrievalAccuracy() * 100);
        System.out.printf("Page Hit Rate: %.1f%%%n", report.pageHitRate() * 100);
        System.out.printf("Average First Relevant Rank: %.2f%n", report.averageFirstRelevantRank());
        System.out.printf("Answer Accuracy: %.1f%%%n", report.answerAccuracy() * 100);
        System.out.printf("Citation Validity: %.1f%%%n", report.citationValidityRate() * 100);
        System.out.printf("Grounded Answer Rate: %.1f%%%n", report.groundedAnswerRate() * 100);
        report.cases().forEach(result -> System.out.printf("[%s] %s | rank=%s | %s%n",
                passedFor(title, result) ? "PASS" : "FAIL", result.question(),
                result.firstRelevantRank(), result.notes()));
    }

    private boolean passedFor(String title, RagEvaluationCaseResult result) {
        return "RETRIEVAL EVALUATION".equals(title) ? result.retrievalPassed() : result.overallPassed();
    }
}
