package com.example.demo6_agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * Demo : Loop Agent — Self-Correction pattern
 * Pattern: Agent runs repeatedly until a condition is met.
 * Workflow:
 * CodeGeneratorAgent → writes Java code for a requirement
 * CodeReviewAgent    → reviews the code and returns PASS or issues
 * Loop continues until review passes OR max iterations reached
 * Concepts shown:
 * - Two specialized agents in a loop
 * - Non-AI condition check terminates the loop (Java code, not LLM)
 * - Self-correction: later iterations receive previous failures as context
 * - Max iteration guard to prevent infinite loops
 */
public class Demo6LoopAgent {

    interface CodeGeneratorAgent {
        @SystemMessage("""
                You are a senior Java developer.
                Write clean, production-quality Java 17+ code.
                - Use records where appropriate
                - Handle edge cases (null, empty)
                - Add brief Javadoc
                Return ONLY the Java code, no explanations, no markdown fences.
                """)
        String generate(@UserMessage String requirement);
    }

    interface CodeReviewAgent {
        @SystemMessage("""
                You are an expert Java code reviewer.
                Review the provided Java code strictly for:
                1. Null safety
                2. Edge case handling (empty collections, blank strings)
                3. Proper exception handling
                4. Java 17+ idioms (records, switch expressions, text blocks)
                
                Respond ONLY in one of two formats:
                  PASS
                  or
                  FAIL: [short comma-separated list of specific issues]
                
                Do NOT add explanations beyond the issues list.
                """)
        String review(@UserMessage String code);
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Loop Agent (Self-Correction)");
        System.out.println("  Flow: Generate → Review → [Fix if FAIL] → repeat");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        CodeGeneratorAgent generator = AiServices.create(CodeGeneratorAgent.class, model);
        CodeReviewAgent reviewer = AiServices.create(CodeReviewAgent.class, model);

        String requirement = """
                Write a Java utility method: 
                  String formatUserName(String firstName, String lastName)
                It should capitalise both names, trim whitespace, 
                and throw IllegalArgumentException if either is blank.
                """;

        System.out.println("\n[Requirement]\n" + requirement.strip());

        final int MAX_ITERATIONS = 4;
        String currentCode = null;
        String reviewResult = null;
        int iteration = 0;

        while (iteration < MAX_ITERATIONS) {
            iteration++;
            System.out.println("\n" + "─".repeat(50));
            System.out.println("[Iteration " + iteration + "]");

            //Generate (or re-generate with feedback)
            String prompt;
            if (currentCode == null) {
                prompt = requirement;
            } else {
                prompt = requirement + "\n\nPrevious attempt had issues: " + reviewResult
                        + "\n\nPlease fix those issues. Previous code:\n" + currentCode;
            }

            System.out.println("[CodeGeneratorAgent] Generating code...");
            currentCode = generator.generate(prompt);
            System.out.println("[Generated Code]\n" + currentCode);

            //Review
            System.out.println("\n[CodeReviewAgent] Reviewing...");
            reviewResult = reviewer.review(currentCode);
            System.out.println("[Review Result] " + reviewResult);

            //Loop termination condition (non-AI Java code)
            if (reviewResult.trim().toUpperCase().startsWith("PASS")) {
                System.out.println("\nCode passed review after " + iteration + " iteration(s)!");
                break;
            }

            if (iteration == MAX_ITERATIONS) {
                System.out.println("\nMax iterations reached. Last review: " + reviewResult);
            }
        }

        System.out.println("\n[Final Code]\n" + currentCode);
        System.out.println("\nDemo complete.");
    }
}
