package com.enterprise.search.exception;

public class PdfFileNotFoundException extends PdfTextExtractionException {

    public PdfFileNotFoundException() {
        super("PDF file does not exist");
    }
}
