package com.learn.userservice;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Phase 3 — User Service
 *
 * A minimal HTTP server running in ECS Fargate, accessed via API Gateway VPC Link.
 *
 * Traffic path:
 *   Client → API GW → VPC Link → NLB (internal) → ECS Fargate (this service, port 8080)
 *
 * Key insight: the service has NO public endpoint. It lives entirely inside the VPC.
 * The only way to reach it from the internet is through API Gateway via the VPC Link.
 *
 * Uses JDK's built-in HttpServer — zero extra dependencies, immediate start.
 * Java 21 virtual threads handle concurrent requests efficiently.
 */
public class UserServiceApp {

    private static final Gson GSON = new Gson();

    // In-memory store — resets on ECS task restart (same as Lambda cold start)
    private static final List<Map<String, Object>> USERS = Collections.synchronizedList(
        new ArrayList<>(List.of(
            Map.of("userId", "u-001", "name", "Alice Johnson", "email", "alice@example.com", "tier", "premium"),
            Map.of("userId", "u-002", "name", "Bob Smith",    "email", "bob@example.com",   "tier", "basic")
        ))
    );

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Business routes
        server.createContext("/users", UserServiceApp::handleUsers);

        // NLB target group health check — must return 2xx for task to receive traffic
        server.createContext("/health", exchange ->
            send(exchange, 200, "{\"status\":\"healthy\",\"service\":\"user-service\"}")
        );

        // Java 21: virtual threads — cheap, non-blocking per-request threads
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.printf("User Service started on port %d%n", port);
    }

    private static void handleUsers(HttpExchange exchange) throws IOException {
        try {
            switch (exchange.getRequestMethod()) {
                case "GET"  -> handleGetUsers(exchange);
                case "POST" -> handlePostUser(exchange);
                default     -> send(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            send(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }

    // GET /users — return all users
    private static void handleGetUsers(HttpExchange exchange) throws IOException {
        String body = GSON.toJson(Map.of(
            "users", USERS,
            "count", USERS.size()
        ));
        send(exchange, 200, body);
    }

    // POST /users — create a user, return 201 with the new object
    private static void handlePostUser(HttpExchange exchange) throws IOException {
        String requestBody = new String(
            exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> input = GSON.fromJson(requestBody, Map.class);

        if (input == null || !input.containsKey("name") || !input.containsKey("email")) {
            send(exchange, 400, "{\"error\":\"name and email are required\"}");
            return;
        }

        Map<String, Object> newUser = new LinkedHashMap<>();
        newUser.put("userId", "u-" + String.format("%03d", USERS.size() + 1));
        newUser.put("name",   input.get("name"));
        newUser.put("email",  input.get("email"));
        newUser.put("tier",   input.getOrDefault("tier", "basic"));
        USERS.add(newUser);

        send(exchange, 201, GSON.toJson(newUser));
    }

    static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
