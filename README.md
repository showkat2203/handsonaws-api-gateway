# Learn AWS API Gateway — Architecture Documentation

## Overview

A hands-on learning project that builds a mini e-commerce API on AWS using:
- **AWS CDK (Java)** — infrastructure as code
- **AWS Lambda (Java 21)** — serverless compute
- **AWS API Gateway REST API** — managed HTTP gateway
- **CloudWatch Logs** — observability

The project is structured in phases. Each phase adds a new layer of complexity on top of the previous one.

---

## Project Structure

```
LearnAWSBasics/
├── pom.xml                              ← Parent Maven POM (version management)
│
├── cdk/                                 ← CDK infrastructure (defines all AWS resources)
│   ├── cdk.json                         ← CDK entry point config
│   ├── pom.xml
│   └── src/main/java/com/learn/
│       ├── LearnAwsApp.java             ← CDK App entry point
│       └── stacks/
│           └── Phase1Stack.java         ← All AWS resources for Phase 1 + 2
│
├── lambda-products/                     ← Products microservice
│   ├── pom.xml                          ← Builds fat JAR via maven-shade-plugin
│   └── src/main/java/com/learn/lambda/
│       └── ProductsHandler.java
│
└── lambda-orders/                       ← Orders microservice
    ├── pom.xml
    └── src/main/java/com/learn/lambda/
        └── OrdersHandler.java
```

---

## Architecture Diagram

```
                        ┌─────────────────────────────────┐
                        │         Internet / Client        │
                        └──────────────┬──────────────────┘
                                       │  HTTPS
                                       │  x-api-key: <key>
                                       ▼
                        ┌─────────────────────────────────────────────────────────────┐
                        │             AWS API Gateway  (REST API)                      │
                        │                  Stage: dev                                  │
                        │                                                              │
                        │  ┌─── Request Pipeline (in order) ──────────────────────┐   │
                        │  │                                                       │   │
                        │  │  Step 1 — API Key check                               │   │
                        │  │           Missing/invalid key → 403 Forbidden         │   │
                        │  │                                                       │   │
                        │  │  Step 2 — Usage Plan (per-client)                     │   │
                        │  │           Quota exceeded  → 429 Quota Exceeded        │   │
                        │  │           Rate exceeded   → 429 Too Many Requests     │   │
                        │  │                                                       │   │
                        │  │  Step 3 — Stage throttle (global API ceiling)         │   │
                        │  │           Rate: 10 RPS  / Burst: 5                    │   │
                        │  │           Exceeded       → 429 Too Many Requests     │   │
                        │  │                                                       │   │
                        │  │  Step 4 — Method throttle (per route)                 │   │
                        │  │           GET /products  8 RPS / 4 burst             │   │
                        │  │           GET /orders    5 RPS / 3 burst             │   │
                        │  │           POST /orders   3 RPS / 2 burst             │   │
                        │  │                                                       │   │
                        │  │  Step 5 — Route + Lambda invoke                       │   │
                        │  │                                                       │   │
                        │  └───────────────────────────────────────────────────────┘   │
                        │              │                        │                       │
                        │        GET /products          GET /orders                    │
                        │                               POST /orders                   │
                        └──────────────┼────────────────────────┼──────────────────────┘
                                       │                        │
                          ┌────────────▼──────────┐  ┌─────────▼──────────┐
                          │   Lambda: products    │  │  Lambda: orders    │
                          │   Java 21 / 512 MB   │  │  Java 21 / 512 MB  │
                          │   ProductsHandler    │  │  OrdersHandler     │
                          └────────────┬──────────┘  └─────────┬──────────┘
                                       │                        │
                        ┌──────────────▼────────────────────────▼──────────────────────┐
                        │                    CloudWatch Logs                             │
                        │                                                               │
                        │  /aws/lambda/learn-apigw-products   ← Lambda execution logs  │
                        │  /aws/lambda/learn-apigw-orders     ← Lambda execution logs  │
                        │  /aws/apigateway/learn-apigw-access ← One line per request   │
                        └───────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

**Base URL:** `https://1s2r943y7i.execute-api.us-west-2.amazonaws.com/dev`

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| `GET` | `/products` | `ProductsHandler` | Returns product catalog |
| `GET` | `/orders` | `OrdersHandler` | Lists all orders |
| `POST` | `/orders` | `OrdersHandler` | Creates a new order |

All endpoints require the `x-api-key` header.

### Example Requests

