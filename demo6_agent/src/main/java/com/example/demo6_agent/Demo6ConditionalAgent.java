package com.example.demo6_agent;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 *
 * Pattern: Classifier → route to the appropriate specialist agent.
 * Workflow:
 *   ClassifierAgent  → reads a support ticket and returns a category
 *   BillingAgent     → handles billing-related questions
 *   TechnicalAgent   → handles technical/API questions
 *   GeneralAgent     → handles everything else
 * Concepts shown:
 *   - A classifier AI service returning an enum (structured output)
 *   - Java if/switch routes to different agents (non-AI orchestration)
 *   - Each specialist has a focused @SystemMessage persona
 *   - Triage pattern used in real customer-support pipelines
 */
public class Demo6ConditionalAgent {

    enum TicketCategory { BILLING, TECHNICAL, GENERAL }

    //Classifier: returns an enum (structured output)
    interface ClassifierAgent {
        @SystemMessage("""
                You are a customer support ticket classifier.
                Classify the ticket into exactly one category:
                BILLING  — payment, invoice, subscription, refund, pricing
                TECHNICAL — API, error, integration, bug, code, performance
                GENERAL  — feature request, account info, how-to, other
                Respond with ONLY the category name, nothing else.
                """)
        TicketCategory classify(@UserMessage String ticketText);
    }

    // Specialist agents
    interface BillingAgent {
        @SystemMessage("""
                You are a billing support specialist.
                You handle payment issues, invoices, subscriptions, and refunds.
                Be empathetic, clear, and offer concrete next steps.
                Keep responses under 3 sentences.
                """)
        String handle(@UserMessage String issue);
    }

    interface TechnicalAgent {
        @SystemMessage("""
                You are a senior technical support engineer.
                You handle API integrations, bugs, performance issues, and code questions.
                Be precise, reference documentation where relevant.
                Keep responses under 3 sentences.
                """)
        String handle(@UserMessage String issue);
    }

    interface GeneralAgent {
        @SystemMessage("""
                You are a friendly customer success specialist.
                Handle general account questions, how-to requests, and feature inquiries.
                Be warm, helpful, and concise.
                Keep responses under 3 sentences.
                """)
        String handle(@UserMessage String issue);
    }

    public static void main(String[] args) {

        System.out.println("=".repeat(60));
        System.out.println("  DEMO — Conditional Agent Routing (Triage)");
        System.out.println("  Flow: Ticket → ClassifierAgent → Specialist");
        System.out.println("=".repeat(60));

        ChatLanguageModel model = OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();

        ClassifierAgent classifier = AiServices.create(ClassifierAgent.class, model);
        BillingAgent    billing    = AiServices.create(BillingAgent.class,    model);
        TechnicalAgent  technical  = AiServices.create(TechnicalAgent.class,  model);
        GeneralAgent    general    = AiServices.create(GeneralAgent.class,    model);

        String[] tickets = {
                "I was charged twice for my subscription this month. Please fix this.",
                "I'm getting a 429 rate limit error when calling the embeddings API at 3am.",
                "How do I add team members to my organisation account?",
                "The API response time has degraded from 200ms to 2s over the last week.",
                "I'd like a refund for last month — I didn't use the service at all."
        };

        for (String ticket : tickets) {
            System.out.println("\n" + "─".repeat(55));
            System.out.println("[Ticket]     " + ticket);

            //Step 1: Classify
            TicketCategory category = classifier.classify(ticket);
            System.out.println("[Category]   " + category);

            //Step 2: Route to specialist (non-AI Java switch)
            String response = switch (category) {
                case BILLING   -> { System.out.print("[→ BillingAgent]   "); yield billing.handle(ticket);   }
                case TECHNICAL -> { System.out.print("[→ TechnicalAgent] "); yield technical.handle(ticket); }
                case GENERAL   -> { System.out.print("[→ GeneralAgent]   "); yield general.handle(ticket);   }
            };

            System.out.println(response);
        }

        System.out.println("\nDemo — Conditional routing complete.");
    }
}
