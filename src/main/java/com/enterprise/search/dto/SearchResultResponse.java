package com.enterprise.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SearchResultResponse {

    private final Long chunkId;
    private final Long documentId;
    private final String documentName;
    private final Integer pageNumber;
    private final Integer chunkNumber;
    private final String content;
    private final Double similarity;
    private final Double cosineDistance;
}
