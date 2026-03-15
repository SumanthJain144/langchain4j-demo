package com.sumanth.demo;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * - MessageWindowChatMemory (keep last N messages)
 * - TokenWindowChatMemory (keep within token budget)
 * - @MemoryId for per-user memory isolation
 * - Memory injected into AI Service automatically
 */
public class Demo3ChatMemory {

    // AI Service that holds a SINGLE shared memory
    interface ChatBot {
        @SystemMessage("You are a friendly assistant. Remember everything the user tells you.")
        String chat(@UserMessage String message);
    }

    // AI Service with PER-USER memory (multi-user)
    interface MultiUserChatBot {
        @SystemMessage("You are a friendly assistant.")
        String chat(@MemoryId String userId, @UserMessage String message);
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Chat Memory");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        // No memory (stateless)
        System.out.println("\n--- WITHOUT memory (stateless) ---");
        ChatBot stateless = AiServices.create(ChatBot.class, model);

        System.out.println("[User] My name is Alice.");
        System.out.println("[Bot]  " + stateless.chat("My name is Alice."));
        System.out.println("[User] What's my name?");
        System.out.println("[Bot]  " + stateless.chat("What's my name?"));

        // MessageWindowChatMemory
        System.out.println("\n--- MessageWindowChatMemory (last 10 messages) ---");
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(10);

        ChatBot memoryBot = AiServices.builder(ChatBot.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .build();

        System.out.println("[User] My name is Alice and I love Java.");
        System.out.println("[Bot]  " + memoryBot.chat("My name is Alice and I love Java."));

        System.out.println("[User] What's my name?");
        System.out.println("[Bot]  " + memoryBot.chat("What's my name?"));

        System.out.println("[User] What programming language do I love?");
        System.out.println("[Bot]  " + memoryBot.chat("What programming language do I love?"));

        //TokenWindowChatMemory
        System.out.println("\n--- TokenWindowChatMemory (1000 token budget) ---");
        ChatMemory tokenMemory = TokenWindowChatMemory.builder()
                .maxTokens(1000, new OpenAiTokenizer("gpt-4o-mini"))
                .build();

        ChatBot tokenBot = AiServices.builder(ChatBot.class)
                .chatLanguageModel(model)
                .chatMemory(tokenMemory)
                .build();

        System.out.println("[User] I'm a backend developer with 8 years of Java experience.");
        System.out.println("[Bot]  " + tokenBot.chat("I'm a backend developer with 8 years of Java experience."));

        System.out.println("[User] What kind of developer am I?");
        System.out.println("[Bot]  " + tokenBot.chat("What kind of developer am I?"));

        //Per-user memory with @MemoryId
        System.out.println("\n--- Per-user memory isolation with @MemoryId ---");
        MultiUserChatBot multiBot = AiServices.builder(MultiUserChatBot.class)
                .chatLanguageModel(model)
                .chatMemoryProvider(userId ->
                        MessageWindowChatMemory.builder()
                                .id(userId)
                                .maxMessages(10)
                                .build())
                .build();

        // User Alice
        System.out.println("[Alice] My name is Alice.");
        System.out.println("[Bot→Alice] " + multiBot.chat("alice", "My name is Alice."));

        // User Bob (separate memory)
        System.out.println("[Bob]   My name is Bob.");
        System.out.println("[Bot→Bob]   " + multiBot.chat("bob", "My name is Bob."));

        // Alice remembers herself but NOT Bob
        System.out.println("[Alice] What's my name?");
        System.out.println("[Bot→Alice] " + multiBot.chat("alice", "What's my name?"));

        System.out.println("[Bob]   What's my name?");
        System.out.println("[Bot→Bob]   " + multiBot.chat("bob", "What's my name?"));

        System.out.println("\n Demo 3 complete.");
    }
}
