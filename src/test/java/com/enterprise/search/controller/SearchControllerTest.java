package com.enterprise.search.controller;

import com.enterprise.search.entity.Document;
import com.enterprise.search.config.CorsConfig;
import com.enterprise.search.exception.EmbeddingServiceException;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import com.enterprise.search.repository.DocumentRepository;
import com.enterprise.search.service.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import(CorsConfig.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SemanticSearchService semanticSearchService;

    @MockitoBean
    private DocumentRepository documentRepository;

    @Test
    void allowsPreflightRequestsFromBothLocalFrontendPorts() throws Exception {
        mockMvc.perform(options("/api/search")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,accept,authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "content-type, accept, authorization"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));

        mockMvc.perform(options("/api/search")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5174")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type,accept"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5174"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "content-type, accept"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void returnsMappedSemanticSearchResults() throws Exception {
        DocumentChunkSimilarityProjection first = result(1L, 3L, 1, 1,
                "Notice period is 30 days.", 0.91, 0.09);
        DocumentChunkSimilarityProjection second = result(2L, 4L, 2, 3,
                "Employee handbook content.", 0.85, 0.15);
        when(semanticSearchService.search("What is the notice period?", 3))
                .thenReturn(List.of(first, second));
        when(documentRepository.findAllById(any())).thenReturn(List.of(
                document(3L, "Notice_Period_Policy.pdf"),
                document(4L, "Employee_Handbook.pdf")));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"What is the notice period?\",\"topK\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("What is the notice period?"))
                .andExpect(jsonPath("$.results.length()").value(2))
                .andExpect(jsonPath("$.results[0].chunkId").value(1))
                .andExpect(jsonPath("$.results[0].documentId").value(3))
                .andExpect(jsonPath("$.results[0].documentName").value("Notice_Period_Policy.pdf"))
                .andExpect(jsonPath("$.results[0].pageNumber").value(1))
                .andExpect(jsonPath("$.results[0].chunkNumber").value(1))
                .andExpect(jsonPath("$.results[0].content").value("Notice period is 30 days."))
                .andExpect(jsonPath("$.results[0].similarity").value(0.91))
                .andExpect(jsonPath("$.results[0].cosineDistance").value(0.09))
                .andExpect(jsonPath("$.results[1].documentName").value("Employee_Handbook.pdf"));

        verify(semanticSearchService, times(1)).search("What is the notice period?", 3);
        verify(documentRepository, times(1)).findAllById(argThat(ids -> {
            assertEquals(Set.of(3L, 4L), toSet(ids));
            return true;
        }));
        verify(documentRepository, never()).findById(any());
    }

    @Test
    void returnsAnEmptyResultsArrayWhenNoChunksMatch() throws Exception {
        when(semanticSearchService.search("notice period", 5)).thenReturn(List.of());

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"notice period\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("notice period"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results").isEmpty());

        verify(semanticSearchService).search("notice period", 5);
        verify(documentRepository, never()).findAllById(any());
    }

    @Test
    void returnsNamesForMultipleChunksFromTheSameDocumentWithOneLookup() throws Exception {
        DocumentChunkSimilarityProjection first = result(1L, 3L, 1, 1,
                "Leave policy details.", 0.91, 0.09);
        DocumentChunkSimilarityProjection second = result(2L, 3L, 2, 2,
                "Leave policy exceptions.", 0.87, 0.13);
        DocumentChunkSimilarityProjection third = result(3L, 4L, 1, 1,
                "Handbook leave summary.", 0.82, 0.18);
        when(semanticSearchService.search("leave policy", 3)).thenReturn(List.of(first, second, third));
        when(documentRepository.findAllById(any())).thenReturn(List.of(
                document(3L, "Notice_Period_Policy.pdf"),
                document(4L, "Employee_Handbook.pdf")));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"leave policy\",\"topK\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results.length()").value(3))
                .andExpect(jsonPath("$.results[0].documentName").value("Notice_Period_Policy.pdf"))
                .andExpect(jsonPath("$.results[1].documentName").value("Notice_Period_Policy.pdf"))
                .andExpect(jsonPath("$.results[2].documentName").value("Employee_Handbook.pdf"));

        verify(documentRepository, times(1)).findAllById(argThat(ids -> {
            assertEquals(Set.of(3L, 4L), toSet(ids));
            return true;
        }));
        verify(documentRepository, never()).findById(any());
    }

    @Test
    void rejectsBlankQuery() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"\",\"topK\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoSearchOrDocumentLookup();
    }

    @Test
    void rejectsNullQuery() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":null,\"topK\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoSearchOrDocumentLookup();
    }

    @Test
    void rejectsZeroTopK() throws Exception {
        rejectsInvalidTopK("{\"query\":\"notice period\",\"topK\":0}");
    }

    @Test
    void rejectsTopKAboveMaximum() throws Exception {
        rejectsInvalidTopK("{\"query\":\"notice period\",\"topK\":51}");
    }

    @Test
    void missingTopKCurrentlyReachesTheControllerAndReturnsInternalServerError() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"notice period\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));

        verifyNoSearchOrDocumentLookup();
    }

    @Test
    void rejectsMissingQuery() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"topK\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoSearchOrDocumentLookup();
    }

    @Test
    void returnsNullDocumentNameWhenASearchResultReferencesAMissingDocument() throws Exception {
        DocumentChunkSimilarityProjection orphanedChunk = result(1L, 999L, 1, 1,
                "Content from an orphaned chunk.", 0.91, 0.09);
        when(semanticSearchService.search("orphaned chunk", 1)).thenReturn(List.of(orphanedChunk));
        when(documentRepository.findAllById(any())).thenReturn(List.of());

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"orphaned chunk\",\"topK\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].documentId").value(999))
                .andExpect(jsonPath("$.results[0].documentName").doesNotExist());

        verify(documentRepository).findAllById(argThat(ids -> {
            assertEquals(Set.of(999L), toSet(ids));
            return true;
        }));
    }

    @Test
    void handlesEmbeddingServiceFailureThroughGlobalExceptionHandler() throws Exception {
        when(semanticSearchService.search("notice period", 5))
                .thenThrow(new EmbeddingServiceException("Embedding provider unavailable"));

        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"notice period\",\"topK\":5}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(documentRepository, never()).findAllById(any());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"notice period\",\"topK\":"))
                .andExpect(status().is4xxClientError());

        verifyNoSearchOrDocumentLookup();
    }

    private void rejectsInvalidTopK(String content) throws Exception {
        mockMvc.perform(post("/api/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoSearchOrDocumentLookup();
    }

    private void verifyNoSearchOrDocumentLookup() {
        verify(semanticSearchService, never()).search(anyString(), anyInt());
        verify(documentRepository, never()).findAllById(any());
    }

    private Set<Long> toSet(Iterable<Long> ids) {
        Set<Long> values = new HashSet<>();
        ids.forEach(values::add);
        return values;
    }

    private DocumentChunkSimilarityProjection result(
            Long chunkId,
            Long documentId,
            int pageNumber,
            int chunkNumber,
            String content,
            double similarity,
            double cosineDistance) {

        DocumentChunkSimilarityProjection result = mock(DocumentChunkSimilarityProjection.class);
        when(result.getChunkId()).thenReturn(chunkId);
        when(result.getDocumentId()).thenReturn(documentId);
        when(result.getPageNumber()).thenReturn(pageNumber);
        when(result.getChunkNumber()).thenReturn(chunkNumber);
        when(result.getContent()).thenReturn(content);
        when(result.getSimilarity()).thenReturn(similarity);
        when(result.getCosineDistance()).thenReturn(cosineDistance);
        return result;
    }

    private Document document(Long id, String fileName) {
        Document document = new Document();
        document.setId(id);
        document.setFileName(fileName);
        return document;
    }
}
