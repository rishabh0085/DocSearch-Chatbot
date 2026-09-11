package com.enterprise.search.controller;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.dto.RagRequest;
import com.enterprise.search.dto.RagResponse;
import com.enterprise.search.dto.RagSourceResponse;
import com.enterprise.search.service.AssistantService;
import com.enterprise.search.service.RagResult;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private static final Logger LOGGER = LoggerFactory.getLogger(RagController.class);

    private final AssistantService assistantService;

    public RagController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    public RagResponse ask(@Valid @RequestBody RagRequest request) {
        long requestStart = System.nanoTime();
        LOGGER.info("[RAG-PERF] request.start questionLength={} topK={}",
                request.getQuestion().length(), request.getTopK());
        try {
            RagResult result = assistantService.answer(request.getQuestion(), request.getTopK());
            List<RagSourceResponse> sources = result.contexts().stream()
                    .map(this::toSourceResponse)
                    .toList();

            LOGGER.info("[RAG-PERF] request.complete answerLength={} sourceCount={} citationValid={}",
                    result.answer().length(), sources.size(), result.citationValid());

            return new RagResponse(request.getQuestion(), result.answer(), result.citationValid(), sources);
        } finally {
            LOGGER.info("[RAG-PERF] request.totalDurationMs={}", elapsedMillis(requestStart));
        }
    }

    private RagSourceResponse toSourceResponse(RagContext context) {
        return new RagSourceResponse(
                context.documentId(),
                context.documentName(),
                context.pageNumber(),
                context.chunkNumber(),
                context.similarity(),
                context.content());
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
