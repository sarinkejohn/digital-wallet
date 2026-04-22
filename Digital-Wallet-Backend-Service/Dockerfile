# =========================
# Build stage is done outside Docker (recommended)
# =========================

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy the built JAR (from target folder)
COPY target/*.jar app.jar

# Expose Spring Boot port
EXPOSE 8080

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]