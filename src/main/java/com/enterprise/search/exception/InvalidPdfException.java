package com.enterprise.search.exception;

public class InvalidPdfException extends PdfTextExtractionException {

    public InvalidPdfException() {
        super("The supplied file is not a valid PDF");
    }
}
