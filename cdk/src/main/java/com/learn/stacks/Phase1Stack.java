package com.learn.stacks;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

// API Gateway
import software.amazon.awscdk.services.apigateway.AccessLogFormat;
import software.amazon.awscdk.services.apigateway.ApiKey;
import software.amazon.awscdk.services.apigateway.ConnectionType;
import software.amazon.awscdk.services.apigateway.GatewayResponse;
import software.amazon.awscdk.services.apigateway.HttpIntegration;
import software.amazon.awscdk.services.apigateway.HttpIntegrationProps;
import software.amazon.awscdk.services.apigateway.IntegrationOptions;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.LogGroupLogDestination;
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
import software.amazon.awscdk.services.apigateway.VpcLink;

// EC2 / VPC
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;

// ECS / Fargate
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LoadBalancerTargetOptions;
import software.amazon.awscdk.services.ecs.LogDrivers;
import software.amazon.awscdk.services.ecs.PortMapping;

// NLB
import software.amazon.awscdk.services.elasticloadbalancingv2.AddNetworkTargetsProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;

// Lambda
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;

// CloudWatch Logs
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import software.constructs.Construct;

import java.util.List;
import java.util.Map;

/**
 * Unified CDK stack covering all phases of the HandsOn AWS API Gateway project.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * Phase 1 — Lambda Proxy Integration
 *   REST API → Lambda (Products, Orders)
 *
 * Phase 2 — Throttling & Rate Limiting
 *   4-layer throttle hierarchy:
 *     AWS account limit (10 000 RPS / 5 000 burst)
 *       ↓ Stage default     (10 RPS / 5 burst)
 *       ↓ Method overrides  (per route)
 *       ↓ Usage Plans       (per API key client)
 *   API Keys, custom gateway responses (THROTTLED / QUOTA_EXCEEDED)
 *   Access logs → CloudWatch (one JSON line per request)
 *
 * Phase 3 — ECS Fargate + NLB + VPC Link
 *   Private User Service in VPC, zero internet exposure.
 *   REST API → VPC Link → NLB (internal) → ECS Fargate (User Service)
 *   VPC: 2 public subnets, no NAT Gateway ($0 extra cost)
 *   ECS tasks use public IPs to pull from ECR (avoids NAT cost)
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class Phase1Stack extends Stack {

    public Phase1Stack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // ================================================================== //
        // Phase 1: Lambda functions                                          //
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
        // Phase 2: Access Log Group                                          //
        // One JSON line per request — captures every status code (200/429…) //
        // ================================================================== //

        LogGroup accessLogs = LogGroup.Builder.create(this, "ApiAccessLogs")
                .logGroupName("/aws/apigateway/learn-apigw-access")
                .retention(RetentionDays.ONE_WEEK)
                .build();

        // ================================================================== //
        // REST API                                                            //
        // Phase 2: stage + method-level throttling + access logs             //
        // ================================================================== //

        RestApi api = RestApi.Builder.create(this, "LearnApiGw")
                .restApiName("learn-apigw")
                .description("HandsOn AWS — API Gateway (Phase 1 + 2 + 3)")
                .deployOptions(StageOptions.builder()
                        .stageName("dev")

                        // Layer 2: stage defaults (keep below Lambda concurrency on free tier)
                        .throttlingRateLimit(10)
                        .throttlingBurstLimit(5)

                        // Layer 3: per-method throttle overrides
                        .methodOptions(Map.of(
                            "/products/GET", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(8).throttlingBurstLimit(4).build(),
                            "/orders/GET", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(5).throttlingBurstLimit(3).build(),
                            "/orders/POST", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(3).throttlingBurstLimit(2).build(),
                            "/users/GET", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(8).throttlingBurstLimit(4).build(),
                            "/users/POST", MethodDeploymentOptions.builder()
                                    .throttlingRateLimit(5).throttlingBurstLimit(3).build()
                        ))

                        // Access logs: every request, every status code
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
        // Phase 1: Routes — Lambda integrations                              //
        // Phase 2: API key required on all methods                           //
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
        // Without these, throttled/quota requests return a generic 500.     //
        // With them, clients get a clear 429 with a helpful message.        //
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
        // Phase 2: Usage Plans (Layer 4 — per-client throttle + quota)      //
        // ================================================================== //

        UsagePlan basicPlan = UsagePlan.Builder.create(this, "BasicUsagePlan")
                .name("basic")
                .description("Low-volume clients")
                .throttle(ThrottleSettings.builder()
                        .rateLimit(5)
                        .burstLimit(10)
                        .build())
                .quota(QuotaSettings.builder()
                        .limit(10)
                        .period(Period.DAY)
                        .build())
                .build();

        basicPlan.addApiStage(UsagePlanPerApiStage.builder()
                .api(api).stage(api.getDeploymentStage()).build());
        basicPlan.addApiKey(basicKey);

        UsagePlan premiumPlan = UsagePlan.Builder.create(this, "PremiumUsagePlan")
                .name("premium")
                .description("High-volume clients")
                .throttle(ThrottleSettings.builder()
                        .rateLimit(50)
                        .burstLimit(100)
                        .build())
                .quota(QuotaSettings.builder()
                        .limit(10000)
                        .period(Period.DAY)
                        .build())
                .build();

        premiumPlan.addApiStage(UsagePlanPerApiStage.builder()
                .api(api).stage(api.getDeploymentStage()).build());
        premiumPlan.addApiKey(premiumKey);

        // ================================================================== //
        // Phase 3: VPC                                                       //
        //                                                                    //
        // Public subnets only, natGateways=0 → $0 extra cost.               //
        // ECS tasks get public IPs to pull Docker images from ECR directly  //
        // (without a NAT Gateway). The User Service itself remains private  //
        // — only reachable through API Gateway via the VPC Link.            //
        // ================================================================== //

        Vpc vpc = Vpc.Builder.create(this, "LearnVpc")
                .vpcName("learn-apigw-vpc")
                .maxAzs(2)
                .natGateways(0)   // no NAT = $0/hr saved
                .subnetConfiguration(List.of(
                        SubnetConfiguration.builder()
                                .name("public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        // ================================================================== //
        // Phase 3: ECS Cluster + Task Definition + Fargate Service          //
        // ================================================================== //

        Cluster cluster = Cluster.Builder.create(this, "LearnCluster")
                .clusterName("learn-apigw-cluster")
                .vpc(vpc)
                .build();

        // Cheapest Fargate config: 0.25 vCPU, 512 MB
        FargateTaskDefinition taskDef = FargateTaskDefinition.Builder.create(this, "UserServiceTaskDef")
                .cpu(256)
                .memoryLimitMiB(512)
                .build();

        LogGroup userServiceLogs = LogGroup.Builder.create(this, "UserServiceLogs")
                .logGroupName("/ecs/learn-apigw-user-service")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // ContainerImage.fromAsset: CDK builds the Dockerfile and pushes to ECR automatically
        taskDef.addContainer("UserServiceContainer",
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromAsset("../user-service"))
                        .portMappings(List.of(
                                PortMapping.builder().containerPort(8080).build()
                        ))
                        .logging(LogDrivers.awsLogs(AwsLogDriverProps.builder()
                                .streamPrefix("user-service")
                                .logGroup(userServiceLogs)
                                .build()))
                        .build());

        // Security group: allow traffic from within the VPC (NLB health checks + data)
        SecurityGroup ecsSg = SecurityGroup.Builder.create(this, "UserServiceSg")
                .vpc(vpc)
                .description("User Service - allow HTTP from within VPC")
                .build();
        ecsSg.addIngressRule(
                Peer.ipv4(vpc.getVpcCidrBlock()),
                Port.tcp(8080),
                "NLB health checks and forwarded traffic"
        );

        // ECS Service — assignPublicIp=true lets the task pull its ECR image
        // without a NAT Gateway (public subnet + public IP → direct internet access)
        FargateService userService = FargateService.Builder.create(this, "UserServiceFargate")
                .serviceName("learn-apigw-user-service")
                .cluster(cluster)
                .taskDefinition(taskDef)
                .desiredCount(1)
                .assignPublicIp(true)   // required when natGateways=0
                .securityGroups(List.of(ecsSg))
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .build();

        // ================================================================== //
        // Phase 3: NLB (internal)                                            //
        //                                                                    //
        // internetFacing=false → no public DNS, no public IP.               //
        // The NLB is only reachable from within the VPC or via VPC Link.    //
        // ================================================================== //

        NetworkLoadBalancer nlb = NetworkLoadBalancer.Builder.create(this, "UserServiceNlb")
                .loadBalancerName("learn-apigw-user-nlb")
                .vpc(vpc)
                .internetFacing(false)   // internal — the whole point of VPC Link
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PUBLIC)
                        .build())
                .build();

        NetworkListener nlbListener = nlb.addListener("UserServiceListener",
                BaseNetworkListenerProps.builder()
                        .port(80)
                        .build());

        // Register ECS tasks as NLB targets (IP mode — required for Fargate)
        nlbListener.addTargets("UserServiceTargets",
                AddNetworkTargetsProps.builder()
                        .port(8080)
                        .targets(List.of(userService.loadBalancerTarget(
                                LoadBalancerTargetOptions.builder()
                                        .containerName("UserServiceContainer")
                                        .containerPort(8080)
                                        .build())))
                        .deregistrationDelay(Duration.seconds(30))
                        .healthCheck(HealthCheck.builder()
                                .port("8080")
                                .protocol(Protocol.HTTP)
                                .path("/health")
                                .healthyHttpCodes("200")
                                .interval(Duration.seconds(30))
                                .healthyThresholdCount(2)
                                .unhealthyThresholdCount(3)
                                .build())
                        .build());

        // ================================================================== //
        // Phase 3: VPC Link                                                  //
        //                                                                    //
        // Bridges API Gateway (public AWS service) to the internal NLB      //
        // inside the VPC. API GW traverses AWS's private network —          //
        // the User Service never has a public address.                      //
        // ================================================================== //

        VpcLink vpcLink = VpcLink.Builder.create(this, "UserServiceVpcLink")
                .vpcLinkName("learn-apigw-user-link")
                .targets(List.of(nlb))
                .build();

        // ================================================================== //
        // Phase 3: /users routes — HTTP_PROXY via VPC Link                  //
        //                                                                    //
        // Unlike Lambda integration (sync invoke), this is a raw HTTP       //
        // proxy: API GW forwards the request as-is to the NLB endpoint.    //
        // ================================================================== //

        String usersBaseUrl = "http://" + nlb.getLoadBalancerDnsName() + "/users";

        IntegrationOptions vpcLinkOptions = IntegrationOptions.builder()
                .connectionType(ConnectionType.VPC_LINK)
                .vpcLink(vpcLink)
                .build();

        Resource users = api.getRoot().addResource("users");

        users.addMethod("GET",
                new HttpIntegration(usersBaseUrl,
                        HttpIntegrationProps.builder()
                                .proxy(true)
                                .httpMethod("GET")
                                .options(vpcLinkOptions)
                                .build()),
                MethodOptions.builder().apiKeyRequired(true).build());

        users.addMethod("POST",
                new HttpIntegration(usersBaseUrl,
                        HttpIntegrationProps.builder()
                                .proxy(true)
                                .httpMethod("POST")
                                .options(vpcLinkOptions)
                                .build()),
                MethodOptions.builder().apiKeyRequired(true).build());

        // ================================================================== //
        // Outputs                                                             //
        // ================================================================== //

        CfnOutput.Builder.create(this, "ApiUrl")
                .value(api.urlForPath("/"))
                .description("API Gateway base URL")
                .build();

        CfnOutput.Builder.create(this, "BasicApiKeyId")
                .value(basicKey.getKeyId())
                .description("Run: aws apigateway get-api-key --api-key <id> --include-value --query value --output text")
                .build();

        CfnOutput.Builder.create(this, "PremiumApiKeyId")
                .value(premiumKey.getKeyId())
                .description("Run: aws apigateway get-api-key --api-key <id> --include-value --query value --output text")
                .build();

        CfnOutput.Builder.create(this, "UserServiceNlbDns")
                .value(nlb.getLoadBalancerDnsName())
                .description("Internal NLB DNS — only reachable via VPC Link (not from internet)")
                .build();
    }
}
