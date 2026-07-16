package com.plm.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Bootable Spring Boot application for the chatbot. Runs as its own process (its own container
 * in docker-compose), independent of plm-api -- the "in-process" boundary in the architecture
 * spec refers to this process calling the plm-service Java interfaces directly, not to sharing
 * a JVM with the GraphQL API. Component-scans {@code com.plm} so it picks up plm-service's
 * repositories/services alongside its own chatbot beans.
 */
@SpringBootApplication(scanBasePackages = "com.plm")
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }
}
