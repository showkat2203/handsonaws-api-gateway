package com.plm.chatbot.dto;

import com.plm.chatbot.executor.ToolCallTrace;

import java.util.List;

/** Response body for POST /chat: the composed answer plus a trace of every tool call made. */
public record ChatResponse(String answer, List<ToolCallTrace> toolCalls) {

    public ChatResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }
}
