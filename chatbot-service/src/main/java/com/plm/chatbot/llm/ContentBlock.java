package com.plm.chatbot.llm;

import java.util.Map;

/** A single content block within an LLM message, modeled after Anthropic's Messages API content blocks. */
public sealed interface ContentBlock {

    record Text(String text) implements ContentBlock {
    }

    /** The model asking to invoke a tool. */
    record ToolUse(String id, String name, Map<String, Object> input) implements ContentBlock {
    }

    /** Our reply feeding a tool's result back to the model. */
    record ToolResult(String toolUseId, String content, boolean isError) implements ContentBlock {
    }
}
