package com.plm.chatbot.llm;

import java.util.List;

/** A single turn in the conversation sent to/received from the LLM. Role is "user" or "assistant". */
public record LlmMessage(String role, List<ContentBlock> content) {

    public static LlmMessage userText(String text) {
        return new LlmMessage("user", List.of(new ContentBlock.Text(text)));
    }

    public static LlmMessage assistant(List<ContentBlock> content) {
        return new LlmMessage("assistant", content);
    }

    public static LlmMessage userToolResults(List<ContentBlock> toolResults) {
        return new LlmMessage("user", toolResults);
    }
}
