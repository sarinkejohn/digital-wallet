# =========================
# Build stage is done outside Docker (recommended)
# =========================

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built JAR (from target folder)
COPY target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Environment variables with defaults
ENV JWT_SECRET="mySecretKeyForDigitalWalletServiceThat'sLongEnoughForHS256Algorithm" \
    BCRYPT_ROUNDS=10 \
    AUTH_MAX_LOGIN_ATTEMPTS=5 \
    AUTH_LOCKOUT_DURATION=5 \
    SPRING_PROFILES_ACTIVE=dev

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]