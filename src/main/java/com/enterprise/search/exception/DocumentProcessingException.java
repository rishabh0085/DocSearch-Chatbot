package com.enterprise.search.exception;

/**
 * Indicates that a document could not complete its synchronous processing pipeline.
 */
public class DocumentProcessingException extends RuntimeException {

    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
