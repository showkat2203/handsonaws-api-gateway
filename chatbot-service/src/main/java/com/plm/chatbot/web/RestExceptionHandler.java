package com.plm.chatbot.web;

import com.plm.service.exception.AuthorizationException;
import com.plm.service.exception.NotFoundException;
import com.plm.service.exception.ValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/** Structured JSON error responses for /chat and any other REST endpoints in this service. */
@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> notFound(NotFoundException e) {
        return body(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Object> validation(ValidationException e) {
        return body(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<Object> authorization(AuthorizationException e) {
        return body(HttpStatus.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(com.plm.chatbot.llm.LlmException.class)
    public ResponseEntity<Object> llmFailure(com.plm.chatbot.llm.LlmException e) {
        return body(HttpStatus.BAD_GATEWAY, "The chatbot's LLM provider request failed: " + e.getMessage());
    }

    private ResponseEntity<Object> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
