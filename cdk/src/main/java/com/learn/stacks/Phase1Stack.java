package com.learn.stacks;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.ApiKey;
import software.amazon.awscdk.services.apigateway.GatewayResponse;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodDeploymentOptions;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.Period;
import software.amazon.awscdk.services.apigateway.QuotaSettings;
import software.amazon.awscdk.services.apigateway.Resource;
import software.amazon.awscdk.services.apigateway.ResponseType;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.apigateway.StageOptions;
import software.amazon.awscdk.services.apigateway.ThrottleSettings;
import software.amazon.awscdk.services.apigateway.UsagePlan;
import software.amazon.awscdk.services.apigateway.UsagePlanPerApiStage;
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.Map;

/**
 * Phase 1 + Phase 2 — API Gateway Lambda basics + Throttling & Rate Limiting
 *
 * Throttling hierarchy (broadest → narrowest):
 *
 *   AWS Account limit  (10 000 RPS / 5 000 burst) ← AWS default ceiling, unchanged
 *       ↓
 *   Stage default      (   50 RPS /   100 burst)  ← our API ceiling
 *       ↓
 *   Method overrides:
 *     GET  /products   (   30 RPS /    50 burst)  ← read-heavy, generous
 *     GET  /orders     (   20 RPS /    40 burst)
 *     POST /orders     (   10 RPS /    20 burst)  ← writes, stricter
 *       ↓
 *   Usage Plan (per API-key client):
 *     basic-client     (    5 RPS /    10 burst,    500 req/day)
 *     premium-client   (   50 RPS /   100 burst, 10 000 req/day)
 *
 * Key insight: a request must pass BOTH the method throttle (global)
 * AND the usage-plan throttle (per-client). The stricter one wins.
 */
public class Phase1Stack extends Stack {

    public Phase1Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // ================================================================== //
        // Lambda functions                                                    //
        // ================================================================== //

