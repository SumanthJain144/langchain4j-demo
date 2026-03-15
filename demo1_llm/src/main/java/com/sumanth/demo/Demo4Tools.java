package com.sumanth.demo;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

/**
 * - @Tool annotation — expose Java methods to the LLM
 * - @P annotation — document parameters for the LLM
 * - LLM decides WHEN and HOW to call tools
 * - Tool execution loop (internal to LangChain4j)
 * - Multiple tools in one AI service
 */
public class Demo4Tools {

    //Tool class: methods the LLM can invoke
    static class UtilityTools {

        @Tool("Get the current date")
        String getCurrentDate() {
            String date = LocalDate.now().toString();
            System.out.println("  [TOOL CALLED] getCurrentDate() → " + date);
            return date;
        }

        @Tool("Get the current time")
        String getCurrentTime() {
            String time = LocalTime.now().toString();
            System.out.println("  [TOOL CALLED] getCurrentTime() → " + time);
            return time;
        }

        @Tool("Get the current weather for a city")
        String getWeather(@P("The city name") String city) {
            // Simulated — replace with a real weather API call
            Map<String, String> weather = Map.of(
                    "Berlin", "18°C, partly cloudy",
                    "London", "12°C, rainy",
                    "Amsterdam", "15°C, overcast",
                    "New York", "22°C, sunny"
            );
            String result = weather.getOrDefault(city, "20°C, unknown conditions");
            System.out.println("  [TOOL CALLED] getWeather(\"" + city + "\") → " + result);
            return result;
        }

        @Tool("Calculate the square root of a number")
        double sqrt(@P("The number to calculate square root of") double number) {
            double result = Math.sqrt(number);
            System.out.println("  [TOOL CALLED] sqrt(" + number + ") → " + result);
            return result;
        }

        @Tool("Convert temperature from Celsius to Fahrenheit")
        double celsiusToFahrenheit(@P("Temperature in Celsius") double celsius) {
            double fahrenheit = celsius * 9.0 / 5.0 + 32;
            System.out.println("  [TOOL CALLED] celsiusToFahrenheit(" + celsius + ") → " + fahrenheit);
            return fahrenheit;
        }
    }

    //AI Service with tools
    interface ToolAssistant {
        @SystemMessage("""
                You are a helpful assistant with access to tools.
                Use tools when you need real-time data or calculations.
                Always explain what you found.
                """)
        String chat(@UserMessage String message);
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Tools & Function Calling");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        ToolAssistant assistant = AiServices.builder(ToolAssistant.class)
                .chatLanguageModel(model)
                .tools(new UtilityTools())
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();

        //Single tool call
        System.out.println("\n--- LLM triggers date tool ---");
        System.out.println("[User] What is today's date?");
        System.out.println("[Bot]  " + assistant.chat("What is today's date?"));

        //Tool with parameter
        System.out.println("\n--- LLM triggers tool with a parameter ---");
        System.out.println("[User] What's the weather like in Berlin right now?");
        System.out.println("[Bot]  " + assistant.chat("What's the weather like in Berlin right now?"));

        //LLM chains multiple tools
        System.out.println("\n--- LLM chains multiple tool calls ---");
        System.out.println("[User] What's the weather in London, and what is that in Fahrenheit?");
        System.out.println("[Bot]  " + assistant.chat(
                "What's the weather in London, and convert that temperature to Fahrenheit."));

        //LLM decides NOT to use a tool
        System.out.println("\n--- LLM decides no tool is needed ---");
        System.out.println("[User] What is the capital of France?");
        System.out.println("[Bot]  " + assistant.chat("What is the capital of France?"));

        //Math tool
        System.out.println("\n--- Math calculation via tool ---");
        System.out.println("[User] What is the square root of 144?");
        System.out.println("[Bot]  " + assistant.chat("What is the square root of 144?"));

        System.out.println("\n Demo complete.");
    }
}
