package com.sumanth.demo;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.model.output.Response;

import java.util.concurrent.CompletableFuture;

/**
 *   - OpenAiStreamingChatModel
 *   - StreamingResponseHandler<AiMessage>
 *   - onNext(token), onComplete(response), onError(error)
 */
public class Demo1Streaming {

    public static void main(String[] args) throws Exception {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Streaming Response");
        System.out.println("=".repeat(60));

        OpenAiStreamingChatModel streamingModel = OpenAiStreamingChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .temperature(0.8)
                .build();

        System.out.println("\n[Prompt] Tell me the features from java 21 to java 25. explain in detail");
        System.out.println("[Streaming response, token by token]\n");

        CompletableFuture<Response<AiMessage>> future = new CompletableFuture<>();

        streamingModel.generate("Tell me all the features from java 21 to java 25. explain in detail. in bullet points.",
                new StreamingResponseHandler<>() {

                    @Override
                    public void onNext(String token) {
                        // Each token arrives here as the LLM generates it
                        System.out.print(token);
                        System.out.flush();
                    }

                    @Override
                    public void onComplete(Response<AiMessage> response) {
                        System.out.println("\n");
                        System.out.println("[onComplete] Full response received.");
                        System.out.println("[TokenUsage] input=" + response.tokenUsage().inputTokenCount()
                                + " output=" + response.tokenUsage().outputTokenCount());
                        future.complete(response);
                    }

                    @Override
                    public void onError(Throwable error) {
                        System.err.println("\n[onError] " + error.getMessage());
                        future.completeExceptionally(error);
                    }
                });

        future.get(); // wait for streaming to finish
        System.out.println( "Demo complete.");
    }
}
