# Digital Wallet Backend Service

A Spring Boot REST API for a digital wallet system supporting user balances and fund transfers.

## Features

- User registration and authentication (JWT)
- Wallet management (view balance)
- Fund transfers between users
- Admin top-ups (add funds to user wallets)
- Top-up request workflow
- Transfer limits (min: 1.0, max: 100,000 TZS)
- Top-up limits (max: 1,000,000 TZS)
- Idempotency support via reference
- OpenAPI/Swagger documentation

## Tech Stack

- Java 25 + Spring Boot 4.0.5
- PostgreSQL
- Spring Security + JWT
- Spring Data JPA
- Lombok
- OpenAPI 3.1 (SpringDoc 3.x)

## Prerequisites

- **JDK 25**: The project requires Java 25. Ensure `JAVA_HOME` points to a JDK 25 installation.
- **Environment Variables**: A `.env` file is required in the root directory. See `.env.example` or the Configuration section for details.

## Quick Start

### Using Docker

```bash
docker-compose up --build
```

### Local Development

1. **Environment Setup**: Ensure you have a `.env` file in the project root.
2. **Start Database**:
   ```bash
   docker run -d -p 5438:5432 -e POSTGRES_DB=digital_wallet_db -e POSTGRES_USER=test -e POSTGRES_PASSWORD=password postgres:16-alpine
   ```
3. **Run Application**:
   Ensure you are using JDK 25:
   ```bash
   # Check java version
   java -version 
   
   # Run
   ./mvnw spring-boot:run
   ```

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login

### User Wallet (Requires JWT)
- `GET /api/wallet/balance` - Get wallet balance
- `POST /api/wallet/transfer` - Transfer funds
- `POST /api/wallet/topup/request` - Request top-up
- `GET /api/wallet/transactions` - Get transaction history

### Admin (Requires ADMIN role)
- `POST /api/admin/topup` - Top-up user wallet
- `GET /api/admin/topup/requests` - Get pending requests
- `POST /api/admin/topup/requests/{id}/approve` - Approve request
- `POST /api/admin/topup/requests/{id}/reject` - Reject request
- `GET /api/admin/wallet/{userId}` - Get user wallet

## API Documentation

Access Swagger UI at: http://localhost:8080/swagger-ui.html

## Security

- JWT-based authentication
- Role-based authorization (ADMIN, USER)
- Transfer amount validation
- Input validation with safe error messages

## Configuration

Key properties in `application-dev.properties`:
```properties
wallet.transfer.min-amount=1.0
wallet.transfer.max-amount=100000.0
wallet.topup.max-amount=1000000.0
jwt.secret=your-secret-key
jwt.expiration=86400000
```

## Testing

```bash
./mvnw test
```

## Architecture

- **Controller** - REST endpoints
- **Service** - Business logic
- **Repository** - Data access
- **Entity** - Database models
- **DTO** - Request/Response objects
- **Security** - JWT handling
- **Exception** - Custom exceptions