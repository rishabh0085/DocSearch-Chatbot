package com.enterprise.search.exception;

/**
 * Indicates that the local Ollama LLM could not generate a usable response.
 */
public class LlmServiceException extends RuntimeException {

    public LlmServiceException(String message) {
        super(message);
    }

    public LlmServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