```bash
BASE=https://1s2r943y7i.execute-api.us-west-2.amazonaws.com/dev

# List products
curl -H "x-api-key: <your-key>" $BASE/products

# Create an order
curl -X POST $BASE/orders \
  -H "x-api-key: <your-key>" \
  -H "Content-Type: application/json" \
  -d '{"productId":"p-001","quantity":2}'

# List orders
curl -H "x-api-key: <your-key>" $BASE/orders
```

### Response Codes

| Code | Meaning | Triggered by |
|------|---------|-------------|
| `200` | OK | Successful GET |
| `201` | Created | Successful POST /orders |
| `400` | Bad Request | POST /orders with empty body |
| `403` | Forbidden | Missing or invalid API key |
| `405` | Method Not Allowed | Unsupported HTTP method on a route |
| `429` | Too Many Requests | Quota exhausted or rate limit exceeded |
| `500` | Internal Server Error | Lambda concurrency throttle (free tier) |

---

## Throttling Architecture

API Gateway applies throttle checks in layers. A request must pass **all** layers to reach Lambda.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Layer 1 — AWS Account: Lambda Concurrency Limit                    │
│            10 concurrent executions on free tier (default: 1 000)  │
│            Excess → 500 Internal Server Error (not 429!)            │
├─────────────────────────────────────────────────────────────────────┤
│  Layer 2 — Stage Default Throttle  (applies to all methods)         │
│            Rate:  10 RPS                                            │
│            Burst:  5 concurrent                                     │
├─────────────────────────────────────────────────────────────────────┤
│  Layer 3 — Method Throttle  (per-route override)                    │
│            GET  /products  →  8 RPS / 4 burst                       │
│            GET  /orders    →  5 RPS / 3 burst                       │
│            POST /orders    →  3 RPS / 2 burst                       │
├─────────────────────────────────────────────────────────────────────┤
│  Layer 4 — Usage Plan  (per API key / per client)                   │
│                                                                     │
│   ┌────────────────────────────────────────────────────────────┐   │
│   │  basic-client key                                          │   │
│   │    Rate:  5 RPS   Burst: 10   Quota: 10 req/day           │   │
│   └────────────────────────────────────────────────────────────┘   │
│   ┌────────────────────────────────────────────────────────────┐   │
│   │  premium-client key                                        │   │
│   │    Rate: 50 RPS   Burst: 100  Quota: 10 000 req/day       │   │
│   └────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Key Insight: Free Tier Throttling Behavior

On a free-tier account, Lambda's concurrency limit (10) is **lower** than typical production API GW burst limits. This means Lambda throttles requests **before** API GW gets a chance to, resulting in `500` instead of `429`.

| Throttle Source | Response | Detected via |
|----------------|----------|-------------|
| API GW Stage/Method | `429 Too Many Requests` | CloudWatch `ThrottleCount` metric |
| API GW Usage Plan — rate | `429 Too Many Requests` | CloudWatch `4XXError` metric |
| API GW Usage Plan — quota | `429 Quota Exceeded` | CloudWatch `4XXError` metric |
| Lambda concurrency | `500 Internal Server Error` | CloudWatch `Lambda.Throttles` metric |

---

## Lambda Functions

### ProductsHandler

**File:** `lambda-products/src/main/java/com/learn/lambda/ProductsHandler.java`

```
Route:      GET /products
Runtime:    Java 21
Memory:     512 MB
Timeout:    30 seconds
Integration: Lambda Proxy (API GW passes full HTTP request, Lambda controls full response)
```

- Implements `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
- Returns a hardcoded product catalog as JSON
- Stateless — every invocation is identical
- `CATALOG` is a `static final` field initialized once per Lambda instance

**Response shape:**
```json
[
  { "id": "p-001", "name": "Laptop",     "price": 999.99, "category": "Electronics" },
  { "id": "p-002", "name": "Headphones", "price": 149.99, "category": "Electronics" },
  { "id": "p-003", "name": "Coffee Mug", "price":  12.99, "category": "Kitchen"     }
]
```

---

### OrdersHandler

**File:** `lambda-orders/src/main/java/com/learn/lambda/OrdersHandler.java`

```
Routes:     GET  /orders  — list all orders
            POST /orders  — create a new order
