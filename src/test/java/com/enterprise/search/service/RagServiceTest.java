package com.enterprise.search.service;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.entity.Document;
import com.enterprise.search.repository.DocumentChunkSimilarityProjection;
import com.enterprise.search.repository.DocumentRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagServiceTest {

    @Test
    void acceptedQuestionUsesNormalRagFlowAndCallsLlm() {
        Dependencies d = dependencies();
        DocumentChunkSimilarityProjection chunk = chunk(1L, 0.77);
        RagContext context = new RagContext(1L, "Notice_Period_Policy.pdf", 1, 1, "Three months", 0.77);
        when(d.search.search("notice", 5)).thenReturn(List.of(chunk));
        when(d.documents.findAllById(any())).thenReturn(List.of(document(1L, "Notice_Period_Policy.pdf")));
        when(d.contexts.build(any(), any())).thenReturn(List.of(context));
        when(d.prompts.selectContexts(List.of(context))).thenReturn(List.of(context));
        when(d.prompts.buildPrompt("notice", List.of(context))).thenReturn("prompt");
        when(d.llm.generate("prompt")).thenReturn("Three months. [Source 1]");
        when(d.citations.isValid("Three months. [Source 1]", 1)).thenReturn(true);

        RagResult result = d.service.answer("notice", 5);

        assertEquals("Three months. [Source 1]", result.answer());
        assertEquals(List.of(context), result.contexts());
        verify(d.llm).generate("prompt");
        verify(d.citations).isValid("Three months. [Source 1]", 1);
    }

    @Test
    void rejectedQuestionReturnsSafeRefusalWithoutCallingLlmOrBuildingContext() {
        Dependencies d = dependencies();
        DocumentChunkSimilarityProjection chunk = chunk(1L, 0.40);
        when(d.search.search("capital", 5)).thenReturn(List.of(chunk));
        when(d.citations.isValid(RagService.SAFE_REFUSAL, 0)).thenReturn(true);

        RagResult result = d.service.answer("capital", 5);

        assertEquals(RagService.SAFE_REFUSAL, result.answer());
        assertTrue(result.contexts().isEmpty());
        verify(d.llm, never()).generate(any());
        verifyNoInteractions(d.documents, d.contexts, d.prompts);
        verify(d.citations).isValid(RagService.SAFE_REFUSAL, 0);
    }

    @Test
    void emptyRetrievalReturnsSafeRefusalWithoutHallucinatedSources() {
        Dependencies d = dependencies();
        when(d.search.search("unknown", 5)).thenReturn(List.of());
        when(d.citations.isValid(RagService.SAFE_REFUSAL, 0)).thenReturn(true);

        RagResult result = d.service.answer("unknown", 5);

        assertEquals(RagService.SAFE_REFUSAL, result.answer());
        assertTrue(result.contexts().isEmpty());
        verify(d.llm, never()).generate(any());
    }

    private Dependencies dependencies() {
        SemanticSearchService search = mock(SemanticSearchService.class);
        DocumentRepository documents = mock(DocumentRepository.class);
        RagContextBuilder contexts = mock(RagContextBuilder.class);
        RagPromptBuilder prompts = mock(RagPromptBuilder.class);
        LlmService llm = mock(LlmService.class);
        RagCitationValidator citations = mock(RagCitationValidator.class);
        return new Dependencies(new RagService(search, documents, contexts, prompts, llm, citations,
                new RagAnswerabilityGuard(0.65)), search, documents, contexts, prompts, llm, citations);
    }

    private DocumentChunkSimilarityProjection chunk(long documentId, double similarity) {
        DocumentChunkSimilarityProjection chunk = mock(DocumentChunkSimilarityProjection.class);
        when(chunk.getDocumentId()).thenReturn(documentId);
        when(chunk.getSimilarity()).thenReturn(similarity);
        return chunk;
    }

    private Document document(long id, String name) {
        Document document = new Document();
        document.setId(id);
        document.setFileName(name);
        return document;
    }

    private record Dependencies(RagService service, SemanticSearchService search, DocumentRepository documents,
                                RagContextBuilder contexts, RagPromptBuilder prompts, LlmService llm,
                                RagCitationValidator citations) { }
}
