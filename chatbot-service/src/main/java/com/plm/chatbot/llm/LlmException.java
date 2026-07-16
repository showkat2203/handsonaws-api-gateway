package com.plm.chatbot.llm;

/** Wraps failures talking to the LLM provider (network errors, non-200 responses). */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
