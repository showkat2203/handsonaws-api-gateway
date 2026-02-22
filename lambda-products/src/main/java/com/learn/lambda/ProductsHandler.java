package com.learn.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

/**
 * Products Service — Phase 1
 *
 * Route:  GET /products
 * Returns a static product catalog as JSON.
 *
 * Key concepts demonstrated:
 *  - Lambda proxy integration with API Gateway
 *  - APIGatewayProxyRequestEvent / APIGatewayProxyResponseEvent contract
 */
public class ProductsHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Gson GSON = new Gson();

    // Static catalog — in a real service this would come from DynamoDB, RDS, etc.
    private static final List<Map<String, Object>> CATALOG = List.of(
            Map.of("id", "p-001", "name", "Laptop",      "price", 999.99, "category", "Electronics"),
            Map.of("id", "p-002", "name", "Headphones",  "price", 149.99, "category", "Electronics"),
            Map.of("id", "p-003", "name", "Coffee Mug",  "price",  12.99, "category", "Kitchen")
    );

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {

        context.getLogger().log("GET /products invoked");

        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(Map.of("Content-Type", "application/json"))
                .withBody(GSON.toJson(CATALOG));
    }
}
