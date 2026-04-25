# Unified Docker Deployment Guide

This guide covers deploying the entire Digital Wallet System using Docker containers.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Host / VM                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────┐    ┌──────────┐    ┌─────────────┐          │
│  │ Gateway │◄──►│ Backend  │◄──►│ PostgreSQL  │          │
│  │ :9090   │    │ :8080    │    │ :5432       │          │
│  └─────────┘    └──────────┘    └─────────────┘          │
│        │              │                   │               │
│  ┌─────────┐    ┌──────────┐    ┌─────────────┐          │
│  │  Redis  │◄──►│   Both  │    │   Jaeger    │          │
│  │ :6379   │    │Services │    │  (Tracing)  │          │
│  └─────────┘    └──────────┘    └─────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start (Recommended)

### Prerequisites
- Docker Engine 24.0+
- Docker Compose 2.0+ (or `docker compose` plugin)
- At least 4GB RAM, 10GB disk space

### One-Command Deployment

```bash
# Clone and navigate
cd /home/sarinke/API-SEC/BankingApp/dev

# Build and start all services
docker compose up -d

# Check status
docker compose ps

# View logs
docker compose logs -f

# Stop all services
docker compose down
```

### What Gets Deployed

| Service | Container Name | Port | Role |
|---------|---------------|------|------|
| **PostgreSQL** | `digital-wallet-db` | 5438 | Primary database |
| **Redis** | `digital-wallet-redis` | 6379 | Cache & rate limiting |
| **Backend API** | `digital-wallet-backend` | 8080 | Core business logic |
| **API Gateway** | `digital-wallet-gateway` | 9090 | Entry point, auth, rate limiting |
| **Jaeger** | `digital-wallet-jaeger` | 16686 | Distributed tracing UI |

## Manual Build & Deploy

### Build Individual Images

```bash
# Build backend service image
cd Digital-Wallet-Backend-Service
docker build -t digital-wallet-backend:latest \
  --build-arg SPRING_PROFILES_ACTIVE=dev \
  -f ../Dockerfile.backend .

# Build gateway image
cd ../backend-gateway
docker build -t digital-wallet-gateway:latest \
  --build-arg SPRING_PROFILES_ACTIVE=dev \
  -f ../Dockerfile.gateway .
```

### Run Individual Containers

```bash
# Start PostgreSQL
docker run -d \
  --name digital-wallet-db \
  -p 5438:5432 \
  -e POSTGRES_DB=digital_wallet_db \
  -e POSTGRES_USER=test \
  -e POSTGRES_PASSWORD=password \
  postgres:16-alpine

# Start Redis
docker run -d \
  --name digital-wallet-redis \
  -p 6379:6379 \
  redis:7-alpine

# Start Backend (depends on DB/Redis being ready)
docker run -d \
  --name digital-wallet-backend \
  -p 8080:8080 \
  --link digital-wallet-db:postgres \
  --link digital-wallet-redis:redis \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/digital_wallet_db \
  -e SPRING_DATA_REDIS_HOST=redis \
  digital-wallet-backend:latest

# Start Gateway
docker run -d \
  --name digital-wallet-gateway \
  -p 9090:9090 \
  --link digital-wallet-backend:backend \
  -e SPRING_CLOUD_GATEWAY_URI=http://backend:8080 \
  digital-wallet-gateway:latest
```

## Configuration

### Environment Variables

#### Backend Service (`Digital-Wallet-Backend-Service`)

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring profile |
| `DB_URL` | `jdbc:postgresql://localhost:5432/digital_wallet_db` | Database JDBC URL |
| `DB_USER` | `test` | Database username |
| `DB_PASSWORD` | `password` | Database password |
| `JWT_SECRET` | *(see code)* | JWT signing secret (CHANGE IN PROD) |
| `JWT_EXPIRATION` | `86400000` | Token expiry in ms (24h) |
| `WALLET_TRANSFER_MIN_AMOUNT` | `1.0` | Minimum transfer amount |
| `WALLET_TRANSFER_MAX_AMOUNT` | `100000.0` | Maximum transfer amount |
| `WALLET_TOPUP_MAX_AMOUNT` | `1000000.0` | Maximum top-up amount |
| `AUTH_MAX_LOGIN_ATTEMPTS` | `5` | Failed login attempts before lockout |
| `AUTH_LOCKOUT_DURATION` | `5` | Lockout duration in minutes |

#### API Gateway (`backend-gateway`)

| Variable | Default | Description |
|----------|---------|-------------|
| `ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | CORS allowed origins |
| `CHANNEL_ARRAY_LIST` | `WEBP,SWIFTY,BANKAPP,MOBILEMONEY,SAMONEY` | Valid channels |
| `CHANNEL_TIER_MAP_*` | See below | Rate limit tier per channel |
| `APP_RATE_LIMIT_PACKAGE` | `SILVER` | Default rate limit tier |

**Rate Limit Tiers** (configurable via `application.yaml`):
- **BRONZE**: 20 req/s, burst 200
- **SILVER**: 100 req/s, burst 1000 (default)
- **GOLD**: 400 req/s, burst 5000

### Custom `.env` File

Create a `.env` file in the project root:

```bash
# JWT Configuration (CHANGE THIS FOR PRODUCTION!)
JWT_SECRET="your-super-secret-256-bit-key-here"

