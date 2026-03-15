package com.sumanth.demo;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.List;

/**
 * - @AiService interface
 * - @SystemMessage — sets LLM persona/rules
 * - @UserMessage — templates the user prompt
 * - @V("name") — injects method params into templates
 * - Structured output (record return type)
 * - List<T> return type
 */
public class Demo2AiService {

    //AI Service interfaces

    interface SimpleAssistant {
        @SystemMessage("You are a helpful Java expert. Be concise.")
        String chat(@UserMessage String question);
    }

    interface Translator {
        @SystemMessage("You are a professional translator.")
        @UserMessage("Translate the following text to {{language}}:\n\n{{text}}")
        String translate(@V("text") String text, @V("language") String language);
    }

    //Structured output
    record SentimentResult(String label, int score, String reasoning) {
    }

    interface SentimentAnalyzer {
        @SystemMessage("""
                You are a sentiment analysis engine.
                Always respond with valid JSON matching:
                { "label": "POSITIVE|NEUTRAL|NEGATIVE", "score": 1-10, "reasoning": "..." }
                """)
        SentimentResult analyze(@UserMessage String text);
    }

    // Keyword extractor returning List<String>
    interface KeywordExtractor {
        @SystemMessage("Extract 5 technical keywords from the text. Return a JSON array of strings.")
        List<String> extract(@UserMessage String text);
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO: High-Level AI Services");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        //Simple chat
        System.out.println("\n--- @SystemMessage + @UserMessage ---");
        SimpleAssistant assistant = AiServices.create(SimpleAssistant.class, model);
        String answer = assistant.chat("What is a Java record and when should I use it?");
        System.out.println("[Q] What is a Java record?");
        System.out.println("[A] " + answer);

        // @V template parameters
        System.out.println("\n--- @V template injection ---");
        Translator translator = AiServices.create(Translator.class, model);
        String translated = translator.translate(
                "LangChain4j makes building AI applications easy for Java developers.",
                "French");
        System.out.println("[EN] LangChain4j makes building AI applications easy...");
        System.out.println("[FR] " + translated);

        //Structured output → Java record
        System.out.println("\n--- Structured output (record return type) ---");
        SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, model);
        SentimentResult sentiment = analyzer.analyze(
                "LangChain4j is absolutely fantastic! It saved me weeks of work.");
        System.out.println("[Input] LangChain4j is absolutely fantastic! ...");
        System.out.println("[Label]     " + sentiment.label());
        System.out.println("[Score]     " + sentiment.score() + "/10");
        System.out.println("[Reasoning] " + sentiment.reasoning());

        //List<String> return
        System.out.println("\n--- List<String> return type ---");
        KeywordExtractor extractor = AiServices.create(KeywordExtractor.class, model);
        List<String> keywords = extractor.extract(
                "LangChain4j provides RAG pipelines, AI services, chat memory, " +
                        "and tool integration for Java developers building LLM applications.");
        System.out.println("[Keywords] " + keywords);

        System.out.println("Demo complete.");
    }
}
