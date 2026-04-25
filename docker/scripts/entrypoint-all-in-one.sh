#!/bin/bash
set -e

echo "================================================"
echo "  Digital Wallet System - All-in-One Container"
echo "================================================"

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to wait for a service
wait_for() {
    local host="$1"
    local port="$2"
    local service="$3"
    local max_attempts=30
    local attempt=0

    log_info "Waiting for $service on $host:$port..."
    while ! nc -z "$host" "$port" 2>/dev/null; do
        attempt=$((attempt + 1))
        if [ $attempt -ge $max_attempts ]; then
            log_error "$service failed to start within timeout"
            exit 1
        fi
        echo -n "."
        sleep 2
    done
    echo ""
    log_info "✅ $service is ready!"
}

# Function to start PostgreSQL
start_postgres() {
    log_info "Starting PostgreSQL..."
    su - postgres -c "/usr/lib/postgresql/16/bin/pg_ctl -D $PGDATA -l /var/log/postgresql/postgresql.log start" || true

    # Wait for PostgreSQL to be ready
    wait_for "localhost" "5432" "PostgreSQL"

    # Initialize database schema if needed
    log_info "Initializing database schema..."
    su - postgres -c "psql -d $POSTGRES_DB -c 'CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";'" 2>/dev/null || true
}

# Function to start Redis
start_redis() {
    log_info "Starting Redis..."
    redis-server --daemonize yes
    wait_for "localhost" "6379" "Redis"
}

# Function to start Backend
start_backend() {
    log_info "Starting Digital Wallet Backend on port 8080..."
    java -jar /app/backend.jar &
    BACKEND_PID=$!

    # Wait for backend
    wait_for "localhost" "8080" "Backend API"
}

# Function to start Gateway
start_gateway() {
    log_info "Starting API Gateway on port 9090..."
    java -jar /app/gateway.jar &
    GATEWAY_PID=$!

    # Wait for gateway
    wait_for "localhost" "9090" "API Gateway"
}

# ===============================================
# Main
# ===============================================

log_info "Initializing services..."

# Start dependencies
start_postgres
start_redis

# Start application services
start_backend
start_gateway

echo ""
log_info "================================================"
log_info "  ✅ All services started successfully!"
log_info "================================================"
log_info "  Backend API:  http://localhost:8080"
log_info "  API Gateway:  http://localhost:9090"
log_info "  Swagger UI:   http://localhost:9090/swagger-ui.html"
log_info "  Database:     localhost:5432 (user: test, db: digital_wallet_db)"
log_info "  Redis:        localhost:6379"
log_info "================================================"
log_info "  Press Ctrl+C to stop all services"
log_info "================================================"

# Handle shutdown
trap 'echo ""; log_info "Shutting down..."; kill $BACKEND_PID $GATEWAY_PID 2>/dev/null; exit 0' SIGTERM SIGINT

# Monitor processes
while true; do
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        log_error "Backend process died! Restarting..."
        start_backend
    fi
    if ! kill -0 $GATEWAY_PID 2>/dev/null; then
        log_error "Gateway process died! Restarting..."
        start_gateway
    fi
    sleep 5
done &

wait $BACKEND_PID $GATEWAY_PID
