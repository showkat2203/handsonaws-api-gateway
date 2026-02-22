# HandsOn AWS API Gateway

A hands-on learning project that builds a mini e-commerce API on AWS, progressing from basic Lambda integration through throttling, rate limiting, and private microservices behind VPC Link.

**Stack:** AWS CDK (Java) · Lambda (Java 21) · ECS Fargate · API Gateway REST API · NLB · VPC Link

---

## Project Structure

```
handsonaws-api-gateway/
├── pom.xml                              ← Parent Maven POM (version management)
│
├── cdk/                                 ← CDK infrastructure (all AWS resources)
│   ├── cdk.json
│   └── src/main/java/com/learn/
│       ├── LearnAwsApp.java
│       └── stacks/
│           └── Phase1Stack.java         ← All phases in one CDK stack
│
├── lambda-products/                     ← Products Lambda (GET /products)
│   └── src/main/java/com/learn/lambda/
│       └── ProductsHandler.java
│
├── lambda-orders/                       ← Orders Lambda (GET /orders, POST /orders)
│   └── src/main/java/com/learn/lambda/
│       └── OrdersHandler.java
│
└── user-service/                        ← User Service (ECS Fargate, GET/POST /users)
    ├── Dockerfile
    └── src/main/java/com/learn/userservice/
        └── UserServiceApp.java
```

---

## Architecture

```
                       ┌──────────────────────────────────────────────────────────────┐
                       │                     Internet / Client                         │
                       └───────────────────────────┬──────────────────────────────────┘
                                                   │  HTTPS + x-api-key
                                                   ▼
                       ┌──────────────────────────────────────────────────────────────┐
                       │                API Gateway REST API  (stage: dev)             │
                       │                                                              │
                       │   Request Pipeline (applied in order):                       │
                       │     1. API Key check          → 403 if missing/invalid       │
                       │     2. Usage Plan (per-client) → 429 if quota/rate exceeded  │
                       │     3. Stage throttle          → 429 if > 10 RPS / 5 burst   │
                       │     4. Method throttle         → 429 if per-route limit hit  │
                       │     5. Integration invoke                                    │
                       │                                                              │
                       │   /products   /orders          /users                        │
                       └──────┬────────────┬────────────────┬─────────────────────────┘
                              │            │                │
                              │            │         VPC Link (private tunnel)
                              │            │                │
                   ┌──────────▼──┐  ┌──────▼──────┐        ▼
                   │  Lambda     │  │  Lambda     │  ┌─────────────────────────────────┐
                   │  products   │  │  orders     │  │         VPC (private)            │
                   │  Java 21    │  │  Java 21    │  │                                  │
                   └─────────────┘  └─────────────┘  │  ┌──────────────────────────┐   │
                                                     │  │  NLB (internal)          │   │
                                                     │  │  no public IP/DNS        │   │
                                                     │  └────────────┬─────────────┘   │
                                                     │               │                  │
                                                     │  ┌────────────▼─────────────┐   │
                                                     │  │  ECS Fargate             │   │
                                                     │  │  user-service  :8080     │   │
                                                     │  │  Java 21 virtual threads │   │
                                                     │  └──────────────────────────┘   │
                                                     └─────────────────────────────────┘
                       ┌──────────────────────────────────────────────────────────────┐
                       │                       CloudWatch Logs                         │
                       │  /aws/lambda/learn-apigw-products   ← Lambda logs            │
                       │  /aws/lambda/learn-apigw-orders     ← Lambda logs            │
                       │  /aws/apigateway/learn-apigw-access ← One JSON line/request  │
                       │  /ecs/learn-apigw-user-service      ← ECS container logs     │
                       └──────────────────────────────────────────────────────────────┘
```

---

## API Endpoints

**Base URL:** `https://1s2r943y7i.execute-api.us-west-2.amazonaws.com/dev`

All endpoints require the `x-api-key` header.

