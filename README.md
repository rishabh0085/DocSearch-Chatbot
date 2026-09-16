# DocSearch-Chatbot
 AI-Powered Enterprise Search Platform

DocSearch-Chatbot is a full-stack AI-powered enterprise document search platform that enables users to upload PDF documents, perform semantic search, and ask natural-language questions over their documents.

The system uses **Retrieval-Augmented Generation (RAG)** to retrieve relevant document content and provide grounded AI responses with document and page-level citations.

It runs locally using **Ollama**, making the AI pipeline completely local without requiring external LLM APIs or API keys.

---

## 🚀 Features

### 📄 Document Management
- Upload PDF documents
- Extract text page-by-page using Apache PDFBox
- Automatically split documents into intelligent, page-aware chunks
- Track document processing status
- Display document metadata including page count and file size

### 🔎 Semantic Search
- Generate vector embeddings using `nomic-embed-text`
- Store embeddings using PostgreSQL + pgvector
- Perform cosine-similarity vector search
- Retrieve the most relevant document chunks for a query
- Page-aware search results with document and chunk information

### 🤖 RAG-powered AI Answers
- Natural-language question answering over uploaded documents
- Retrieves relevant context before generating an answer
- Uses local LLM inference through Ollama
- Answers are grounded in retrieved document content
- Provides document and page-level citations
- Prevents unsupported answers when relevant information is unavailable

### 💬 Conversational AI Interface
- ChatGPT-style conversational interface
- Casual conversation support
- Document-based question answering
- AI-generated document summaries
- Source citations
- Copy AI responses
- Edit and regenerate user messages
- Text-to-speech for AI responses
- One-at-a-time speech playback

### 📊 RAG Evaluation
Includes an evaluation framework for measuring:

- Retrieval accuracy
- Recall@K
- Page-hit rate
- Average first relevant rank
- Answer accuracy
- Citation validity
- Grounded-answer rate

---

## 🏗️ Architecture

```text
                        ┌──────────────────────┐
                        │      React + TS      │
                        │       Frontend       │
                        └──────────┬───────────┘
                                   │
                                   │ REST API
                                   ▼
                        ┌──────────────────────┐
                        │    Spring Boot      │
                        │       Backend       │
                        └──────────┬───────────┘
                                   │
                 ┌─────────────────┼─────────────────┐
                 │                 │                 │
                 ▼                 ▼                 ▼
        ┌────────────────┐ ┌───────────────┐ ┌────────────────┐
        │ PostgreSQL +   │ │ Apache PDFBox │ │    Ollama      │
        │    pgvector    │ │               │ │                │
        └────────────────┘ └───────────────┘ │ llama3.2:3b    │
                                             │ nomic-embed-text│
                                             └────────────────┘



## 🔄 RAG Pipeline

The core document question-answering pipeline works as follows:

PDF Upload
    │
    ▼
Text Extraction
    │
    ▼
Page-aware Chunking
    │
    ▼
Embedding Generation
    │
    ▼
PostgreSQL + pgvector
    │
    ▼
User Question
    │
    ▼
Query Normalization
    │
    ▼
Query Embedding
    │
    ▼
Vector Similarity Search
    │
    ▼
Top-K Relevant Chunks
    │
    ▼
Context Construction
    │
    ▼
Grounded RAG Prompt
    │
    ▼
Local LLM (Ollama)
    │
    ▼
Answer + Citations


# Screenshots

![image alt](https://github.com/rishabh0085/DocSearch-Chatbot/blob/main/Screenshot%20(293).png?raw=true)
