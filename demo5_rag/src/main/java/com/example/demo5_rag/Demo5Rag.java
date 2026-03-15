package com.example.demo5_rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

/**
 *   - Document → DocumentSplitter → TextSegment
 *   - EmbeddingModel (local, no API key needed for embedding)
 *   - InMemoryEmbeddingStore
 *   - EmbeddingStoreContentRetriever
 *   - AI Service with contentRetriever()
 *   - Compare: answer with vs without RAG context
 */
public class Demo5Rag {

    interface DocumentQA {
        @SystemMessage("""
                You are a helpful assistant that answers questions based on the provided documents.
                If the answer is not in the documents, say "I don't have that information."
                Always be concise and factual.
                """)
        String ask(@UserMessage String question);
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — RAG (Retrieval Augmented Generation)");
        System.out.println("=".repeat(60));

        //Step 1: Create documents (simulating loaded files)
        System.out.println("\n[Step 1] Creating knowledge base documents...");

        Document doc1 = Document.from("""
                LangChain4j Release Notes - Version 0.36.0
                
                New Features:
                - MCP (Model Context Protocol) client support added.
                - Tool filtering via McpToolProvider.toolFilter() predicate.
                - TokenWindowChatMemory now supports custom tokenizers.
                - EmbeddingStoreIngestor supports batch ingestion with parallelism.
                - New guardrails: @InputGuardrail and @OutputGuardrail annotations.
                
                Bug Fixes:
                - Fixed NullPointerException in MessageWindowChatMemory when memory is empty.
                - Corrected token counting for GPT-4o models.
                - OpenAiStreamingChatModel now properly handles timeout configuration.
                
                Breaking Changes:
                - ChatModel interface: generate(String) shorthand now returns String directly.
                - Minimum Java version raised from 11 to 17.
                """);

        Document doc2 = Document.from("""
                LangChain4j Getting Started Guide
                
                Prerequisites:
                - JDK 17 or higher (JDK 21 recommended for virtual threads)
                - Gradle 8.x or Maven 3.9+
                - An OpenAI API key (or any supported LLM provider)
                
                Quick Start:
                Add to build.gradle:
                  implementation 'dev.langchain4j:langchain4j:0.36.0'
                  implementation 'dev.langchain4j:langchain4j-open-ai:0.36.0'
                
                Create your first AI service:
                1. Define an interface annotated with @AiService methods.
                2. Call AiServices.builder(YourInterface.class).chatLanguageModel(model).build().
                3. Call your interface method — that's it!
                
                Supported LLM Providers:
                OpenAI, Anthropic (Claude), Google Gemini, Azure OpenAI,
                Amazon Bedrock, Ollama (local), HuggingFace, Mistral AI,
                Cohere, and many more.
                """);

        Document doc3 = Document.from("""
                LangChain4j Architecture Overview
                
                Core Modules:
                - langchain4j: Core abstractions and utilities.
                - langchain4j-open-ai: OpenAI provider integration.
                - langchain4j-spring-boot-starter: Spring Boot auto-configuration.
                
                Memory Implementations:
                - InMemoryChatMemoryStore: Simple in-process storage.
                - RedisChatMemoryStore: Redis-backed persistent memory.
                - Custom: Implement ChatMemoryStore interface.
                
                Vector Stores:
                - InMemoryEmbeddingStore: Development and testing.
                - PgVectorEmbeddingStore: PostgreSQL with pgvector extension.
                - ChromaEmbeddingStore: Chroma vector database.
                - PineconeEmbeddingStore: Pinecone cloud service.
                - MilvusEmbeddingStore: Milvus vector database.
                
                Security Considerations:
                - Always store API keys in environment variables, never in code.
                - Use @InputGuardrail to block prompt injection attacks.
                - Apply metadata filters in RAG to enforce document-level access control.
                """);

        System.out.println("  Loaded 3 documents: release notes, getting started guide, architecture overview");

        //Step 2: Split into chunks
        System.out.println("\n[Step 2] Splitting documents into chunks...");
        DocumentSplitter splitter = DocumentSplitters.recursive(300, 30);

        List<TextSegment> segments = List.of(doc1, doc2, doc3)
                .stream()
                .flatMap(doc -> splitter.split(doc).stream())
                .toList();

        System.out.println("  Split into " + segments.size() + " text segments");

        //Step 3: Embed
        System.out.println("\n[Step 3] Embedding segments with text-embedding-3-small...");
        EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("text-embedding-3-small")
                .build();
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);
        System.out.println("  Embedded and stored " + embeddings.size() + " vectors");

        //Step 4: Build retriever
        System.out.println("\n[Step 4] Building content retriever (top-3 results)...");
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(3)
                .minScore(0.3)
                .build();

        //Step 5: Build AI Service with RAG
        System.out.println("\n[Step 5] Building AI Service with RAG retriever...");
        ChatLanguageModel llm = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        DocumentQA qa = AiServices.builder(DocumentQA.class)
                .chatLanguageModel(llm)
                .contentRetriever(retriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        //Step 6: Ask questions
        System.out.println("\n[Step 6] Asking questions against the knowledge base...\n");

        String[] questions = {
                "What are the new features in LangChain4j 0.36.0?",
                "What is the minimum Java version required?",
                "What vector stores does LangChain4j support?",
                "How do I store API keys securely?",
                "What is the capital of France?"  // Not in the docs!
        };

        for (String q : questions) {
            System.out.println("[Q] " + q);
            System.out.println("[A] " + qa.ask(q));
            System.out.println();
        }

        System.out.println("Demo 5 complete.");
    }
}