Runtime:    Java 21
Memory:     512 MB
Timeout:    30 seconds
Integration: Lambda Proxy
```

- Single Lambda handles both `GET` and `POST` by switching on `event.getHttpMethod()`
- Orders are stored in a `static List<Map<String, Object>>` — survives **warm** invocations, resets on **cold start**
- `POST` validates the request body, adds `orderId` (UUID) and `status: PENDING`

**Important:** The in-memory store is intentional for learning. It demonstrates Lambda's execution lifecycle (warm vs. cold start). In production, this would be DynamoDB or RDS.

**POST request body:**
```json
{ "productId": "p-001", "quantity": 2 }
```

**POST response (201 Created):**
```json
{ "productId": "p-001", "quantity": 2, "orderId": "uuid-...", "status": "PENDING" }
```

---

## Gateway Responses (Custom Error Shapes)

Without custom gateway responses, API GW returns `500 Internal server error` for throttled requests. We override two error types:

| Response Type | HTTP Status | Body |
|--------------|-------------|------|
| `THROTTLED` | 429 | `{"error":"Too Many Requests","message":"Rate limit exceeded..."}` |
| `QUOTA_EXCEEDED` | 429 | `{"error":"Quota Exceeded","message":"Daily request quota exhausted..."}` |

---

## Observability

### Access Logs — `/aws/apigateway/learn-apigw-access`

One JSON line per request, regardless of outcome (200, 403, 429, 500).
Retention: 7 days (auto-deleted, no ongoing cost).

**Log shape:**
```json
{
  "requestId":  "3ed5a793-...",
  "timestamp":  "22/Feb/2026:10:23:56 +0000",
  "method":     "GET",
  "path":       "/products",
  "status":     429,
  "responseMs": 8,
  "apiKeyId":   "379xhlii14",
  "ip":         "24.16.80.46",
  "errorType":  "QUOTA_EXCEEDED",
  "errorMsg":   "Limit Exceeded"
}
```

**Notable fields:**
- `responseMs: 8` on a 429 → Lambda was never invoked (API GW handled it entirely)
- `responseMs: 200+` on a 200 → Lambda cold start included in that time
- `errorType` → exact reason for failure (`QUOTA_EXCEEDED`, `THROTTLED`, `INVALID_API_KEY`)
- `apiKeyId: "-"` → request had no API key (403)

### Lambda Execution Logs — `/aws/lambda/learn-apigw-*`

One log stream per Lambda instance. Each invocation writes:
```
START RequestId: abc-123 Version: $LATEST
GET /products invoked          ← context.getLogger().log(...)
END RequestId: abc-123
REPORT RequestId: abc-123  Duration: 3ms  Billed: 4ms  Memory: 512MB  Max Used: 109MB
```

Cold starts additionally write:
```
Init Duration: 1234ms          ← JVM startup + class loading (Java-specific)
```

### Useful CLI Commands

```bash
# Stream live Lambda logs
aws logs tail /aws/lambda/learn-apigw-products --follow --format short

# Find all 429s in the last hour
aws logs filter-log-events \
  --log-group-name /aws/apigateway/learn-apigw-access \
  --start-time $(date -d '1 hour ago' +%s000) \
  --filter-pattern '{ $.status = 429 }'

