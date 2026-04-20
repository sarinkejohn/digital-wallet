# API Gateway - Tiered Rate Limiting & Security

This service acts as the central API Gateway built upon **Spring Cloud Gateway**. It robustly handles identity resolution, rate limiting, circuit breaking, and resilience for all upstream services using Redis and OAuth2 constructs.

## Features

- **Tiered Dynamic Rate Limiting**: Built on top of Spring Data Redis utilizing Lua scripts, enforcing limits per-client based on packages (`BRONZE`, `SILVER`, `GOLD`).
- **OAuth2 Identity Resolution**: Natively extracts client IDs (`sub` claims) from JWT tokens acting securely via `spring-boot-starter-oauth2-resource-server`.
- **In-Memory Caching (Caffeine)**: Limits the overhead of Redis by caching client tier mappings locally for up to 60 seconds manually.
- **Fail-Open Local Resilience**: Wrapped by **Resilience4j Circuit Breakers**, any Redis outages cascade gracefully into a local `Caffeine` counter fallback restricting usage to *5 global requests per minute* iteratively.
- **Observability**: Directly instrumented with Micrometer, exposing rich `.prometheus` metric data continuously without spamming log layers.

---

## Configuration

The default `.env` layout controls the environment constraints:
```env
# Gateway Port
SERVER_PORT=9090

# The default Rate Limiting Package Fallback
APP_RATE_LIMIT_PACKAGE=SILVER

# Redis Details
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# The JWT Secret Signature validating Authorization requests
JWT_SECRET=Z1hyWHRoZVRyeVNlY3JldEtleUZvckpXVFRva2VuR2VuZXJhdGlvbnZhbGlkYXRpb24xMjM=
```

### Rate Limiting Tiers
Constraints are injected dynamically through `application.yaml`:
- **BRONZE**: 1 request per second, 1 burst capability.
- **SILVER**: 10 requests per second, 20 burst capability.
- **GOLD**: 50 requests per second, 100 burst capability.

By default, an unknown user consumes the metric set by `APP_RATE_LIMIT_PACKAGE`. However, overrides are fully supported by inserting custom flags natively onto Redis: `SET rate_limit_tier:{clientId} GOLD`.

---

## Running the Application

### 1. Boot up Redis natively
Use Docker Compose to deploy the Redis instance gracefully.
```bash
docker-compose up -d school-directory-gateway-redis
```

### 2. Build and Execute the Gateway
Ensure you have `Java 17` compiled into your environment variables.
```bash
./mvnw clean package -DskipTests
java -jar target/gateway-demo-0.0.1-SNAPSHOT.jar
```
Or simply use:
```bash
./mvnw spring-boot:run
```

---

## Testing API Integrations

### Hitting the Gateway
By default, the gateway binds to `http://localhost:8080` and redirects `/api/**` traffic properly to backend hosts connected on port `8081`. 

### Identity Usage
The `TieredDynamicRateLimiter` isolates the user identifier based on 3 priorities:
1. **OAuth2 Bearer Token**: Using `Authorization: Bearer <token>`. The system automatically decrypts the HMAC SHA-256 signature from `JWT_SECRET` natively reading out the `.sub` identity scope.
2. **Custom Header**: Supplying an `AppId: <custom-id>` Header in regular endpoints.
3. **Guest IP**: Routing defaults to utilizing the IP Request structure if no identity strings exist natively.

#### Standard Test Hit
```bash
curl -i -H "AppId: test-user" http://localhost:9090/api/schools/list
```

### Simulating Limits (HTTP 429)
In order to test the Tier Limits:
1. Supply `APP_RATE_LIMIT_PACKAGE=BRONZE` inside your `.env` securely.
2. Rapidly hit the `/api/` endpoint using `curl` continuously multiple times within the identical second span.
3. You will natively receive `HTTP/1.1 429 Too Many Requests` denoting the exhaustion of the Token Bucket alongside your payload dropping.

### Simulating Redis Disaster Resilience (Local Circuit Breaking)
To test the `Fail Open` metrics caching architecture:
1. Continuously curl the endpoints successfully proving routing functions securely.
2. Stop the overarching Redis container: `docker stop school-directory-gateway-redis`.
3. Hit the Endpoint again! It natively wraps gracefully onto the `CacheService Local Counter` logic! 
4. Attempt making `6` requests straight into the application bounds under a minute, which safely kicks an immediate drop `429 Too Many Requests` verifying abuse vectors have been completely nullified.

---

## Metrics Observability

The `TieredDynamicRateLimiter` internally populates native Counter properties natively tracking block/allow metrics via Spring Boot Actuators directly.

Access the Prometheus structured readout metrics globally at:
`GET http://localhost:9090/actuator/prometheus`

Look for the distinct counters natively attached inside the output structure:
```
rate_limit_allowed_total_total
rate_limit_blocked_total_total
```
