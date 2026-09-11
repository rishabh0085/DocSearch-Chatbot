package com.enterprise.search.exception;

public class EmptyPdfException extends PdfTextExtractionException {

    public EmptyPdfException() {
        super("The PDF contains no pages");
    }
}
