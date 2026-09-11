# Performance notes

## Before

The local application included Redis final-answer caching and Kafka plus a transactional outbox for document processing. Measurements identified Ollama generation, rather than PostgreSQL/pgvector retrieval, as the material latency bottleneck.

## After

The local architecture is Spring Boot, PostgreSQL + pgvector, local file storage, PDF processing, `nomic-embed-text`, and `llama3.2:3b`; Redis, Kafka, and the outbox have been removed. PDF uploads synchronously reach `READY` or `FAILED` before the existing response is returned.

Production remains `llama3.2:3b` with `nomic-embed-text` and retrieval `topK=5`. `ollama.llm.keep-alive=10m` bounds model residency, and `ollama.llm.max-output-tokens=192` remains configurable.

No post-generation cache is used. `[RAG-PERF]` logs separately report embedding, vector search, document lookup, context build, prompt build, LLM generation, citation validation, and total request time.

### Measured local run (2026-09-09)

With PostgreSQL 17.5 and local Ollama (`nomic-embed-text`, `llama3.2:3b`), after explicitly unloading the LLM before the first request: cold RAG was **42192 ms**; three warm runs averaged **4062.7 ms** (min **2745 ms**, max **6609 ms**). The cold run spent **41642 ms** generating; warm request components were about 52-65 ms for embedding, 6-7 ms for vector search, 5-6 ms for document lookup, and 0-1 ms for context/prompt construction. Warm LLM generation was 2667-6539 ms. LLM generation, not pgvector, is the meaningful bottleneck.

The pre-change numerical baseline is not available, so no before/after latency claim is made. The final live full-RAG evaluation had Recall@5 **100%**, page hit rate **100%**, average first relevant rank **1.00**, answer accuracy **100%**, citation validity **75%**, and grounded answer rate **75%**. The resignation answer did not pass citation validation, so citation adherence remains the principal quality risk for the local 3B model.

The pgvector query remains exact cosine similarity. Do not add HNSW without a corpus-size benchmark and a versioned migration.
