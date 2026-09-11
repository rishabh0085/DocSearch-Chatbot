package com.enterprise.search.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validates bracketed source citations against the contexts supplied to one RAG prompt.
 */
@Service
public class RagCitationValidator {

    private static final Pattern SOURCE_CITATION_CANDIDATE =
            Pattern.compile("(?i)\\[source[^\\]]*\\]");
    private static final Pattern VALID_SOURCE_CITATION =
            Pattern.compile("\\[Source ([1-9]\\d*)\\]");

    /**
     * A response is valid when every bracketed source citation uses the exact required format
     * and refers to an available source. When context was supplied, at least one citation is
     * required; without context, only answers without citations are valid.
     */
    public boolean isValid(String answer, int sourceCount) {
        if (sourceCount < 0) {
            throw new IllegalArgumentException("Source count cannot be negative");
        }
        if (answer == null || answer.isBlank()) {
            return sourceCount == 0;
        }

        boolean foundCitation = false;
        Matcher candidateMatcher = SOURCE_CITATION_CANDIDATE.matcher(answer);
        while (candidateMatcher.find()) {
            foundCitation = true;
            Matcher validMatcher = VALID_SOURCE_CITATION.matcher(candidateMatcher.group());
            if (!validMatcher.matches()) {
                return false;
            }
            try {
                int sourceNumber = Integer.parseInt(validMatcher.group(1));
                if (sourceNumber > sourceCount) {
                    return false;
                }
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return sourceCount == 0 || foundCitation;
    }
}
