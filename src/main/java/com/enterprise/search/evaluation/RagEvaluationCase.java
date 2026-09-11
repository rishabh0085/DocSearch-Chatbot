package com.enterprise.search.evaluation;

import java.util.List;

/** A deterministic expectation for a single RAG evaluation question. */
public record RagEvaluationCase(
        String question,
        List<String> expectedAnswerContains,
        String expectedSourceDocument,
        List<Integer> expectedSourcePages,
        boolean shouldAnswerFromContext) {

    public RagEvaluationCase {
        expectedAnswerContains = expectedAnswerContains == null ? List.of() : List.copyOf(expectedAnswerContains);
        expectedSourcePages = expectedSourcePages == null ? List.of() : List.copyOf(expectedSourcePages);
    }
}