| Method | Path | Backend | Integration Type |
|--------|------|---------|-----------------|
| `GET` | `/products` | Lambda `learn-apigw-products` | Lambda Proxy |
| `GET` | `/orders` | Lambda `learn-apigw-orders` | Lambda Proxy |
| `POST` | `/orders` | Lambda `learn-apigw-orders` | Lambda Proxy |
| `GET` | `/users` | ECS Fargate via NLB | HTTP Proxy / VPC Link |
| `POST` | `/users` | ECS Fargate via NLB | HTTP Proxy / VPC Link |

### Example Requests

```bash
BASE=https://1s2r943y7i.execute-api.us-west-2.amazonaws.com/dev
KEY=<your-api-key>

# Products
curl -H "x-api-key: $KEY" $BASE/products

# Orders
curl -H "x-api-key: $KEY" $BASE/orders
curl -X POST $BASE/orders -H "x-api-key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"productId":"p-001","quantity":2}'

# Users
curl -H "x-api-key: $KEY" $BASE/users
curl -X POST $BASE/users -H "x-api-key: $KEY" \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice","email":"alice@example.com","tier":"premium"}'
```

### Response Codes

| Code | Meaning | Triggered by |
|------|---------|-------------|
| `200` | OK | Successful GET |
| `201` | Created | Successful POST |
| `400` | Bad Request | Missing required fields in POST body |
| `403` | Forbidden | Missing or invalid API key |
| `429` | Too Many Requests | Quota exhausted or rate limit exceeded |
| `500` | Internal Server Error | Lambda concurrency throttle (free tier) |

---

## Throttling Architecture (Phase 2)

Throttle checks are applied in layers. A request must pass **all** layers to reach the backend.

```
┌─────────────────────────────────────────────────────────────────────┐
│  Layer 1 — AWS Account: Lambda Concurrency Limit                    │
│            10 concurrent executions on free tier (default: 1 000)  │
│            Excess → 500 Internal Server Error (not 429!)            │
├─────────────────────────────────────────────────────────────────────┤
│  Layer 2 — Stage Default Throttle  (applies to all methods)         │
│            Rate:  10 RPS  /  Burst: 5                               │
├─────────────────────────────────────────────────────────────────────┤
│  Layer 3 — Method Throttle  (per-route override)                    │
│            GET  /products  →  8 RPS / 4 burst                       │
│            GET  /orders    →  5 RPS / 3 burst                       │
│            POST /orders    →  3 RPS / 2 burst                       │
│            GET  /users     →  8 RPS / 4 burst                       │
│            POST /users     →  5 RPS / 3 burst                       │
├─────────────────────────────────────────────────────────────────────┤
│  Layer 4 — Usage Plan  (per API key / per client)                   │
│                                                                     │
│   basic-client:    5 RPS  / 10 burst  /     10 req/day  (demo)      │
│   premium-client: 50 RPS  / 100 burst / 10 000 req/day              │
└─────────────────────────────────────────────────────────────────────┘
```

**Key insight — free tier throttling behavior:**

| Throttle Source | Response | Detected via |
|----------------|----------|-------------|
| API GW Stage/Method throttle | `429 Too Many Requests` | CloudWatch `ThrottleCount` |
| Usage Plan rate exceeded | `429 Too Many Requests` | CloudWatch `4XXError` |
| Usage Plan quota exceeded | `429 Quota Exceeded` | CloudWatch `4XXError` |
| Lambda concurrency limit | `500 Internal Server Error` | CloudWatch `Lambda.Throttles` |

---

## VPC Link — How It Works (Phase 3)

VPC Link is what makes the User Service private. Without it, you'd need to expose the ECS container to the internet.

```
Client
  → API Gateway (public AWS service, no VPC)
  → VPC Link (AWS-managed private tunnel into your VPC)
  → NLB (internal — no public IP, no internet access)
  → ECS Fargate task (User Service on port 8080)
```

**Why NLB, not ALB?**
REST API Gateway VPC Link requires a Network Load Balancer. NLB operates at Layer 4 (TCP), forwarding raw connections directly to ECS tasks.

