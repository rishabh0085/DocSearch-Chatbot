package com.enterprise.search.evaluation;

import com.enterprise.search.service.RagResult;
import com.enterprise.search.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Live, manual-only model benchmark. Invoke once per installed model, for example:
 * {@code mvn test -Dtest=RagModelBenchmarkManual -Dollama.llm-model=llama3.2:1b}.
 * It intentionally does not end in Test, so the normal Maven suite never calls Ollama.
 * Stop the target model with {@code ollama stop <model>} immediately before invoking this
 * class to measure its cold request. The configured production default is never modified.
 */
@SpringBootTest
class RagModelBenchmarkManual {

    private static final String BENCHMARK_QUESTION = "What is the notice period for resignation?";
    private static final int WARM_RUNS = 3;

    @Autowired
    private RagService ragService;

    @Autowired
    private RagEvaluationService evaluationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ollama.llm-model}")
    private String model;

    @Value("${rag.evaluation.top-k:5}")
    private int topK;

    @Test
    void runsColdWarmAndExistingEvaluationDataset() throws Exception {
        System.out.printf("%n==== MODEL BENCHMARK: %s ====%n", model);
        System.out.println("Cold run is the first RAG request; unload the model with `ollama stop " + model
                + "` immediately before this command.");

        BenchmarkRun cold = run("cold");
        List<BenchmarkRun> warm = new ArrayList<>();
        for (int run = 1; run <= WARM_RUNS; run++) {
            warm.add(run("warm-" + run));
        }
        printRuns(cold, warm);

        List<RagEvaluationCase> cases = loadCases();
        printReport("RETRIEVAL EVALUATION", evaluationService.evaluateRetrieval(cases));
        printReport("FULL RAG EVALUATION", evaluationService.evaluateFullRag(cases));
    }

    private BenchmarkRun run(String name) {
        long started = System.nanoTime();
        RagResult result = ragService.answer(BENCHMARK_QUESTION, topK);
        long totalMillis = (System.nanoTime() - started) / 1_000_000;
        BenchmarkRun run = new BenchmarkRun(name, totalMillis, result.answer().length(), result.contexts().size(),
                result.citationValid(), !result.answer().isBlank());
        System.out.printf("BENCHMARK RUN %s: totalMs=%d, answerChars=%d, sources=%d, citationValid=%s, answerReturned=%s%n",
                run.name(), run.totalMillis(), run.answerChars(), run.sources(), run.citationValid(), run.answerReturned());
        return run;
    }

    private void printRuns(BenchmarkRun cold, List<BenchmarkRun> warm) {
        long min = warm.stream().mapToLong(BenchmarkRun::totalMillis).min().orElse(0);
        long max = warm.stream().mapToLong(BenchmarkRun::totalMillis).max().orElse(0);
        double average = warm.stream().mapToLong(BenchmarkRun::totalMillis).average().orElse(0);
        System.out.printf("BENCHMARK SUMMARY: coldTotalMs=%d, warmAvgMs=%.1f, warmMinMs=%d, warmMaxMs=%d%n",
                cold.totalMillis(), average, min, max);
        System.out.println("Component timings (embedding, vector search, document lookup, context, prompt, LLM, citation) are logged as [RAG-PERF] for every run.");
    }

    private List<RagEvaluationCase> loadCases() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/rag-evaluation.json")) {
            if (input == null) {
                throw new IllegalStateException("rag-evaluation.json was not found");
            }
            return objectMapper.readValue(input, new TypeReference<>() { });
        }
    }

    private void printReport(String title, RagEvaluationReport report) {
        System.out.printf("%n%s: retrievalAccuracy=%.1f%%, recallAt5=%.1f%%, pageHitRate=%.1f%%, firstRelevantRank=%.2f, answerAccuracy=%.1f%%, citationValidity=%.1f%%, groundedRate=%.1f%%%n",
                title, report.retrievalAccuracy() * 100, report.recallAtK() * 100, report.pageHitRate() * 100,
                report.averageFirstRelevantRank(), report.answerAccuracy() * 100, report.citationValidityRate() * 100,
                report.groundedAnswerRate() * 100);
        report.cases().forEach(result -> System.out.printf("CASE: %s | answer=%s | citation=%s | grounding=%s | rank=%s | %s%n",
                result.question(), result.answerPassed(), result.citationPassed(), result.groundingPassed(),
                result.firstRelevantRank(), result.notes()));
    }

    private record BenchmarkRun(String name, long totalMillis, int answerChars, int sources,
                                boolean citationValid, boolean answerReturned) { }
}
