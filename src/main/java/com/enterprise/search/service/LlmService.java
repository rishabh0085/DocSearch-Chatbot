package com.enterprise.search.service;

import com.enterprise.search.exception.LlmServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.util.Map;


/**
 * Generates text through the locally hosted Ollama generation API.
 */
@Service
public class LlmService {

    private final RestClient restClient;
    private final String llmModel;
    private final String keepAlive;
    private final int maxOutputTokens;

    @Autowired
    public LlmService(
            @Value("${ollama.base-url}") String ollamaBaseUrl,
            @Value("${ollama.llm-model}") String llmModel,
            @Value("${ollama.llm.keep-alive:10m}") String keepAlive,
            @Value("${ollama.llm.max-output-tokens:192}") int maxOutputTokens) {

        this(RestClient.builder(), ollamaBaseUrl, llmModel, keepAlive, maxOutputTokens);
    }

    LlmService(RestClient.Builder restClientBuilder, String ollamaBaseUrl, String llmModel) {
        this(restClientBuilder, ollamaBaseUrl, llmModel, "10m", 192);
    }

    LlmService(RestClient.Builder restClientBuilder, String ollamaBaseUrl, String llmModel,
               String keepAlive, int maxOutputTokens) {
        if (maxOutputTokens <= 0) throw new IllegalArgumentException("LLM max output tokens must be greater than 0");
        this.restClient = restClientBuilder.baseUrl(ollamaBaseUrl).build();
        this.llmModel = llmModel;
        this.keepAlive = keepAlive;
        this.maxOutputTokens = maxOutputTokens;
    }

    public String generate(String prompt) {
        if (prompt == null) {
            throw new IllegalArgumentException("Prompt for LLM generation cannot be null");
        }
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt for LLM generation cannot be blank");
        }

        OllamaGenerateResponse response;
        try {
            response = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(prompt))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);
        } catch (RestClientResponseException exception) {
            throw new LlmServiceException(
                    "Ollama LLM request failed with HTTP status " + exception.getStatusCode().value(),
                    exception);
        } catch (ResourceAccessException exception) {
            throw new LlmServiceException("Could not connect to local Ollama LLM service", exception);
        } catch (RestClientException exception) {
            throw new LlmServiceException("Invalid response from local Ollama LLM service", exception);
        }

        return validateResponse(response);
    }

    private OllamaGenerateRequest request(String prompt) {
        return new OllamaGenerateRequest(llmModel, prompt, false, keepAlive, Map.of("num_predict", maxOutputTokens));
    }

    private String validateResponse(OllamaGenerateResponse response) {
        if (response == null) {
            throw new LlmServiceException("Ollama LLM response was empty");
        }
        if (response.response() == null || response.response().isBlank()) {
            throw new LlmServiceException("Ollama LLM response did not contain generated text");
        }
        return response.response();
    }
}

record OllamaGenerateRequest(
        String model,
        String prompt,
        boolean stream,
        String keep_alive,
        Map<String, Integer> options) {
}

record OllamaGenerateResponse(String response) {
}
