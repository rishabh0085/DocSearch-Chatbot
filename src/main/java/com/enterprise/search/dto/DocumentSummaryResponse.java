package com.enterprise.search.dto;

import java.util.List;

public record DocumentSummaryResponse(
        Long documentId,
        String documentName,
        String summary,
        List<String> keyPoints,
        List<String> importantRules,
        List<DocumentSummarySource> sources) {
}
