package com.plm.chatbot.executor;

import java.util.Map;

/** A record of one tool invocation, returned to the client alongside the chat answer. */
public record ToolCallTrace(String tool, Map<String, Object> params, Object result, String error, long durationMs) {

    public static ToolCallTrace success(String tool, Map<String, Object> params, Object result, long durationMs) {
        return new ToolCallTrace(tool, params, result, null, durationMs);
    }

    public static ToolCallTrace failure(String tool, Map<String, Object> params, String error, long durationMs) {
        return new ToolCallTrace(tool, params, null, error, durationMs);
    }
}
