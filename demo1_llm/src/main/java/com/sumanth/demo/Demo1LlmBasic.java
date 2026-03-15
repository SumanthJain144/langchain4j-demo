package com.sumanth.demo;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;

/**
 *   - ChatLanguageModel interface
 *   - OpenAiChatModel.builder()
 *   - SystemMessage, UserMessage, AiMessage
 *   - Response<AiMessage> with token usage
 */
public class Demo1LlmBasic {

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — LLM Basic Request & Response");
        System.out.println("=".repeat(60));

        // Build the model
        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.7)
                .build();

        System.out.println("\n[Model] OpenAiChatModel ready (gpt-4o-mini)");

        // Simple string shorthand
        System.out.println("\n--- Method 1: Simple string generate ---");
        String simple = model.generate("What is LangChain4j in one sentence?");
        System.out.println("[Response] " + simple);

        //Explicit message types
        System.out.println("\n---Explicit message types ---");

        SystemMessage systemMessage = SystemMessage.from(
                "You are a concise Java expert. Reply in max 2 sentences.");

        UserMessage userMessage = UserMessage.from(
                "What is the difference between ChatModel and StreamingChatModel?");

        System.out.println("[SystemMessage] " + systemMessage.text());
        System.out.println("[UserMessage]   " + userMessage.singleText());

        Response<AiMessage> response = model.generate(systemMessage, userMessage);

        System.out.println("\n[AiMessage]     " + response.content().text());
        System.out.println("\n[TokenUsage]");
        System.out.println("  Input tokens:  " + response.tokenUsage().inputTokenCount());
        System.out.println("  Output tokens: " + response.tokenUsage().outputTokenCount());
        System.out.println("  Total tokens:  " + response.tokenUsage().totalTokenCount());
        System.out.println("[FinishReason]  " + response.finishReason());

        //conversation without chat memory
        System.out.println("\n---  Conversation without chat memory ---");

        var turn1User = UserMessage.from("My favourite language is Java.");
        var turn1Ai = model.generate(systemMessage, turn1User);
        System.out.println("[Turn 1 AI] " + turn1Ai.content().text());

        var turn2User = UserMessage.from("What's my favourite language?");
        var turn2Ai = model.generate(systemMessage, turn2User);
        System.out.println("[Turn 2 AI] " + turn2Ai.content().text());

        System.out.println("\n Demo 1a complete.");
    }
}
