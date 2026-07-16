package com.plm.chatbot.config;

import com.plm.chatbot.llm.AnthropicLlmProvider;
import com.plm.chatbot.llm.LlmProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Produces the {@link LlmProvider} bean. Kept behind the interface so the concrete provider is
 * swappable (a different vendor, or a stub for tests) without touching {@code ChatService}.
 */
@Configuration
public class LlmConfig {

    @Value("${anthropic.api-key:}")
    private String apiKey;

    @Value("${anthropic.model:claude-opus-4-8}")
    private String model;

    @Value("${anthropic.max-tokens:2048}")
    private int maxTokens;

    @Bean
    public LlmProvider llmProvider() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY is not set. The chatbot service requires a Claude API key.");
        }
        return new AnthropicLlmProvider(apiKey, model, maxTokens);
    }
}
