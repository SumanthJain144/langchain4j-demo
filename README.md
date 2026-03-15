# LangChain4j Demos
### "From RESTful APIs to AI Agents" — Meetup Talk Code

A collection of ** standalone demos** , each illustrating
a core LangChain4j concept. Every demo runs independently with a single Gradle command.

---

## Prerequisites

| Requirement | Version |
|---|---|
| JDK | 17 or higher (21 recommended) |
| Gradle | included via `./gradlew` wrapper |
| OpenAI API Key | paid account — set as env variable |

```bash
export OPENAI_API_KEY=sk-...
```

---

## Running the Demos

### Demo 1 — LLM Request & Response

```bash
# Basic request/response — ChatModel, messages, token usage
./gradlew :demo1_llm:run -Pmain=com.sumanth.demo.Demo1LlmBasic

# Streaming — token-by-token output
./gradlew :demo1_llm:run -Pmain=com.sumanth.demo.Demo1Streaming
```

**What you'll see:**
- SystemMessage, UserMessage, AiMessage types
- Token usage in console output
- Multi-turn conversation (manual history)
- Streaming tokens printed one by one

---

### Demo 2 — High-Level AI Services

```bash
./gradlew :demo2_aiservice:run
```

**What you'll see:**
- `@SystemMessage` sets the LLM persona
- `@UserMessage` templates the prompt
- `@V("name")` injects parameters into templates
- Return type `SentimentResult` record (structured output)
- Return type `List<String>` (keyword extraction)

---

### Demo 3 — Chat Memory

```bash
./gradlew :demo3_memory:run
```

**What you'll see:**
- Without memory: LLM forgets between calls
- `MessageWindowChatMemory`: keeps last N messages
- `TokenWindowChatMemory`: keeps within token budget
- `@MemoryId`: per-user isolated memory (Alice vs Bob)

---

### Demo 4 — Tools & Function Calling

```bash
./gradlew :demo4_tools:run
```

**What you'll see:**
- `[TOOL CALLED]` lines show when the LLM invokes Java methods
- LLM deciding TO use a tool (date, weather, math)
- LLM deciding NOT to use a tool (capital of France)
- LLM chaining multiple tool calls in one turn

---

### Demo 5 — RAG (Retrieval Augmented Generation)

```bash
./gradlew :demo5_rag:run
```

**What you'll see:**
- Documents split into `TextSegment`s
- Embedded with `AllMiniLmL6V2EmbeddingModel` (runs locally, no extra key)
- Retriever finds top-3 relevant segments per question
- LLM answers FROM the docs, or says "I don't have that information"

---

### Demo 6 — Agent Orchestration

```bash
# Sequential: Research → Summarise → Translate
./gradlew :demo6_agent:run -Pmain=com.example.demo6_agent.Demo6SequentialAgent

# Parallel: ProAgent + ConAgent concurrently → JudgeAgent
./gradlew :demo6_agent:run -Pmain=com.example.demo6_agent.Demo6ParallelAgent

# Loop: Generate → Review → Fix (self-correction)
./gradlew :demo6_agent:run -Pmain=com.example.demo6_agent.Demo6LoopAgent

# Conditional: Triage → route to Billing / Technical / General agent
./gradlew :demo6_agent:run -Pmain=com.example.demo6_agent.Demo6ConditionalAgent
```

**What you'll see:**
- `[Step N →]` labels showing which agent is running
- Multi-agent pipelines producing richer outputs than a single LLM call
- Loop iterations with self-correction between rounds
- Classifier enum output routing to specialist agents

---


## Key LangChain4j Classes by Demo

| Demo | Core Classes |
|------|-------------|
| 1 — LLM | `ChatLanguageModel`, `OpenAiChatModel`, `UserMessage`, `SystemMessage`, `AiMessage`, `StreamingResponseHandler` |
| 2 — AI Services | `AiServices`, `@AiService`, `@SystemMessage`, `@UserMessage`, `@V` |
| 3 — Memory | `MessageWindowChatMemory`, `TokenWindowChatMemory`, `@MemoryId`, `ChatMemoryStore` |
| 4 — Tools | `@Tool`, `@P`, `ToolExecutionRequest`, `ToolExecutionResultMessage` |
| 5 — RAG | `Document`, `DocumentSplitters`, `EmbeddingModel`, `InMemoryEmbeddingStore`, `EmbeddingStoreContentRetriever` |
| 6 — Agents | `AiServices` (multiple), `CompletableFuture`, Java `while` loop, `switch` expression |

---

## References

- 🌐 Interactive docs: https://chat.langchain4j.dev/
- 📘 Official docs: https://docs.langchain4j.dev
- 💻 GitHub: https://github.com/langchain4j/langchain4j

