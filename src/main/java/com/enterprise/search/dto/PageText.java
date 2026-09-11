package com.enterprise.search.dto;

/**
 * Text extracted from one PDF page. Page numbers are one-based.
 */
public record PageText(int pageNumber, String text) {
}
