#!/bin/bash
set -e

echo "================================================"
echo "  Digital Wallet System - Starting..."
echo "================================================"

# Function to wait for a service to be ready
wait_for_service() {
    local host="$1"
    local port="$2"
    local service="$3"
    local max_attempts=30
    local attempt=0

    echo "Waiting for $service on $host:$port..."
    while ! nc -z "$host" "$port" 2>/dev/null; do
        attempt=$((attempt + 1))
        if [ $attempt -ge $max_attempts ]; then
            echo "ERROR: $service failed to start within timeout"
            exit 1
        fi
        echo "  Attempt $attempt/$max_attempts..."
        sleep 2
    done
    echo "✅ $service is ready!"
}

# Check if we should start embedded databases or connect to external
START_DB="${START_DB:-true}"
START_REDIS="${START_REDIS:-true}"

if [ "$START_DB" = "true" ]; then
    echo "Starting embedded PostgreSQL..."
    # Use external Docker Compose for full stack (recommended)
    echo "Note: For production, use docker-compose.yml with external DB/Redis"
    echo "Starting with external database connections..."
fi

# Wait for external services if configured
if [ "$START_DB" = "false" ]; then
    wait_for_service "${DB_HOST:-localhost}" "${DB_PORT:-5432}" "PostgreSQL"
fi

if [ "$START_REDIS" = "false" ]; then
    wait_for_service "${SPRING_REDIS_HOST:-redis}" "${SPRING_REDIS_PORT:-6379}" "Redis"
fi

# Start Backend Service in background
echo "Starting Digital Wallet Backend Service on port 8080..."
java -jar /app/backend.jar &
BACKEND_PID=$!

# Wait for backend to be ready
sleep 5

# Start Gateway in background
echo "Starting API Gateway on port 9090..."
java -jar /app/gateway.jar &
GATEWAY_PID=$!

# Wait for gateway to be ready
sleep 5

echo "================================================"
echo "  ✅ All services started successfully!"
echo "================================================"
echo "  Backend API:  http://localhost:8080"
echo "  API Gateway:  http://localhost:9090"
echo "  Swagger UI:   http://localhost:9090/swagger-ui.html"
echo "================================================"
echo "  Press Ctrl+C to stop all services"
echo "================================================"

# Handle shutdown gracefully
trap 'echo "Shutting down..."; kill $BACKEND_PID $GATEWAY_PID; exit 0' SIGTERM SIGINT

# Wait for both processes
wait $BACKEND_PID $GATEWAY_PID
