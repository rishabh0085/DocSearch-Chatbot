package com.enterprise.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RagSourceResponse {

    private final Long documentId;
    private final String documentName;
    private final Integer pageNumber;
    private final Integer chunkNumber;
    private final Double similarity;
    private final String content;
}
