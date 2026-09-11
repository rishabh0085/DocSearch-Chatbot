package com.enterprise.search.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void representsASearchRequest() {
        SearchRequest request = new SearchRequest("What is the notice period?", 5);

        assertEquals("What is the notice period?", request.getQuery());
        assertEquals(5, request.getTopK());
        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void representsAllSearchResultFields() {
        SearchResultResponse result = result(1L, "Notice_Period_Policy.pdf");

        assertEquals(1L, result.getChunkId());
        assertEquals(3L, result.getDocumentId());
        assertEquals("Notice_Period_Policy.pdf", result.getDocumentName());
        assertEquals(1, result.getPageNumber());
        assertEquals(2, result.getChunkNumber());
        assertEquals("Notice period is 30 days.", result.getContent());
        assertEquals(0.91, result.getSimilarity());
        assertEquals(0.09, result.getCosineDistance());
    }

    @Test
    void representsAQueryWithMultipleResults() {
        List<SearchResultResponse> results = List.of(
                result(1L, "Notice_Period_Policy.pdf"),
                result(2L, "Employee_Handbook.pdf"));
        SearchResponse response = new SearchResponse("What is the notice period?", results);

        assertEquals("What is the notice period?", response.getQuery());
        assertEquals(results, response.getResults());
        assertEquals(2, response.getResults().size());
    }

    @Test
    void rejectsBlankOrNullQueries() {
        assertFalse(validator.validate(new SearchRequest(" ", 5)).isEmpty());
        assertFalse(validator.validate(new SearchRequest(null, 5)).isEmpty());
    }

    @Test
    void rejectsInvalidTopK() {
        assertFalse(validator.validate(new SearchRequest("notice period", 0)).isEmpty());
        assertFalse(validator.validate(new SearchRequest("notice period", 51)).isEmpty());
    }

    private SearchResultResponse result(Long chunkId, String documentName) {
        return new SearchResultResponse(
                chunkId,
                3L,
                documentName,
                1,
                2,
                "Notice period is 30 days.",
                0.91,
                0.09);
    }
}
