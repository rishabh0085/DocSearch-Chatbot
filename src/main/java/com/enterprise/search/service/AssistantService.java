package com.enterprise.search.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Selects either the conversational LLM path or the relevance-guarded RAG path.
 */
@Service
public class AssistantService {

    private static final String CONVERSATION_PROMPT = """
            You are DocSearch, a friendly and concise AI assistant.
            Answer normal conversational questions naturally. Explain that you can help search and understand an organization's documents when asked what you can do. Do not claim access to information you do not have or invent organization-specific/document information.

            User question: %s
            """;

    private final ConversationIntentService conversationIntentService;
    private final LlmService llmService;
    private final RagService ragService;

    public AssistantService(
            ConversationIntentService conversationIntentService,
            LlmService llmService,
            RagService ragService) {
        this.conversationIntentService = conversationIntentService;
        this.llmService = llmService;
        this.ragService = ragService;
    }

    public RagResult answer(String question, int topK) {
        if (question == null) {
            throw new IllegalArgumentException("Assistant question cannot be null");
        }
        if (question.isBlank()) {
            throw new IllegalArgumentException("Assistant question cannot be blank");
        }

        if (conversationIntentService.determineIntent(question) == QuestionIntent.DOCUMENT) {
            return ragService.answer(question, topK);
        }

        String answer = llmService.generate(CONVERSATION_PROMPT.formatted(question.trim()));
        return new RagResult(answer, true, List.of());
    }
}
