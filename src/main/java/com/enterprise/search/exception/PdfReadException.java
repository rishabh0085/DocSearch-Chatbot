package com.enterprise.search.exception;

public class PdfReadException extends PdfTextExtractionException {

    public PdfReadException(Throwable cause) {
        super("The PDF could not be read", cause);
    }
}
