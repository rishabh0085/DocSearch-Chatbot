package com.enterprise.search.service;

import com.enterprise.search.dto.RagContext;

import java.util.List;

/**
 * Internal result of one RAG execution, including the contexts supplied to the prompt.
 */
public record RagResult(String answer, boolean citationValid, List<RagContext> contexts) {
}
