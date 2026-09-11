package com.enterprise.search.exception;

/**
 * Indicates that a local embedding provider could not produce a valid embedding.
 */
public class EmbeddingServiceException extends RuntimeException {

    public EmbeddingServiceException(String message) {
        super(message);
    }

    public EmbeddingServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
