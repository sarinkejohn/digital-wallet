# Gateway Headers & Request Flow Documentation

## Overview
The API Gateway enforces strict header validation via JSON Schema before routing requests to the backend. All requests **must** include mandatory headers.

---

## Required Request Headers (All APIs)

### Mandatory for All Requests
| Header | Type | Required | Description | Example |
|--------|------|----------|-------------|---------|
| `Content-Type` | String | Yes | Must be `application/json` for POST/PUT | `application/json` |
| `Channel` | Enum | Yes | Request channel identifier | `WEBP`, `SWIFTY`, `BANKAPP`, `MOBILEMONEY`, `SAMONEY` |
| `AppId` | String | Yes | Application identifier (max 10 chars) | `WEBP`, `BANKAPP` |
| `AppVersion` | String | Yes | Semantic version pattern `X.Y.Z` | `1.0.0`, `2.1.3` |
| `RequestId` | String | Yes | Unique request ID (UUID recommended, max 36 chars) | `550e8400-e29b-41d4-a716-446655440000` |
| `Authorization` | String | Conditional | Bearer JWT token for protected endpoints | `Bearer <jwt_token>` |

**Conditional**: `Authorization` is required for all `/api/**` endpoints except `/api/auth/register` and `/api/auth/login`.

---

### Header Validation Logic
1. **Schema Validation** – All headers validated against JSON Schema (`gateway-headers-schema.json`)
2. **Channel Allow-list** – Channel must be in `app.channel.allowed-list` (default: `WEBP,SWIFTY,BANKAPP,MOBILEMONEY,SAMONEY`)
3. **RequestId Length** – Max 36 characters (configurable via `REQUEST_ID_MAX_LENGTH`)
4. **AppVersion Pattern** – Must match `^[vV]?[0-9]+\.[0-9]+\.[0-9]+.*$`
5. **Content-Type Enum** – Only `application/json` accepted

### Error Codes (Header Validation)
| Code | Condition | Message |
|------|-----------|---------|
| `ERR_1000001` | Missing/invalid mandatory header | "Missing mandatory parameters" |
| `ERR_1000002` | RequestId exceeds max length | "Invalid parameter length" |
| `ERR_1000003` | Channel not in allow-list | "Invalid channel" |

**Error Response Format** (HTTP 400):
```json
{
  "errorCode": "ERR_1000001",
  "errorMessage": "Missing mandatory parameters",
  "errorDescription": "One or few mandatory parameters are missing in the request",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "channel": "WEBP"
}
```

---

## Gateway-Modified Request Headers

The gateway **adds** these headers before forwarding to the backend:

| Header | Value | Description |
|--------|-------|-------------|
| `X-Channel-Tier` | `SILVER` \| `GOLD` \| `BRONZE` | Mapped from `Channel` header via `app.channel.tier-map` |
| `X-Gateway-Service` | `SchoolDirectoryGateway` | Identifies gateway service (added via `AddResponseHeader` filter) |

**Example**:
```http
GET /api/wallet/balance HTTP/1.1
Authorization: Bearer <token>
Content-Type: application/json
Channel: WEBP
AppId: WEBP
AppVersion: 1.0.0
RequestId: 550e8400-e29b-41d4-a716-446655440000
X-Channel-Tier: SILVER   ← added by gateway
```

---

## Gateway Response Headers

| Header | Value | Description |
|--------|-------|-------------|
| `X-Gateway-Service` | `SchoolDirectoryGateway` | Added to all responses |
| `X-RateLimit-Fallback` | `true` (when Redis down) | Indicates circuit breaker triggered, using Caffeine local counter fallback |

---

## Endpoint-Specific Header Requirements

### Public Endpoints (No Auth Required)
- `POST /api/auth/register`
- `POST /api/auth/login`

**Required headers**: `Content-Type`, `Channel`, `AppId`, `AppVersion`, `RequestId`

---

### Authenticated Endpoints (Require JWT)
All `/api/wallet/**` and `/api/admin/**` endpoints require `Authorization: Bearer <token>` in addition to the mandatory headers.

---

## Channel-to-Tier Mapping

Configured in `backend-gateway/src/main/resources/application.yaml`:

```yaml
app:
  channel:
    allowed-list: WEBP,SWIFTY,BANKAPP,MOBILEMONEY,SAMONEY
    tier-map:
      WEBP: SILVER
      SWIFTY: GOLD
      BANKAPP: GOLD
      MOBILEMONEY: BRONZE
      SAMONEY: BRONZE
```

**Rate Limit Tiers** (per second):

| Tier | Replenish Rate | Burst Capacity | Requests/Second |
|------|---------------|----------------|-----------------|
| BRONZE | 20 | 200 | 20 |
| SILVER | 100 | 1000 | 100 |
| GOLD | 400 | 5000 | 400 |

---

## Rate Limit Identity Resolution

The gateway's `clientKeyResolver` determines the rate-limit key (identity) in this priority order:

1. **JWT Authenticated** – Extracts `Authentication.getName()` (from JWT `sub` claim)
2. **AppId Header** – Uses `AppId` value if present
3. **Client IP** – Falls back to remote IP address
4. **Anonymous** – Final fallback to `"anonymous"`

The resolved identity is used as the Redis key: `rate_limit:{identity}`.

### Example Identity Keys in Redis
```
rate_limit:john_doe               # authenticated user
rate_limit:WEBP                   # unauthenticated AppId
rate_limit:192.168.1.100          # IP fallback
rate_limit:anonymous              # final fallback
```

---

## Complete cURL Template with All Required Headers

