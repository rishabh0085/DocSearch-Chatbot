package com.enterprise.search.evaluation;

import java.util.List;

/** Results for one evaluation question. A null rank means no relevant rank was applicable or found. */
public record RagEvaluationCaseResult(
        String question,
        boolean retrievalPassed,
        boolean pageScored,
        boolean pagePassed,
        boolean answerPassed,
        boolean citationPassed,
        boolean groundingPassed,
        boolean overallPassed,
        Integer firstRelevantRank,
        List<String> retrievedDocuments,
        List<Integer> retrievedPages,
        String notes) {
}
