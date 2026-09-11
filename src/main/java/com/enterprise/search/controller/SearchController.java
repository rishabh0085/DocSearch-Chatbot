package com.enterprise.search.controller;

import com.enterprise.search.dto.SearchRequest;
import com.enterprise.search.dto.SearchResponse;
import com.enterprise.search.dto.SearchResultResponse;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import com.enterprise.search.repository.DocumentRepository;
import com.enterprise.search.service.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final SemanticSearchService semanticSearchService;
    private final DocumentRepository documentRepository;

    public SearchController(
            SemanticSearchService semanticSearchService,
            DocumentRepository documentRepository) {

        this.semanticSearchService = semanticSearchService;
        this.documentRepository = documentRepository;
    }

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        List<DocumentChunkSimilarityProjection> searchResults = semanticSearchService.search(
                request.getQuery(),
                request.getTopK());

        Map<Long, String> documentNames = findDocumentNames(searchResults);
        List<SearchResultResponse> results = searchResults.stream()
                .map(result -> new SearchResultResponse(
                        result.getChunkId(),
                        result.getDocumentId(),
                        documentNames.get(result.getDocumentId()),
                        result.getPageNumber(),
                        result.getChunkNumber(),
                        result.getContent(),
                        result.getSimilarity(),
                        result.getCosineDistance()))
                .toList();

        return new SearchResponse(request.getQuery(), results);
    }

    private Map<Long, String> findDocumentNames(List<DocumentChunkSimilarityProjection> searchResults) {
        if (searchResults.isEmpty()) {
            return Map.of();
        }

        Set<Long> documentIds = searchResults.stream()
                .map(DocumentChunkSimilarityProjection::getDocumentId)
                .collect(Collectors.toSet());

        return documentRepository.findAllById(documentIds).stream()
                .collect(Collectors.toMap(Document::getId, Document::getFileName));
    }
}
