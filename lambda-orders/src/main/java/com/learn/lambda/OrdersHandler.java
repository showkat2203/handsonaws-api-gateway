package com.learn.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Orders Service — Phase 1
 *
 * Routes:
 *   GET  /orders   → list all orders
 *   POST /orders   → create a new order
 *
 * Key concepts demonstrated:
 *  - Single Lambda handling multiple HTTP methods (routing on httpMethod)
 *  - In-memory state: resets on Lambda cold start — intentional teaching point
 *    (Phase 2 will replace this with DynamoDB)
 */
public class OrdersHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson GSON = new Gson();

    // NOTE: static field → shared across warm invocations of the same instance,
    // reset on cold start. Great for observing Lambda lifecycle behaviour.
    private static final List<Map<String, Object>> ORDERS = new ArrayList<>();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        String method = event.getHttpMethod();
        context.getLogger().log(method + " /orders invoked");

        return switch (method) {
            case "GET"  -> listOrders();
            case "POST" -> createOrder(event, context);
            default     -> methodNotAllowed(method);
        };
    }

    private APIGatewayProxyResponseEvent listOrders() {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(GSON.toJson(ORDERS));
    }

    private APIGatewayProxyResponseEvent createOrder(
            APIGatewayProxyRequestEvent event, Context context) {

        if (event.getBody() == null || event.getBody().isBlank()) {
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(400)
                    .withHeaders(Map.of("Content-Type", "application/json"))
                    .withBody("{\"error\":\"Request body is required\"}");
        }

        Map<String, Object> order = GSON.fromJson(
                event.getBody(),
                new TypeToken<Map<String, Object>>() {}.getType());

        // Enrich with server-side fields
        order = new HashMap<>(order);          // make mutable
        order.put("orderId", UUID.randomUUID().toString());
        order.put("status", "PENDING");

        ORDERS.add(order);
        context.getLogger().log("Created order: " + order.get("orderId"));

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(201)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(GSON.toJson(order));
    }

    private APIGatewayProxyResponseEvent methodNotAllowed(String method) {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(405)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody("{\"error\":\"Method not allowed: " + method + "\"}");
    }
}
