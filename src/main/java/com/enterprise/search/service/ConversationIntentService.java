package com.enterprise.search.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Identifies the small set of conversational exchanges that can bypass RAG.
 * All other questions are sent through relevance-guarded retrieval so generic
 * questions cannot reach the conversational LLM as general-knowledge prompts.
 */
@Service
public class ConversationIntentService {

    private static final Pattern DOCUMENT_SIGNAL = Pattern.compile(
            "\\b(?:document(?:s)?|policy|policies|procedure(?:s)?|rule(?:s)?|guideline(?:s)?|handbook|"
                    + "notice\\s+period|resignation|leave|holiday(?:s)?|travel|expense(?:s)?|reimbursement|"
                    + "benefit(?:s)?|allowance(?:s)?|employee(?:s)?|employment|hr|human\\s+resources|"
                    + "technical\\s+team|non-technical\\s+team|team|company|organization(?:al)?|"
                    + "onboarding|attendance|payroll|salary|compensation|insurance|termination|"
                    + "work\\s+from\\s+home|remote\\s+work|wfh|compliance|"
                    + "sde(?:[-\\s]?\\d+)?|software\\s+(?:developer|engineer)|"
                    + "(?:backend|back-end|frontend|front-end|full[-\\s]?stack)\\s+developer|"
                    + "developer|engineer|qa(?:\\s+engineer)?|quality\\s+assurance|tester)\\b");
    private static final Pattern CASUAL_CONVERSATION = Pattern.compile(
            "(?:hi|hello|hey)(?:\\s+there)?|good\\s+(?:morning|afternoon|evening)|"
                    + "how are you(?: doing)?|what(?:'s| is) your name|who are you|"
                    + "what(?:'s| is) docsearch|what can you do|thanks?|thank you|"
                    + "bye|goodbye|help(?: me)?|can you help me");

    public QuestionIntent determineIntent(String question) {
        String normalizedQuestion = normalize(question);
        if (DOCUMENT_SIGNAL.matcher(normalizedQuestion).find()) {
            return QuestionIntent.DOCUMENT;
        }
        if (CASUAL_CONVERSATION.matcher(normalizedQuestion).matches()) {
            return QuestionIntent.CASUAL;
        }
        return QuestionIntent.DOCUMENT;
    }

    private String normalize(String question) {
        return question == null ? "" : question.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("[.!?]+$", "")
                .trim();
    }
}