### Public Endpoint (Register)
```bash
curl -X POST http://localhost:9090/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Channel: WEBP" \
  -H "AppId: WEBP" \
  -H "AppVersion: 1.0.0" \
  -H "RequestId: $(uuidgen)" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123"
  }'
```

### Authenticated Endpoint (Get Balance)
```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -X GET http://localhost:9090/api/wallet/balance \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Channel: WEBP" \
  -H "AppId: WEBP" \
  -H "AppVersion: 1.0.0" \
  -H "RequestId: $(uuidgen)"
```

### Admin Endpoint (Direct Top-Up)
```bash
curl -X POST http://localhost:9090/api/admin/topup \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -H "Channel: BANKAPP" \
  -H "AppId: BANKAPP" \
  -H "AppVersion: 1.0.0" \
  -H "RequestId: $(uuidgen)" \
  -d '{
    "userId": 2,
    "amount": 150000.00,
    "reference": "ADMIN-001"
  }'
```

---

## Postman Collection Headers

In Postman, set these as **pre-request scripts** or **environment variables**:

```javascript
// Pre-request Script (auto-generates RequestId)
pm.environment.set("requestId", pm.variables.replaceIn("{{$guid}}"));
```

Then add headers to each request:
| Key | Value |
|-----|-------|
| Content-Type | `application/json` |
| Channel | `{{channel}}` (env var: `WEBP`) |
| AppId | `{{appId}}` (env var: `WEBP`) |
| AppVersion | `1.0.0` |
| RequestId | `{{requestId}}` |
| Authorization | `Bearer {{token}}` (for protected endpoints) |

---

## Header Flow Diagram

```
Client Request → Gateway Filter → Validation → Channel Tier Resolution → Backend

Headers In:  Content-Type, Channel, AppId, AppVersion, RequestId, [Authorization]
               ↓
     JsonSchemaValidationFilter
     - Validate schema
     - Check Channel allow-list
     - Check RequestId length
     - Resolve X-Channel-Tier
               ↓
Modified Request: + X-Channel-Tier header
               ↓
         Backend (:8080)
               ↓
Response Headers: X-Gateway-Service, [X-RateLimit-Fallback]
```

---

## Configuration Reference

### application.yaml (Gateway)
```yaml
app:
  rate-limit:
    package: SILVER  # default tier
    plans:
      BRONZE:
        replenishRate: 20
        burstCapacity: 200
      SILVER:
        replenishRate: 100
        burstCapacity: 1000
      GOLD:
        replenishRate: 400
        burstCapacity: 5000
  channel:
    allowed-list: WEBP,SWIFTY,BANKAPP,MOBILEMONEY,SAMONEY
    tier-map:
      WEBP: SILVER
      SWIFTY: GOLD
      BANKAPP: GOLD
      MOBILEMONEY: BRONZE
      SAMONEY: BRONZE

REQUEST_ID_MAX_LENGTH: 36
```

### JSON Schema File
`backend-gateway/src/main/resources/schema/gateway-headers-schema.json` – defines all header constraints.

---

## Troubleshooting

### Error: `ERR_1000001` – "Missing mandatory parameters"
**Cause**: Missing one of `Channel`, `AppId`, `AppVersion`, or `RequestId`.
**Fix**: Include all 5 mandatory headers + `Content-Type`.

### Error: `ERR_1000003` – "Invalid channel"
**Cause**: `Channel` value not in allow-list (e.g., `UNKNOWN`).
**Fix**: Use one of: `WEBP`, `SWIFTY`, `BANKAPP`, `MOBILEMONEY`, `SAMONEY`.

### Error: `ERR_1000002` – "RequestId exceeds maximum"
**Cause**: `RequestId` longer than configured max (default 36).
**Fix**: Use UUID or keep RequestId ≤ 36 characters.

### No `X-Channel-Tier` in request to backend
**Cause**: Channel not mapped in `app.channel.tier-map`.
**Fix**: Add channel→tier mapping in `application.yaml` or use a configured channel value.

### 401 Unauthorized
**Cause**: Missing or invalid `Authorization: Bearer <token>` on protected endpoint.
**Fix**: Obtain JWT via `/api/auth/login` or `/api/auth/register`, then include header.

---

## Summary Checklist

Before sending any request to `http://localhost:9090`:

- [ ] `Content-Type: application/json` (for POST/PUT)
- [ ] `Channel: <WEBP|SWIFTY|BANKAPP|MOBILEMONEY|SAMONEY>`
- [ ] `AppId: <string>` (≤10 chars)
- [ ] `AppVersion: <semver>` (e.g., `1.0.0`)
- [ ] `RequestId: <uuid>` (≤36 chars)
- [ ] `Authorization: Bearer <jwt>` (for protected endpoints only)
- [ ] For Postman: generate a fresh `RequestId` per request (use `{{$guid}}`)

---

## Quick Reference Card

```bash
# Generate headers
CHANNEL="WEBP"
APP_ID="WEBP"
APP_VERSION="1.0.0"
REQUEST_ID=$(uuidgen)

# Public endpoint
curl -X POST http://localhost:9090/api/auth/register \
  -H "Content-Type: application/json" \
  -H "Channel: $CHANNEL" \
  -H "AppId: $APP_ID" \
  -H "AppVersion: $APP_VERSION" \
  -H "RequestId: $REQUEST_ID" \
  -d '{"username":"user","password":"pass"}'

# Authenticated endpoint
curl -X GET http://localhost:9090/api/wallet/balance \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -H "Channel: $CHANNEL" \
  -H "AppId: $APP_ID" \
  -H "AppVersion: $APP_VERSION" \
  -H "RequestId: $(uuidgen)"
```
