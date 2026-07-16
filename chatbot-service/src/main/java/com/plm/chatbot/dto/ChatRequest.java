package com.plm.chatbot.dto;

import java.util.List;

/** Request body for POST /chat. */
public record ChatRequest(String message, List<ChatMessageDto> history) {

    public ChatRequest {
        history = history == null ? List.of() : List.copyOf(history);
    }
}
