package com.enterprise.search.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagCitationValidatorTest {

    private final RagCitationValidator validator = new RagCitationValidator();

    @Test
    void acceptsValidCitationsIncludingDuplicatesAndTheHighestAvailableSource() {
        assertTrue(validator.isValid("The policy says one month. [Source 1]", 5));
        assertTrue(validator.isValid("One month. [Source 1] [Source 3]", 5));
        assertTrue(validator.isValid("Supported twice. [Source 5] [Source 5]", 5));
    }

    @Test
    void rejectsMalformedOrUnavailableCitations() {
        assertFalse(validator.isValid("[Source 0]", 5));
        assertFalse(validator.isValid("[Source 6]", 5));
        assertFalse(validator.isValid("[Source 99]", 5));
        assertFalse(validator.isValid("[Source A]", 5));
        assertFalse(validator.isValid("[Source]", 5));
        assertFalse(validator.isValid("[source 1]", 5));
    }

    @Test
    void handlesNoContextAndAnswersWithoutCitations() {
        assertTrue(validator.isValid(null, 0));
        assertTrue(validator.isValid("  ", 0));
        assertTrue(validator.isValid("The information is not available in the provided documents.", 0));
        assertFalse(validator.isValid("[Source 1]", 0));
        assertFalse(validator.isValid(null, 5));
        assertFalse(validator.isValid("  ", 5));
        assertFalse(validator.isValid("Normal prose refers to Source 1 without a citation.", 5));
    }

    @Test
    void rejectsNegativeSourceCount() {
        assertThrows(IllegalArgumentException.class, () -> validator.isValid("answer", -1));
    }
}