# Count 4XX errors in last hour
aws cloudwatch get-metric-statistics \
  --namespace AWS/ApiGateway \
  --metric-name 4XXError \
  --dimensions Name=ApiName,Value=learn-apigw Name=Stage,Value=dev \
  --start-time $(date -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --period 3600 --statistics Sum \
  --query 'Datapoints[*].Sum' --output text

# Check Lambda throttle count
aws cloudwatch get-metric-statistics \
  --namespace AWS/Lambda \
  --metric-name Throttles \
  --dimensions Name=FunctionName,Value=learn-apigw-products \
  --start-time $(date -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ) \
  --end-time $(date -u +%Y-%m-%dT%H:%M:%SZ) \
  --period 3600 --statistics Sum \
  --query 'Datapoints[*].Sum' --output text

# Check daily quota usage for basic key
aws apigateway get-usage \
  --usage-plan-id <plan-id> \
  --key-id 379xhlii14 \
  --start-date $(date +%Y-%m-%d) \
  --end-date $(date -d '+1 day' +%Y-%m-%d)
```

### CloudWatch Log Insights Queries

Run in **CloudWatch → Logs → Log Insights**, select log group `/aws/apigateway/learn-apigw-access`:

```sql
-- All 429s
fields timestamp, method, path, status, apiKeyId, errorType, responseMs
| filter status = 429
| sort @timestamp desc

-- Slowest successful requests
fields timestamp, method, path, status, responseMs
| filter status = 200
| sort responseMs desc
| limit 20

-- Requests per minute by status code
fields status
| stats count() as requests by bin(1m), status
| sort @timestamp desc

-- Which API keys are hitting errors
fields apiKeyId, errorType
| filter status >= 400
| stats count() as errors by apiKeyId, errorType
| sort errors desc
```

---

## Infrastructure — AWS Resources Deployed

All resources defined in `Phase1Stack.java`, deployed via `cdk deploy`.

| Resource | Name / ID | Description |
|----------|-----------|-------------|
| Lambda Function | `learn-apigw-products` | Products microservice |
| Lambda Function | `learn-apigw-orders` | Orders microservice |
| IAM Role | `ProductsFunctionServiceRole` | Execution role for products Lambda |
| IAM Role | `OrdersFunctionServiceRole` | Execution role for orders Lambda |
| API Gateway REST API | `learn-apigw` | The gateway |
| API Gateway Stage | `dev` | Deployed stage with throttling config |
| API Gateway Deployment | _(auto-named)_ | Snapshot of API config at deploy time |
| Gateway Response | `THROTTLED` | Custom 429 for rate limit |
| Gateway Response | `QUOTA_EXCEEDED` | Custom 429 for quota |
| API Key | `basic-client` (ID: `379xhlii14`) | Low-volume client key |
| API Key | `premium-client` (ID: `imh8gqb2qj`) | High-volume client key |
| Usage Plan | `basic` | 5 RPS / 10 burst / 10 req/day |
| Usage Plan | `premium` | 50 RPS / 100 burst / 10 000 req/day |
| CloudWatch Log Group | `/aws/apigateway/learn-apigw-access` | Access logs (7-day retention) |
| CloudWatch Log Group | `/aws/lambda/learn-apigw-products` | Lambda logs |
| CloudWatch Log Group | `/aws/lambda/learn-apigw-orders` | Lambda logs |
| S3 Bucket | `cdk-bootstrap-*` | CDK asset staging (Lambda JARs) |
| CloudFormation Stack | `LearnApigwPhase1` | Parent stack for all resources |

---

## Build & Deploy

### Prerequisites

```bash
java --version      # OpenJDK 21
mvn --version       # Maven 3.6+
node --version      # Node 22 (via nvm)
cdk --version       # AWS CDK 2.x
aws sts get-caller-identity   # AWS credentials configured
```

### Build Lambda JARs

```bash
cd /home/sonnet/dev/LearnAWSBasics
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 \
  PATH=$JAVA_HOME/bin:$PATH \
  mvn package -pl lambda-products,lambda-orders
```

This produces:
- `lambda-products/target/lambda-products-1.0.0.jar` — fat JAR (~4 MB)
- `lambda-orders/target/lambda-orders-1.0.0.jar` — fat JAR (~4 MB)

### Deploy

```bash
cd cdk
source ~/.nvm/nvm.sh && nvm use 22
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# First time only (per account/region)
cdk bootstrap

# Deploy / update
cdk deploy --require-approval never
```

### Destroy (clean up)

```bash
cdk destroy
```

---

## Cost on AWS Free Tier

| Service | Free Tier Allowance | Our Usage |
|---------|-------------------|-----------|
| Lambda | 1M requests/month + 400K GB-s/month (permanent) | ~hundreds of test calls → $0 |
| API Gateway REST | 1M calls/month for 12 months | ~hundreds of test calls → $0 |
| S3 (CDK bootstrap) | 5 GB storage | ~10 MB (2 JARs) → $0 |
| CloudWatch Logs | 5 GB ingestion/month | Minimal → $0 |
| CloudFormation | Always free | $0 |
| IAM | Always free | $0 |

**Expected monthly cost for this setup: $0**

> **Warning for Phase 3:** Adding ALB (~$16/month) or NLB (~$16/month) or ECS Fargate will incur charges even on free tier. Always run `cdk destroy` after testing those phases.

---

## Phases Roadmap

| Phase | Status | Topics |
|-------|--------|--------|
| **Phase 1** | ✅ Complete | Lambda proxy integration, REST API, stages, routes |
| **Phase 2** | ✅ Complete | API Keys, Usage Plans, throttling layers, gateway responses, access logs |
| **Phase 3** | 🔜 Planned | User Service on ECS, ALB integration, VPC Link |
| **Phase 4** | 🔜 Planned | NLB integration, TCP routing, ALB vs NLB comparison |
| **Phase 5** | 🔜 Planned | Lambda authorizers, Cognito, WAF, caching, custom domains |
