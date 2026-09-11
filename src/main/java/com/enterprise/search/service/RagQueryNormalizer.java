package com.enterprise.search.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Enriches retrieval queries with deterministic role terminology. The original
 * question remains the sole question supplied to the answer-generation prompt.
 */
@Service
public class RagQueryNormalizer {

    private static final Pattern NOTICE_PERIOD = Pattern.compile(
            "\\b(?:notice(?:\\s+period)?|resignation|how\\s+long.*\\bnotice)\\b");
    private static final Pattern TECH_ROLE = Pattern.compile(
            "\\b(?:sde(?:[-\\s]?\\d+)?|software\\s+(?:developer|engineer)|"
                    + "(?:backend|back-end|frontend|front-end|full[-\\s]?stack)\\s+developer|"
                    + "developer|engineer|qa(?:\\s+engineer)?|quality\\s+assurance|tester)\\b");
    private static final Pattern HR_ROLE = Pattern.compile("\\b(?:hr|human\\s+resources)\\b");

    public String normalizeForRetrieval(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("RAG query cannot be null or blank");
        }

        String normalizedQuestion = question.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        if (!NOTICE_PERIOD.matcher(normalizedQuestion).find()) {
            return question;
        }
        if (TECH_ROLE.matcher(normalizedQuestion).find()) {
            return "notice period Tech Team developer engineer role";
        }
        if (HR_ROLE.matcher(normalizedQuestion).find()) {
            return "notice period HR Human Resources role";
        }
        return question;
    }
}