**VPC design (zero NAT cost):**
- 2 public subnets across 2 AZs
- No NAT Gateway → `$0/hr` saved vs private subnets
- ECS tasks use `assignPublicIp=true` to pull Docker images from ECR directly
- The NLB is `internetFacing=false` — it has no public DNS, only a VPC-internal address
- The only way to reach the User Service from the internet is through API Gateway

---

## Services & Handlers

### ProductsHandler

**File:** `lambda-products/src/main/java/com/learn/lambda/ProductsHandler.java`

- Implements `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
- Returns a hardcoded product catalog as JSON
- `CATALOG` is `static final` — initialized once per Lambda instance, shared across warm invocations

### OrdersHandler

**File:** `lambda-orders/src/main/java/com/learn/lambda/OrdersHandler.java`

- Single Lambda handles `GET` and `POST` by switching on `event.getHttpMethod()`
- Orders stored in `static List<Map<String, Object>>` — survives warm invocations, resets on cold start
- `POST` validates body, appends `orderId` (UUID) and `status: PENDING`

### UserServiceApp

**File:** `user-service/src/main/java/com/learn/userservice/UserServiceApp.java`

- Built-in JDK `HttpServer` — no frameworks, no extra dependencies
- Java 21 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) — one cheap thread per request
- `GET /health` → NLB target group health check endpoint (must return 200)
- `GET /users` → returns all users
- `POST /users` → creates a user, returns 201
- In-memory store — resets when the ECS task restarts (same concept as Lambda cold start)

---

## Observability

### Access Logs — `/aws/apigateway/learn-apigw-access`

One JSON line per request, every status code.

```json
{
  "requestId":  "3ed5a793-...",
  "timestamp":  "22/Feb/2026:10:23:56 +0000",
  "method":     "GET",
  "path":       "/users",
  "status":     200,
  "responseMs": 12,
  "apiKeyId":   "imh8gqb2qj",
  "ip":         "24.16.80.46",
  "errorType":  "",
  "errorMsg":   ""
}
```

**Notable fields:**
- `responseMs: 8` on a 429 → Lambda/ECS never invoked, API GW rejected at the gate
- `responseMs: 1000+` on first `/users` 200 → ECS task warm-up included
- `errorType: QUOTA_EXCEEDED` → exact failure reason, no guessing

### ECS Container Logs — `/ecs/learn-apigw-user-service`

```
User Service started on port 8080
```

### Useful CLI Commands

```bash
# Stream live access logs
aws logs tail /aws/apigateway/learn-apigw-access --follow --format short

# Stream ECS user-service logs
aws logs tail /ecs/learn-apigw-user-service --follow --format short

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
  --period 3600 --statistics Sum

# Check daily quota usage for basic key
aws apigateway get-usage \
  --usage-plan-id <plan-id> \
  --key-id 379xhlii14 \
  --start-date $(date +%Y-%m-%d) \
  --end-date $(date -d '+1 day' +%Y-%m-%d)
```

### CloudWatch Log Insights Queries

Log group: `/aws/apigateway/learn-apigw-access`

```sql
-- All errors by type
fields timestamp, method, path, status, errorType, responseMs
| filter status >= 400
| sort @timestamp desc

-- Slowest successful requests (spot cold starts)
fields timestamp, method, path, status, responseMs
| filter status = 200
| sort responseMs desc
| limit 20

-- Requests per minute by status
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

## Infrastructure — All AWS Resources

All resources are defined in `Phase1Stack.java` and deployed as a single CloudFormation stack.

### Phase 1 + 2 (Lambda + Throttling)

