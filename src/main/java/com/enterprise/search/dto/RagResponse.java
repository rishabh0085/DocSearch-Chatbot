package com.enterprise.search.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class RagResponse {

    private final String question;
    private final String answer;
    private final boolean citationValid;
    private final List<RagSourceResponse> sources;
}
