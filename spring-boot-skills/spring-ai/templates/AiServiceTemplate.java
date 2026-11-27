package com.example.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class AiServiceTemplate {

    // This example assumes the builder has no stateful memory/session advisor.
    // If one is configured, pass an authorized session ID on every call.
    private final ChatClient chatClient;

    public AiServiceTemplate(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Value("classpath:prompts/analyze-order.st")
    private Resource analyzePrompt;

    /** Parses structured output; validate it before using it for a business decision. */
    public OrderAnalysis analyzeOrder(String customerTenureSegment, String items, String total) {
        return chatClient.prompt()
            .user(u -> u
                .text(analyzePrompt)
                .param("customerTenureSegment", customerTenureSegment)
                .param("items", items)
                .param("total", total))
            .call()
            .entity(OrderAnalysis.class);
    }

    public record OrderAnalysis(String riskLevel, String summary, List<String> recommendations) {
        public OrderAnalysis {
            if (riskLevel == null || !Set.of("LOW", "MEDIUM", "HIGH").contains(riskLevel)) {
                throw new IllegalArgumentException("Unsupported risk level");
            }
            if (summary == null || summary.isBlank()) {
                throw new IllegalArgumentException("Summary is required");
            }
            recommendations = List.copyOf(Objects.requireNonNull(recommendations));
            if (recommendations.size() > 3) {
                throw new IllegalArgumentException("At most three recommendations are allowed");
            }
        }
    }
}
