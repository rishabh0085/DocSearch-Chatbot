package com.enterprise.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SearchResponse {

    private final String query;
    private final List<SearchResultResponse> results;
}
