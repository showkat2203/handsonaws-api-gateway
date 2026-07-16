package com.plm.chatbot.controller;

import com.plm.chatbot.dto.ChatRequest;
import com.plm.chatbot.dto.ChatResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * The chatbot's sole entry point. Deliberately separate from the GraphQL/UI layer: this REST
 * endpoint's tools call straight into the plm-service internal service layer via
 * {@link ChatService}, never through GraphQL.
 */
@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }
}
