# API Gateway - Security & Rate Limiting

This service acts as the central API Gateway built upon **Spring Cloud Gateway**. It provides request validation, JWT authentication, channel binding enforcement, and tiered rate limiting.

## Features

### 1. Request Header Validation
Validates mandatory headers on every request:
- `Content-Type`: Required for POST/PUT
- `Channel`: Required (WEBP, SWIFTY, BANKAPP, MOBILEMONEY, SAMONEY)
- `AppId`: Required
- `AppVersion`: Required
- `RequestId`: Required (UUID)

Returns `400 Bad Request` if validation fails.

### 2. Channel Binding Security
Ensures JWT tokens are only usable within their issued channel:
- Token must include `channel` claim
- Request `Channel` header must match token's channel
- Returns `403 Forbidden` on mismatch

This prevents token theft across channels.

### 3. JWT Authentication
- Validates Bearer tokens using HMAC SHA-256
- Extracts claims: `sub`, `role`, `userId`, `channel`
- Passes to backend via headers

### 4. Tiered Rate Limiting
Per-channel tiers with configurable limits:
- **BRONZE** (MOBILEMONEY, SAMONEY): 20 req/s, 200 burst
- **SILVER** (WEBP): 100 req/s, 1000 burst
- **GOLD** (SWIFTY, BANKAPP): 400 req/s, 5000 burst

---

## Architecture

```
Client Request
     ↓
┌─────────────────────────────────┐
│  Gateway (Port 9090)              │
├─────────────────────────────────┤
│  1. JsonSchemaValidationFilter   │ ← Validate headers
│  2. ChannelValidationFilter    │ ← Channel binding
│  3. RequestRateLimiter       │ ← Rate limiting
│  4. Route                   │ ← Proxy to backend
└─────────────────────────────────┘
     ↓
Backend (Port 8080)
```

---

## Headers Required

| Header | Required | Values |
|--------|----------|--------|
| `Content-Type` | POST/PUT | `application/json` |
| `Channel` | Always | `WEBP`, `SWIFTY`, `BANKAPP`, `MOBILEMONEY`, `SAMONEY` |
| `AppId` | Always | Any string |
| `AppVersion` | Always | Semver (e.g., `1.0.0`) |
| `RequestId` | Always | UUID |
| `Authorization` | Protected | `Bearer <jwt>` |

---

## Running

### Docker Compose
```bash
docker compose up --build
```

### Manual
```bash
./mvnw clean package -DskipTests
java -jar target/gateway-demo-0.0.1-SNAPSHOT.jar
```

---

## Environment Variables

```env
# Gateway
SERVER_PORT=9090

# Redis (for rate limiting)
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# JWT Secret (must match backend)
JWT_SECRET=mySecretKeyForDigitalWalletServiceThat'sLongEnoughForHS256Algorithm

# Rate Limiting
APP_RATE_LIMIT_PACKAGE=SILVER
```

---

## Response Codes

| Code | Meaning |
|------|---------|
| 400 | Missing/invalid headers |
| 401 | Missing/invalid JWT |
| 403 | Channel mismatch |
| 404 | Not found |
| 423 | Rate limited (Redis locked) |
| 429 | Rate limit exceeded |
| 500 | Backend error |

---

## Metrics

Access Prometheus metrics:
```bash
curl http://localhost:9090/actuator/prometheus
```

Key metrics:
- `rate_limit_allowed_total` - Requests allowed
- `rate_limit_blocked_total` - Requests blocked

---

## Reusable Components

| Component | File | Reusable? |
|-----------|------|-----------|
| `JsonSchemaValidationFilter` | `filter/JsonSchemaValidationFilter.java` | ✅ Yes |
| `ChannelValidationFilter` | `filter/ChannelValidationFilter.java` | ✅ Yes |
| `TieredDynamicRateLimiter` | `config/TieredDynamicRateLimiter.java` | ✅ Yes |
| `ChannelProperties` | `config/ChannelProperties.java` | ✅ Yes |

To reuse in another project:
1. Add gateway dependency
2. Configure routes in `application.yaml`
3. Add filters to route

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: my-api
          uri: http://backend:8080
          predicates:
            - Path=/api/**
          filters:
            - JsonSchemaValidationFilter
            - ChannelValidationFilter
```