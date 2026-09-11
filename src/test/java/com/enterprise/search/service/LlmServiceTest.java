package com.enterprise.search.service;

import com.enterprise.search.exception.LlmServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class LlmServiceTest {

    private MockRestServiceServer mockServer;
    private LlmService llmService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        llmService = new LlmService(builder, "http://ollama.test", "llama3.2:3b");
    }

    @Test
    void generatesTextUsingTheConfiguredModelAndNonStreamingRequest() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().json("""
                        {"model":"llama3.2:3b","prompt":"Explain notice periods.","stream":false}
                        """))
                .andRespond(withSuccess("{\"response\":\"A notice period is advance notice before leaving a job.\"}",
                        MediaType.APPLICATION_JSON));

        String generatedText = llmService.generate("Explain notice periods.");

        assertEquals("A notice period is advance notice before leaving a job.", generatedText);
        mockServer.verify();
    }

    @Test
    void rejectsNullPrompt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> llmService.generate(null));

        assertEquals("Prompt for LLM generation cannot be null", exception.getMessage());
    }

    @Test
    void rejectsBlankPrompt() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> llmService.generate(" \n\t"));

        assertEquals("Prompt for LLM generation cannot be blank", exception.getMessage());
    }

    @Test
    void reportsOllamaHttpFailures() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        LlmServiceException exception = assertThrows(LlmServiceException.class,
                () -> llmService.generate("hello"));

        assertEquals("Ollama LLM request failed with HTTP status 503", exception.getMessage());
    }

    @Test
    void reportsOllamaConnectionFailures() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andRespond(request -> {
                    throw new ResourceAccessException("Connection refused");
                });

        LlmServiceException exception = assertThrows(LlmServiceException.class,
                () -> llmService.generate("hello"));

        assertEquals("Could not connect to local Ollama LLM service", exception.getMessage());
    }

    @Test
    void reportsMalformedResponses() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andRespond(withSuccess("{not-valid-json", MediaType.APPLICATION_JSON));

        LlmServiceException exception = assertThrows(LlmServiceException.class,
                () -> llmService.generate("hello"));

        assertEquals("Invalid response from local Ollama LLM service", exception.getMessage());
    }

    @Test
    void reportsAnEmptyResponseBody() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        LlmServiceException exception = assertThrows(LlmServiceException.class,
                () -> llmService.generate("hello"));

        assertEquals("Ollama LLM response was empty", exception.getMessage());
    }

    @Test
    void reportsMissingGeneratedText() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        LlmServiceException exception = assertThrows(LlmServiceException.class,
                () -> llmService.generate("hello"));

        assertEquals("Ollama LLM response did not contain generated text", exception.getMessage());
    }

    @Test
    void reportsBlankGeneratedText() {
        mockServer.expect(requestTo("http://ollama.test/api/generate"))
                .andRespond(withSuccess("{\"response\":\"  \"}", MediaType.APPLICATION_JSON));

        LlmServiceException exception = assertThrows(LlmServiceException.class,
                () -> llmService.generate("hello"));

        assertEquals("Ollama LLM response did not contain generated text", exception.getMessage());
    }
}