        Function productsLambda = Function.Builder.create(this, "ProductsFunction")
                .functionName("learn-apigw-products")
                .runtime(Runtime.JAVA_21)
                .handler("com.learn.lambda.ProductsHandler")
                .code(Code.fromAsset("../lambda-products/target/lambda-products-1.0.0.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .build();

        Function ordersLambda = Function.Builder.create(this, "OrdersFunction")
                .functionName("learn-apigw-orders")
                .runtime(Runtime.JAVA_21)
                .handler("com.learn.lambda.OrdersHandler")
                .code(Code.fromAsset("../lambda-orders/target/lambda-orders-1.0.0.jar"))
                .memorySize(512)
                .timeout(Duration.seconds(30))
                .build();

        // ================================================================== //
        // Access Log Group                                                   //
        // One JSON line per request — captures 200, 403, 429, 500 all.      //
        // ================================================================== //

        LogGroup accessLogs = LogGroup.Builder.create(this, "ApiAccessLogs")
                .logGroupName("/aws/apigateway/learn-apigw-access")
                .retention(RetentionDays.ONE_WEEK)   // auto-delete after 7 days → $0 storage cost
                .build();

        // ================================================================== //
        // REST API — Phase 2: stage + method-level throttling                //
        // ================================================================== //

        RestApi api = RestApi.Builder.create(this, "LearnApiGw")
                .restApiName("learn-apigw")
                .description("Learning AWS API Gateway — Phase 2: Throttling & Rate Limiting")
                .deployOptions(StageOptions.builder()
                        .stageName("dev")

                        // --- Layer 2: Stage-level defaults ---
                        // NOTE: must stay BELOW Lambda account concurrency (10 on free tier)
                        // so API GW throttles before Lambda does → clean 429 instead of 500
                        .throttlingRateLimit(10)   // 10 steady-state RPS
                        .throttlingBurstLimit(5)   // 5 concurrent burst

                        // --- Layer 3: Method-level overrides ---
                        // Keys follow the pattern: /{resource-path}/{HTTP_METHOD}
                        .methodOptions(Map.of(

                            "/products/GET", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(8)   // reads: slightly generous
                                    .throttlingBurstLimit(4)
                                    .build(),

                            "/orders/GET", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(5)
                                    .throttlingBurstLimit(3)
                                    .build(),

                            "/orders/POST", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(3)   // writes: strict
                                    .throttlingBurstLimit(2)
                                    .build()
                        ))

                        // --- Access logs: one JSON line per request ---
                        .accessLogDestination(new LogGroupLogDestination(accessLogs))
                        .accessLogFormat(AccessLogFormat.custom(
                                "{\"requestId\":\"$context.requestId\"" +
                                ",\"timestamp\":\"$context.requestTime\"" +
                                ",\"method\":\"$context.httpMethod\"" +
                                ",\"path\":\"$context.resourcePath\"" +
                                ",\"status\":$context.status" +
                                ",\"responseMs\":$context.responseLatency" +
                                ",\"apiKeyId\":\"$context.identity.apiKeyId\"" +
                                ",\"ip\":\"$context.identity.sourceIp\"" +
                                ",\"errorType\":\"$context.error.responseType\"" +
                                ",\"errorMsg\":\"$context.error.message\"}"
                        ))
                        .build())
                .build();

        // ================================================================== //
        // Routes — Phase 2: require API key on all methods                   //
        // ================================================================== //

        Resource products = api.getRoot().addResource("products");
        products.addMethod("GET", new LambdaIntegration(productsLambda),
                MethodOptions.builder().apiKeyRequired(true).build());

        Resource orders = api.getRoot().addResource("orders");
        orders.addMethod("GET", new LambdaIntegration(ordersLambda),
                MethodOptions.builder().apiKeyRequired(true).build());
        orders.addMethod("POST", new LambdaIntegration(ordersLambda),
                MethodOptions.builder().apiKeyRequired(true).build());

        // ================================================================== //
        // Phase 2: Custom Gateway Responses                                  //
        //                                                                    //
        // Without these, API GW returns 500 "Internal server error" when     //
        // throttling. With them, clients get a proper 429 + clear message.   //
        // ================================================================== //

        GatewayResponse.Builder.create(this, "ThrottledResponse")
                .restApi(api)
                .type(ResponseType.THROTTLED)
                .statusCode("429")
                .responseHeaders(Map.of(
                        "gatewayresponse.header.Content-Type", "'application/json'"
                ))
                .templates(Map.of(
                        "application/json",
                        "{\"error\":\"Too Many Requests\"," +
                        "\"message\":\"Rate limit exceeded. Retry after a moment.\"," +
                        "\"limit\":\"$context.identity.apiKeyId\"}"
                ))
                .build();

        GatewayResponse.Builder.create(this, "QuotaExceededResponse")
                .restApi(api)
                .type(ResponseType.QUOTA_EXCEEDED)
                .statusCode("429")
                .responseHeaders(Map.of(
                        "gatewayresponse.header.Content-Type", "'application/json'"
                ))
                .templates(Map.of(
                        "application/json",
                        "{\"error\":\"Quota Exceeded\"," +
                        "\"message\":\"Daily request quota exhausted. Try again tomorrow.\"}"
                ))
                .build();

        // ================================================================== //
        // Phase 2: API Keys                                                  //
        // ================================================================== //

        ApiKey basicKey = ApiKey.Builder.create(this, "BasicApiKey")
                .apiKeyName("basic-client")
                .description("Basic tier client — 5 RPS / 500 req per day")
                .build();

        ApiKey premiumKey = ApiKey.Builder.create(this, "PremiumApiKey")
                .apiKeyName("premium-client")
                .description("Premium tier client — 50 RPS / 10 000 req per day")
                .build();

        // ================================================================== //
        // Phase 2: Usage Plans (Layer 4 — per-client throttle + quota)       //
        // ================================================================== //

        // --- Basic Plan ---
        UsagePlan basicPlan = UsagePlan.Builder.create(this, "BasicUsagePlan")
                .name("basic")
                .description("Low-volume clients")
                .throttle(ThrottleSettings.builder()
                        .rateLimit(5)    // 5 RPS per client
                        .burstLimit(10)
                        .build())
                .quota(QuotaSettings.builder()
                        .limit(10)           // LOW: for quota-exhaustion demo
                        .period(Period.DAY)  // resets daily
                        .build())
                .build();

        basicPlan.addApiStage(UsagePlanPerApiStage.builder()
                .api(api)
                .stage(api.getDeploymentStage())
                .build());
        basicPlan.addApiKey(basicKey);

        // --- Premium Plan ---
        UsagePlan premiumPlan = UsagePlan.Builder.create(this, "PremiumUsagePlan")
                .name("premium")
                .description("High-volume clients")
                .throttle(ThrottleSettings.builder()
                        .rateLimit(50)    // 50 RPS per client
                        .burstLimit(100)
                        .build())
                .quota(QuotaSettings.builder()
                        .limit(10000)
                        .period(Period.DAY)
                        .build())
                .build();

        premiumPlan.addApiStage(UsagePlanPerApiStage.builder()
                .api(api)
                .stage(api.getDeploymentStage())
                .build());
        premiumPlan.addApiKey(premiumKey);

        // ================================================================== //
        // Outputs                                                             //
        // ================================================================== //

        CfnOutput.Builder.create(this, "ApiUrl")
                .value(api.urlForPath("/"))
                .description("API Gateway base URL")
                .build();

        // API key values are never exposed in CloudFormation for security.
        // Use the CLI command in the description to retrieve the actual value.
        CfnOutput.Builder.create(this, "BasicApiKeyId")
                .value(basicKey.getKeyId())
                .description("Run: aws apigateway get-api-key --api-key <id> --include-value --query value --output text")
                .build();

        CfnOutput.Builder.create(this, "PremiumApiKeyId")
                .value(premiumKey.getKeyId())
                .description("Run: aws apigateway get-api-key --api-key <id> --include-value --query value --output text")
                .build();
    }
}