| Resource | Name | Description |
|----------|------|-------------|
| Lambda Function | `learn-apigw-products` | Products microservice |
| Lambda Function | `learn-apigw-orders` | Orders microservice |
| API Gateway REST API | `learn-apigw` | The gateway |
| API Gateway Stage | `dev` | Stage with throttling config |
| Gateway Response | `THROTTLED` | Custom 429 for rate limit |
| Gateway Response | `QUOTA_EXCEEDED` | Custom 429 for quota |
| API Key | `basic-client` (ID: `379xhlii14`) | 5 RPS / 10 req/day |
| API Key | `premium-client` (ID: `imh8gqb2qj`) | 50 RPS / 10 000 req/day |
| Usage Plan | `basic` | Attached to basic-client key |
| Usage Plan | `premium` | Attached to premium-client key |
| Log Group | `/aws/apigateway/learn-apigw-access` | Access logs (7-day retention) |

### Phase 3 (ECS + NLB + VPC Link)

| Resource | Name | Description |
|----------|------|-------------|
| VPC | `learn-apigw-vpc` | 2 public subnets, 2 AZs, no NAT |
| ECS Cluster | `learn-apigw-cluster` | Fargate cluster |
| ECS Task Definition | — | 0.25 vCPU / 512 MB |
| ECS Service | `learn-apigw-user-service` | 1 Fargate task, public IP |
| ECR Repository | _(CDK-managed)_ | Docker image for user-service |
| NLB | `learn-apigw-user-nlb` | Internal, port 80 → ECS :8080 |
| VPC Link | `learn-apigw-user-link` | Bridges API GW to NLB |
| Security Group | `UserServiceSg` | Port 8080 open within VPC |
| Log Group | `/ecs/learn-apigw-user-service` | ECS container logs (7-day retention) |

---

## Build & Deploy

### Prerequisites

```bash
java --version      # OpenJDK 21
mvn --version       # Maven 3.6+
node --version      # Node 22 (via nvm)
cdk --version       # AWS CDK 2.x
docker --version    # Docker (required for ECS image build)
aws sts get-caller-identity   # AWS credentials configured
```

### Build All Modules

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Lambda JARs + user-service JAR (Docker picks up the user-service JAR)
mvn package -pl lambda-products,lambda-orders,user-service -DskipTests
```

### Deploy

```bash
source ~/.nvm/nvm.sh && nvm use 22
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

cd cdk

# First time only (one-time per account/region)
cdk bootstrap

# Deploy / update (CDK builds and pushes the Docker image automatically)
cdk deploy --require-approval never
```

### Destroy (stop all charges)

```bash
cd cdk && cdk destroy
```

---

## Cost

### Phase 1 + 2 only (Lambda + API Gateway)

| Service | Free Tier | Our Usage |
|---------|-----------|-----------|
| Lambda | 1M req/month + 400K GB-s (permanent) | Hundreds of test calls → $0 |
| API Gateway REST | 1M calls/month for 12 months | Hundreds of test calls → $0 |
| CloudWatch Logs | 5 GB ingestion/month | Minimal → $0 |
| S3 (CDK assets) | 5 GB | ~10 MB → $0 |

**Phase 1 + 2 total: $0/month**

### Phase 3 additions (ECS + NLB)

| Service | Free Tier | Estimated Cost |
|---------|-----------|---------------|
| ECS Fargate (0.25 vCPU, 0.5 GB) | Not included | ~$0.30/day while running |
| NLB | Not included | ~$0.19/day while running |
| ECR storage | 500 MB free | $0 (image is ~150 MB) |

**Phase 3 adds: ~$0.50/day (~$15/month if left running)**

> **Recommendation:** Run `cdk destroy` after each learning session. Re-deploy takes ~10 minutes and costs ~$0.05. Never leave it running overnight.

---

## Phases Roadmap

| Phase | Status | Topics |
|-------|--------|--------|
| **Phase 1** | ✅ Complete | Lambda proxy integration, REST API, stages, routes |
| **Phase 2** | ✅ Complete | API Keys, Usage Plans, 4-layer throttling, gateway responses, access logs |
| **Phase 3** | ✅ Complete | ECS Fargate, Docker, NLB, VPC Link, private HTTP integration |
| **Phase 4** | 🔜 Planned | ALB, ALB vs NLB comparison, HTTP API Gateway |
| **Phase 5** | 🔜 Planned | Lambda authorizers, Cognito, WAF, caching, custom domains |