# Database
DB_USER=wallet_user
DB_PASSWORD=SecurePass123!

# Rate Limits
APP_RATE_LIMIT_PACKAGE=GOLD
CHANNEL_TIER_MAP_WEBP=GOLD

# Security
AUTH_MAX_LOGIN_ATTEMPTS=3
AUTH_LOCKOUT_DURATION=10
```

Then reference it in `docker-compose.yml`:
```yaml
environment:
  JWT_SECRET: ${JWT_SECRET}
```

## Testing the Deployment

### 1. Health Checks

```bash
# Backend health
curl http://localhost:8080/actuator/health

# Gateway health
curl http://localhost:9090/actuator/health

# Database connection
docker exec digital-wallet-db pg_isready -U test
```

### 2. Register a User

```bash
curl -X POST 'http://localhost:9090/api/auth/register' \
  -H 'Content-Type: application/json' \
  -H 'Channel: WEBP' \
  -H 'AppId: WEBP' \
  -H 'AppVersion: 1.0.0' \
  -H 'RequestId: test-123' \
  -d '{"username": "testuser", "password": "SecurePass123!"}'
```

### 3. Get Wallet Balance

```bash
# Extract token from registration response
TOKEN="<your-jwt-token>"

curl -X GET 'http://localhost:9090/api/wallet/balance' \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Channel: WEBP' \
  -H 'AppId: WEBP' \
  -H 'AppVersion: 1.0.0' \
  -H 'RequestId: test-456'
```

### 4. Access Swagger UI

Open in browser: http://localhost:9090/swagger-ui.html

## Troubleshooting

### Services Won't Start

```bash
# Check logs
docker compose logs backend
docker compose logs gateway
docker compose logs postgres

# Check container status
docker compose ps

# Restart specific service
docker compose restart backend
```

### Database Connection Errors

```bash
# Wait for PostgreSQL to be ready
docker exec digital-wallet-db pg_isready -U test

# View PostgreSQL logs
docker logs digital-wallet-db

# Reset database (WARNING: deletes all data)
docker compose down -v
docker compose up -d
```

### Port Already in Use

```bash
# Check what's using port 8080/9090
sudo lsof -i :8080
sudo lsof -i :9090

# Kill process or change ports in docker-compose.yml
```

### Gateway Returns 400 Bad Request

Common causes:
1. Missing required headers (Channel, AppId, AppVersion, RequestId)
2. Invalid channel (must be in allowed-list)
3. Mismatched channel in JWT token

### "Mapper Bean Not Found" Error

This indicates the JAR wasn't built with MapStruct processor. Rebuild:

```bash
cd Digital-Wallet-Backend-Service
mvn clean package -DskipTests
cd ..
docker compose build backend
```

## Production Deployment

### 1. Use External Databases

Update `docker-compose.yml` to use managed databases:

```yaml
backend:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://your-rds.amazonaws.com:5432/digital_wallet_db
    SPRING_DATA_REDIS_HOST: your-elasticache-endpoint
```

### 2. Set Strong JWT Secret

```bash
# Generate 256-bit secret
openssl rand -base64 32

# Use in production
JWT_SECRET="your-generated-secret-here"
```

### 3. Enable HTTPS

Add SSL/TLS termination at gateway level or use a reverse proxy (nginx, Traefik).

### 4. Configure Logging

```yaml
backend:
  environment:
    LOGGING_LEVEL_ROOT: INFO
    LOGGING_LEVEL_COM_SARINKEJOHN: DEBUG
```

### 5. Resource Limits

```yaml
backend:
  deploy:
    resources:
      limits:
        memory: 2G
        cpus: '1.0'
```

## Monitoring & Observability

### Access Jaeger UI

Open: http://localhost:16686

Search for traces by:
- Service name: `digital-wallet-backend` or `digital-wallet-gateway`
- Operation: `/api/auth/register`, `/api/wallet/balance`, etc.
- Tags: `http.status_code`, `http.method`

### View Metrics

```bash
# Prometheus metrics
curl http://localhost:9090/actuator/prometheus

# Backend metrics
curl http://localhost:8080/actuator/prometheus
```

### Application Logs

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend

# Last 100 lines
docker compose logs --tail=100 backend
```

## Updating the Application

### Rolling Update (Zero Downtime)

```bash
# Rebuild images
docker compose build

# Rolling restart
docker compose up -d --no-deps --build backend
docker compose up -d --no-deps --build gateway
```

### Database Migrations

If using Flyway/Liquibase (not currently configured):

```bash
# Run migrations manually
docker exec digital-wallet-backend \
  java -jar /app/backend.jar \
  --spring.profiles.active=migrate
```

## Cleanup

### Stop All Services

```bash
# Stop but keep data
docker compose stop

# Stop and remove containers (keeps volumes)
docker compose down

# Stop and remove everything including volumes (WARNING: data loss!)
docker compose down -v
```

### Remove Images

```bash
docker rmi digital-wallet-backend:latest
docker rmi digital-wallet-gateway:latest
```

## Support

- Backend Issues: Check `Digital-Wallet-Backend-Service/README.md`
- Gateway Issues: Check `backend-gateway/README.md`
- Architecture: See `IMPLEMENTATION_REFERENCE.md`

---

**Last Updated**: 2026-04-25
**Version**: 1.0.0
