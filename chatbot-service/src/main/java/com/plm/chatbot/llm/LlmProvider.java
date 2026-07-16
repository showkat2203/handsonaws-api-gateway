package com.plm.chatbot.llm;

import java.util.List;

/**
 * Abstraction over the LLM backing the chatbot, so the provider is swappable. The default
 * implementation ({@code AnthropicLlmProvider}) calls the Claude API; a test double or a
 * different vendor can be substituted without touching {@code ChatService} or the tool layer.
 */
public interface LlmProvider {

    LlmResult complete(String systemPrompt, List<LlmMessage> messages, List<ToolSpec> tools);
}
