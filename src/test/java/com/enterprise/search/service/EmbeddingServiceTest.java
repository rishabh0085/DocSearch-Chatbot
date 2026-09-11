package com.enterprise.search.service;

import com.enterprise.search.exception.EmbeddingServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class EmbeddingServiceTest {

    private MockRestServiceServer mockServer;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        embeddingService = new EmbeddingService(builder, "http://ollama.test", "nomic-embed-text");
    }

    @Test
    void generatesA768DimensionEmbedding() {
        mockServer.expect(requestTo("http://ollama.test/api/embed"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().json("{\"model\":\"nomic-embed-text\",\"input\":\"hello\"}"))
                .andRespond(withSuccess(responseWithDimensions(768), MediaType.APPLICATION_JSON));

        List<Float> embedding = embeddingService.generateEmbedding("hello");

        assertEquals(768, embedding.size());
        assertEquals(0.0F, embedding.getFirst());
        mockServer.verify();
    }

    @Test
    void rejectsNullInput() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> embeddingService.generateEmbedding(null));

        assertEquals("Text for embedding cannot be null", exception.getMessage());
    }

    @Test
    void rejectsBlankInput() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> embeddingService.generateEmbedding(" \n\t"));

        assertEquals("Text for embedding cannot be blank", exception.getMessage());
    }

    @Test
    void rejectsAnUnexpectedEmbeddingDimension() {
        mockServer.expect(requestTo("http://ollama.test/api/embed"))
                .andRespond(withSuccess(responseWithDimensions(767), MediaType.APPLICATION_JSON));

        EmbeddingServiceException exception = assertThrows(EmbeddingServiceException.class,
                () -> embeddingService.generateEmbedding("hello"));

        assertEquals("Expected embedding dimension 768 but received 767", exception.getMessage());
    }

    @Test
    void reportsOllamaHttpFailures() {
        mockServer.expect(requestTo("http://ollama.test/api/embed"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        EmbeddingServiceException exception = assertThrows(EmbeddingServiceException.class,
                () -> embeddingService.generateEmbedding("hello"));

        assertEquals("Ollama embedding request failed with HTTP status 503", exception.getMessage());
    }

    @Test
    void reportsOllamaConnectionFailures() {
        mockServer.expect(requestTo("http://ollama.test/api/embed"))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection refused");
                });

        EmbeddingServiceException exception = assertThrows(EmbeddingServiceException.class,
                () -> embeddingService.generateEmbedding("hello"));

        assertEquals("Could not connect to Ollama embedding service", exception.getMessage());
    }

    @Test
    void reportsMalformedResponses() {
        mockServer.expect(requestTo("http://ollama.test/api/embed"))
                .andRespond(withSuccess("{not-valid-json", MediaType.APPLICATION_JSON));

        EmbeddingServiceException exception = assertThrows(EmbeddingServiceException.class,
                () -> embeddingService.generateEmbedding("hello"));

        assertEquals("Invalid response from Ollama embedding service", exception.getMessage());
    }

    @Test
    void reportsResponsesWithoutEmbeddings() {
        mockServer.expect(requestTo("http://ollama.test/api/embed"))
                .andRespond(withSuccess("{\"model\":\"nomic-embed-text\",\"embeddings\":[]}",
                        MediaType.APPLICATION_JSON));

        EmbeddingServiceException exception = assertThrows(EmbeddingServiceException.class,
                () -> embeddingService.generateEmbedding("hello"));

        assertEquals("Ollama embedding response did not contain embeddings", exception.getMessage());
    }

    private String responseWithDimensions(int dimensions) {
        String values = IntStream.range(0, dimensions)
                .mapToObj(index -> index + ".0")
                .collect(java.util.stream.Collectors.joining(","));
        return "{\"model\":\"nomic-embed-text\",\"embeddings\":[[" + values + "]]}";
    }
}
