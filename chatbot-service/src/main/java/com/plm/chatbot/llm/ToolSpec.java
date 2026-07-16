package com.plm.chatbot.llm;

import java.util.Map;

/** Provider-agnostic tool declaration: name, natural-language description, and a JSON Schema for its params. */
public record ToolSpec(String name, String description, Map<String, Object> inputSchema) {
}
