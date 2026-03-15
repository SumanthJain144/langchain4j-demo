package com.example.demo6_agent;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Demo : Sequential Agent Orchestration
 * Pattern: Agent A → Agent B → Agent C
 * Output of one agent becomes the input to the next.
 * Workflow:
 * ResearchAgent   → gathers raw information on a topic
 * SummaryAgent    → condenses raw info into key points
 * TranslateAgent  → translates the summary into German
 * Concepts shown:
 * - Multiple specialized AI Services chained together
 * - Each agent has its own @SystemMessage persona
 * - Output of one is the @UserMessage of the next
 * - Non-AI orchestration code (plain Java) connects them
 */
public class Demo6SequentialAgent {

    //Agent 1: Research
    interface ResearchAgent {
        @SystemMessage("""
                You are a technical research agent.
                When given a topic, provide a thorough overview covering:
                - What it is
                - Why it matters
                - Key components or features
                - Common use cases
                Be detailed but factual. Write 3-4 paragraphs.
                """)
        String research(@UserMessage String topic);
    }

    //Agent 2: Summariser
    interface SummaryAgent {
        @SystemMessage("""
                You are a technical writer specialising in concise summaries.
                Given detailed research text, extract exactly 5 bullet points.
                Each bullet point should be one clear, actionable sentence.
                Format: • [bullet point]
                """)
        String summarise(@UserMessage String researchText);
    }

    //Agent 3: Translator
    interface TranslateAgent {
        @SystemMessage("""
                You are a professional technical translator.
                Translate the provided text into German.
                Preserve technical terms in English (e.g., "LangChain4j", "RAG", "API").
                """)
        String translateToGerman(@UserMessage String englishText);
    }

    //Shared tools (used by ResearchAgent)
    static class ResearchTools {
        @Tool("Look up current information about a Java/AI topic")
        String lookup(@P("The topic to look up") String topic) {
            // Simulated knowledge base — in production, call a real search API
            System.out.println("    [TOOL] lookup(\"" + topic + "\")");
            return """
                    From knowledge base about "%s":
                    LangChain4j is an open-source framework (Apache 2.0) for building
                    LLM-powered applications in Java. It provides abstractions for
                    chat models, embedding models, vector stores, RAG pipelines,
                    AI services with annotations, and agent orchestration.
                    Key integrations include OpenAI, Anthropic, Ollama, PgVector,
                    Chroma, Pinecone, Redis and Spring Boot.
                    """.formatted(topic);
        }
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Sequential Agent Orchestration");
        System.out.println("  Flow: Research → Summarise → Translate");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // Build each specialized agent
        ResearchAgent researcher = AiServices.builder(ResearchAgent.class)
                .chatLanguageModel(model)
                .tools(new ResearchTools())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(5))
                .build();

        SummaryAgent summariser = AiServices.create(SummaryAgent.class, model);

        TranslateAgent translator = AiServices.create(TranslateAgent.class, model);

        //Sequential pipeline
        String topic = "LangChain4j framework for Java developers";

        System.out.println("\n[Topic] " + topic);

        // Step 1: Research
        System.out.println("\n[Step 1 → ResearchAgent] Gathering information...");
        String rawResearch = researcher.research(topic);
        System.out.println("[Research Output]\n" + rawResearch);

        // Step 2: Summarise (receives Step 1 output)
        System.out.println("\n[Step 2 → SummaryAgent] Condensing to key points...");
        String summary = summariser.summarise(rawResearch);
        System.out.println("[Summary Output]\n" + summary);

        // Step 3: Translate (receives Step 2 output)
        System.out.println("\n[Step 3 → TranslateAgent] Translating to German...");
        String translated = translator.translateToGerman(summary);
        System.out.println("[German Output]\n" + translated);

        System.out.println("\nDemo complete.");
    }
}
