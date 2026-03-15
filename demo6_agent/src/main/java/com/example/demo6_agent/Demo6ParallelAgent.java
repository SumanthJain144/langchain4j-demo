package com.example.demo6_agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demo - Parallel Agent Orchestration
 * Pattern: Run multiple specialized agents concurrently, merge results.
 * Workflow:
 * ProAgent  → argues FOR a technology choice
 * ConAgent  → argues AGAINST it
 * JudgeAgent → reads both sides and delivers a balanced verdict
 * Concepts shown:
 * - CompletableFuture for concurrent agent execution
 * - Each agent is independent — no shared state
 * - A merge/aggregation agent processes combined outputs
 * - Plain Java orchestration (no special LangChain4j agent class needed)
 */
public class Demo6ParallelAgent {

    interface ProAgent {
        @SystemMessage("""
                You are a strong advocate for the technology you are given.
                Write 3 compelling arguments FOR adopting it.
                Be specific, technical, and persuasive.
                Format: numbered list.
                """)
        String argue(@UserMessage String technology);
    }

    interface ConAgent {
        @SystemMessage("""
                You are a critical analyst who finds risks and downsides.
                Write 3 strong arguments AGAINST adopting the given technology.
                Be specific, technical, and fair.
                Format: numbered list.
                """)
        String critique(@UserMessage String technology);
    }

    interface JudgeAgent {
        @SystemMessage("""
                You are a senior architect delivering a balanced technology assessment.
                Given arguments for and against, produce:
                - A 2-sentence executive summary
                - A final recommendation (Adopt / Trial / Hold / Avoid)
                - One key condition for the recommendation
                """)
        String judge(@UserMessage String bothSides);
    }

    public static void main(String[] args) throws Exception {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Parallel Agent Orchestration");
        System.out.println("  Flow: ProAgent ╗                        ");
        System.out.println("                 ╠══> JudgeAgent          ");
        System.out.println("        ConAgent ╝                        ");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        ProAgent proAgent = AiServices.create(ProAgent.class, model);
        ConAgent conAgent = AiServices.create(ConAgent.class, model);
        JudgeAgent judgeAgent = AiServices.create(JudgeAgent.class, model);

        String topic = "Adopting LangChain4j for production Java microservices";
        System.out.println("\n[Topic] " + topic);

        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        //Run ProAgent and ConAgent IN PARALLEL
        System.out.println("\n[Parallel] Launching ProAgent and ConAgent simultaneously...");
        long start = System.currentTimeMillis();

        CompletableFuture<String> proFuture = CompletableFuture.supplyAsync(
                () -> {
                    System.out.println("  [ProAgent starting...]");
                    String result = proAgent.argue(topic);
                    System.out.println("  [ProAgent done]");
                    return result;
                }, executor);

        CompletableFuture<String> conFuture = CompletableFuture.supplyAsync(
                () -> {
                    System.out.println("  [ConAgent starting...]");
                    String result = conAgent.critique(topic);
                    System.out.println("  [ConAgent done]");
                    return result;
                }, executor);

        // Wait for both to complete
        CompletableFuture.allOf(proFuture, conFuture).join();
        long elapsed = System.currentTimeMillis() - start;

        String pros = proFuture.get();
        String cons = conFuture.get();

        System.out.println("\n[Both agents completed in " + elapsed + "ms (parallel)]");

        System.out.println("\n[PRO Arguments]\n" + pros);
        System.out.println("\n[CON Arguments]\n" + cons);

        //JudgeAgent receives merged output
        System.out.println("\n[JudgeAgent] Evaluating both sides...");
        String combined = "ARGUMENTS FOR:\n" + pros + "\n\nARGUMENTS AGAINST:\n" + cons;
        String verdict = judgeAgent.judge(combined);
        System.out.println("\n[Verdict]\n" + verdict);

        executor.shutdown();
        System.out.println("\nDemo complete.");
    }
}
