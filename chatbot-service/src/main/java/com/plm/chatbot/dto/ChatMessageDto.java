package com.plm.chatbot.dto;

/** One turn of prior conversation history supplied by the client. Role is "user" or "assistant". */
public record ChatMessageDto(String role, String content) {
}
