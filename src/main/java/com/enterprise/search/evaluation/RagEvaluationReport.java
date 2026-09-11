package com.enterprise.search.evaluation;

import java.util.List;

/** Aggregate metrics for a small RAG evaluation baseline. Metrics are reported, never build gates. */
public record RagEvaluationReport(
        int totalCases,
        int successfulRetrievalCases,
        double retrievalAccuracy,
        double recallAtK,
        double pageHitRate,
        double averageFirstRelevantRank,
        double answerAccuracy,
        double citationValidityRate,
        double groundedAnswerRate,
        List<RagEvaluationCaseResult> cases) {
}
