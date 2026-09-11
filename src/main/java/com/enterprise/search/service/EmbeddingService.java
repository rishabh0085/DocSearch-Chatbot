package com.enterprise.search.service;

import com.enterprise.search.exception.EmbeddingServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

/**
 * Generates embeddings through the locally hosted Ollama embedding API.
 */
@Service
public class EmbeddingService {

    private static final int EXPECTED_EMBEDDING_DIMENSION = 768;

    private final RestClient restClient;
    private final String embeddingModel;

    @Autowired
    public EmbeddingService(
            @Value("${ollama.base-url}") String ollamaBaseUrl,
            @Value("${ollama.embedding-model}") String embeddingModel) {

        this(RestClient.builder(), ollamaBaseUrl, embeddingModel);
    }

    EmbeddingService(
            RestClient.Builder restClientBuilder,
            String ollamaBaseUrl,
            String embeddingModel) {

        this.restClient = restClientBuilder.baseUrl(ollamaBaseUrl).build();
        this.embeddingModel = embeddingModel;
    }

    public List<Float> generateEmbedding(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text for embedding cannot be null");
        }
        if (text.isBlank()) {
            throw new IllegalArgumentException("Text for embedding cannot be blank");
        }

        OllamaEmbeddingResponse response;
        try {
            response = restClient.post()
                    .uri("/api/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new OllamaEmbeddingRequest(embeddingModel, text))
                    .retrieve()
                    .body(OllamaEmbeddingResponse.class);
        } catch (RestClientResponseException exception) {
            throw new EmbeddingServiceException(
                    "Ollama embedding request failed with HTTP status " + exception.getStatusCode().value(),
                    exception);
        } catch (ResourceAccessException exception) {
            throw new EmbeddingServiceException("Could not connect to Ollama embedding service", exception);
        } catch (RestClientException exception) {
            throw new EmbeddingServiceException("Invalid response from Ollama embedding service", exception);
        }

        return validateResponse(response);
    }

    private List<Float> validateResponse(OllamaEmbeddingResponse response) {
        if (response == null) {
            throw new EmbeddingServiceException("Ollama embedding response was empty");
        }
        if (response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new EmbeddingServiceException("Ollama embedding response did not contain embeddings");
        }

        List<Float> embedding = response.embeddings().getFirst();
        if (embedding == null) {
            throw new EmbeddingServiceException("Ollama embedding response contained a null embedding");
        }
        if (embedding.size() != EXPECTED_EMBEDDING_DIMENSION) {
            throw new EmbeddingServiceException(
                    "Expected embedding dimension 768 but received " + embedding.size());
        }
        if (embedding.stream().anyMatch(value -> value == null)) {
            throw new EmbeddingServiceException("Ollama embedding response contained a null embedding value");
        }
        return List.copyOf(embedding);
    }
}

record OllamaEmbeddingRequest(String model, String input) {
}

record OllamaEmbeddingResponse(String model, List<List<Float>> embeddings) {
}
