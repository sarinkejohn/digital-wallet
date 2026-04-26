#!/bin/sh
set -e

# Color codes
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  Digital Wallet System - Starting All-in-One  ${NC}"
echo -e "${GREEN}================================================${NC}"

# Wait function
wait_for() {
    host="$1"
    port="$2"
    service="$3"
    max=30
    i=0
    while ! nc -z "$host" "$port" 2>/dev/null; do
        i=$((i+1))
        if [ $i -ge $max ]; then
            echo -e "${RED}ERROR: $service not ready after $max attempts${NC}"
            exit 1
        fi
        echo -n "."
        sleep 2
    done
    echo ""
    echo -e "${GREEN}✅ $service is ready${NC}"
}

# Start PostgreSQL
echo -e "${YELLOW}Starting PostgreSQL...${NC}"
su - postgres -c "/usr/lib/postgresql/16/bin/pg_ctl -D $PGDATA -l /tmp/postgres.log start" || true
wait_for "localhost" "5432" "PostgreSQL"

# Initialize DB basic setup
su - postgres -c "psql -d $POSTGRES_DB -c 'CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";'" 2>/dev/null || true

# Start Redis
echo -e "${YELLOW}Starting Redis...${NC}"
redis-server --daemonize yes
wait_for "localhost" "6379" "Redis"

# Start Backend
echo -e "${YELLOW}Starting Backend Service on port 8080...${NC}"
JAVA_OPTS="-Xmx512m -Xms256m" java -jar /app/backend.jar &
BACKEND_PID=$!
wait_for "localhost" "8080" "Backend API"

# Start Gateway
echo -e "${YELLOW}Starting API Gateway on port 9090...${NC}"
JAVA_OPTS="-Xmx512m -Xms256m" java -jar /app/gateway.jar &
GATEWAY_PID=$!
wait_for "localhost" "9090" "API Gateway"

echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}  ✅ All services running!${NC}"
echo -e "${GREEN}================================================${NC}"
echo -e "  Backend:  http://localhost:8080"
echo -e "  Gateway:  http://localhost:9090"
echo -e "  Swagger:  http://localhost:9090/swagger-ui.html"
echo -e "  DB:       localhost:5432 (user:test, db:digital_wallet_db)"
echo -e "  Redis:    localhost:6379"
echo -e "${GREEN}================================================${NC}"
echo -e "${YELLOW}Press Ctrl+C to stop${NC}"

# Handle shutdown
trap 'echo ""; echo -e "${YELLOW}Shutting down...${NC}"; kill $BACKEND_PID $GATEWAY_PID 2>/dev/null; exit 0' INT TERM

# Monitor and restart if needed
while true; do
    if ! kill -0 $BACKEND_PID 2>/dev/null; then
        echo -e "${RED}Backend died, restarting...${NC}"
        java -jar /app/backend.jar & BACKEND_PID=$!
    fi
    if ! kill -0 $GATEWAY_PID 2>/dev/null; then
        echo -e "${RED}Gateway died, restarting...${NC}"
        java -jar /app/gateway.jar & GATEWAY_PID=$!
    fi
    sleep 5
done
