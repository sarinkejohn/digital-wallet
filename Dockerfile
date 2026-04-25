# ===============================================
# Multi-Stage Build: Digital Wallet System
# Packages Backend Service + Gateway + DB + Redis
# ===============================================

# Stage 1: Build Backend Service
FROM maven:4.0.1-eclipse-temurin-25 AS backend-builder
WORKDIR /build/backend
COPY Digital-Wallet-Backend-Service/pom.xml .
COPY Digital-Wallet-Backend-Service/src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Stage 2: Build Gateway
FROM maven:4.0.1-eclipse-temurin-25 AS gateway-builder
WORKDIR /build/gateway
COPY backend-gateway/pom.xml .
COPY backend-gateway/src ./src
RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Stage 3: Runtime Image with PostgreSQL + Redis + Both Services
FROM eclipse-temurin:25-jre-alpine

# Install dumbbell for process management
RUN apk add --no-cache dumb-init

WORKDIR /app

# Copy backend JAR
COPY --from=backend-builder /build/backend/target/*.jar /app/backend.jar

# Copy gateway JAR
COPY --from=gateway-builder /build/gateway/target/*.jar /app/gateway.jar

# Copy startup script
COPY docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

# Expose ports
# 8080 - Backend Service
# 9090 - Gateway
# 5432 - PostgreSQL (internal)
# 6379 - Redis (internal)
EXPOSE 8080 9090

# Environment variables with defaults
ENV \
    # Database
    POSTGRES_DB=digital_wallet_db \
    POSTGRES_USER=test \
    POSTGRES_PASSWORD=password \
    DB_URL=jdbc:postgresql://localhost:5432/digital_wallet_db \
    DB_USER=test \
    DB_PASSWORD=password \
    \
    # JWT
    JWT_SECRET="mySecretKeyForDigitalWalletServiceThat'sLongEnoughForHS256Algorithm" \
    JWT_EXPIRATION=86400000 \
    \
    # Business Rules
    WALLET_TRANSFER_MIN_AMOUNT=1.0 \
    WALLET_TRANSFER_MAX_AMOUNT=100000.0 \
    WALLET_TOPUP_MAX_AMOUNT=1000000.0 \
    \
    # Auth Security
    AUTH_MAX_LOGIN_ATTEMPTS=5 \
    AUTH_LOCKOUT_DURATION=5 \
    BCRYPT_ROUNDS=10 \
    \
    # App Config
    SPRING_PROFILES_ACTIVE=dev \
    CHANNEL_ARRAY_LIST="WEBP,SWIFTY,BANKAPP,MOBILEMONEY,SAMONEY"

ENTRYPOINT ["/usr/bin/dumb-init", "--"]
CMD ["docker-entrypoint.sh"]
