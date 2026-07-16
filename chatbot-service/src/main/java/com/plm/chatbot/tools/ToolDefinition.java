package com.plm.chatbot.tools;

import com.plm.chatbot.llm.ToolSpec;

import java.util.Map;
import java.util.function.Function;

/** A tool the LLM can call, pairing its JSON-schema spec with the function that executes it. */
public record ToolDefinition(ToolSpec spec, Function<Map<String, Object>, Object> execute) {

    public String name() {
        return spec.name();
    }
}
