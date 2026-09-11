package com.enterprise.search.controller;

import com.enterprise.search.dto.RagContext;
import com.enterprise.search.service.RagResult;
import com.enterprise.search.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RagController.class)
class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RagService ragService;

    @Test
    void returnsTheGeneratedAnswerAndSourcesInRagExecutionOrder() throws Exception {
        List<RagContext> contexts = List.of(
                context(3L, "Notice_Period_Policy.pdf", 2, 3,
                        "Non-tech team notice period is one month.", 0.7707),
                context(4L, "Employee_Handbook.pdf", 1, 2,
                        "Employee handbook notice details.", 0.7122));
        when(ragService.answer("What is the notice period for resignation?", 5))
                .thenReturn(new RagResult("The notice period is one month. [Source 1]", true, contexts));

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the notice period for resignation?\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is the notice period for resignation?"))
                .andExpect(jsonPath("$.answer").value("The notice period is one month. [Source 1]"))
                .andExpect(jsonPath("$.citationValid").value(true))
                .andExpect(jsonPath("$.sources.length()").value(2))
                .andExpect(jsonPath("$.sources[0].documentId").value(3))
                .andExpect(jsonPath("$.sources[0].documentName").value("Notice_Period_Policy.pdf"))
                .andExpect(jsonPath("$.sources[0].pageNumber").value(2))
                .andExpect(jsonPath("$.sources[0].chunkNumber").value(3))
                .andExpect(jsonPath("$.sources[0].similarity").value(0.7707))
                .andExpect(jsonPath("$.sources[0].content").value("Non-tech team notice period is one month."))
                .andExpect(jsonPath("$.sources[1].documentId").value(4))
                .andExpect(jsonPath("$.sources[1].documentName").value("Employee_Handbook.pdf"));

        verify(ragService, times(1)).answer("What is the notice period for resignation?", 5);
    }

    @Test
    void returnsAnEmptySourcesArrayWhenRagUsedNoRetrievedContexts() throws Exception {
        when(ragService.answer("What is the notice period?", 5))
                .thenReturn(new RagResult("The information is unavailable in the provided documents.", true, List.of()));

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the notice period?\",\"topK\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is the notice period?"))
                .andExpect(jsonPath("$.citationValid").value(true))
                .andExpect(jsonPath("$.sources").isArray())
                .andExpect(jsonPath("$.sources").isEmpty());

        verify(ragService, times(1)).answer("What is the notice period?", 5);
    }

    @Test
    void rejectsMissingQuestion() throws Exception {
        rejectsInvalidRequest("{\"topK\":5}");
    }

    @Test
    void rejectsNullQuestion() throws Exception {
        rejectsInvalidRequest("{\"question\":null,\"topK\":5}");
    }

    @Test
    void rejectsBlankQuestion() throws Exception {
        rejectsInvalidRequest("{\"question\":\"   \",\"topK\":5}");
    }

    @Test
    void rejectsMissingTopK() throws Exception {
        rejectsInvalidRequest("{\"question\":\"What is the notice period?\"}");
    }

    @Test
    void rejectsNullTopK() throws Exception {
        rejectsInvalidRequest("{\"question\":\"What is the notice period?\",\"topK\":null}");
    }

    @Test
    void rejectsZeroTopK() throws Exception {
        rejectsInvalidRequest("{\"question\":\"What is the notice period?\",\"topK\":0}");
    }

    @Test
    void rejectsTopKAboveTheMaximum() throws Exception {
        rejectsInvalidRequest("{\"question\":\"What is the notice period?\",\"topK\":51}");
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the notice period?\",\"topK\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoRagExecution();
    }

    @Test
    void propagatesRagServiceFailuresThroughTheExistingExceptionHandler() throws Exception {
        when(ragService.answer("What is the notice period?", 5))
                .thenThrow(new RuntimeException("Ollama unavailable"));

        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"What is the notice period?\",\"topK\":5}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));

        verify(ragService, times(1)).answer("What is the notice period?", 5);
    }

    private void rejectsInvalidRequest(String content) throws Exception {
        mockMvc.perform(post("/api/rag/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoRagExecution();
    }

    private void verifyNoRagExecution() {
        verify(ragService, never()).answer(anyString(), anyInt());
    }

    private RagContext context(
            Long documentId,
            String documentName,
            int pageNumber,
            int chunkNumber,
            String content,
            double similarity) {

        return new RagContext(documentId, documentName, pageNumber, chunkNumber, content, similarity);
    }
}
