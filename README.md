# Eagle Bank API

A RESTful banking API built with Spring Boot 3.5.6 and Java 21, implementing user management, bank accounts, and
transaction processing with JWT authentication.

## Table of Contents

- [Requirements Implemented](#requirements-implemented)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Authentication](#authentication)
- [Testing](#testing)
- [Architecture](#architecture)
- [Project Structure](#project-structure)

## Requirements Implemented

All required functionality has been implemented:

### Security & Authorization

- ✅ User login with JWT token generation (POST `/v1/auth/login`)
- ✅ JWT-based authentication
- ✅ All endpoints (except registration) require authentication
- ✅ Users can only access their own resources
- ✅ Users authenticate using email and password

### User Management

- ✅ Create user (POST `/v1/users`)
- ✅ Fetch user details (GET `/v1/users/{userId}`)
- ✅ Update user details (PATCH `/v1/users/{userId}`)
- ✅ Delete user (DELETE `/v1/users/{userId}`)

### Bank Account Management

- ✅ Create bank account (POST `/v1/accounts`)
- ✅ List user's accounts (GET `/v1/accounts`)
- ✅ Fetch account details (GET `/v1/accounts/{accountNumber}`)
- ✅ Update account name (PATCH `/v1/accounts/{accountNumber}`)
- ✅ Delete account (DELETE `/v1/accounts/{accountNumber}`)

### Transaction Processing

- ✅ Create transaction (deposit/withdrawal) (POST `/v1/accounts/{accountNumber}/transactions`)
- ✅ List account transactions (GET `/v1/accounts/{accountNumber}/transactions`)
- ✅ Fetch transaction details (GET `/v1/accounts/{accountNumber}/transactions/{transactionId}`)

## Quick Start

### Prerequisites

- Java 21
- Docker
- Gradle (wrapper included)

### Running the Application

```bash
./gradlew bootRun
```

The API will be available at `http://localhost:8080`

### First API Call

Register a user:

```bash
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "phoneNumber": "+447700900123",
    "address": {
      "line1": "123 Main St",
      "town": "London",
      "county": "Greater London",
      "postcode": "SW1A 1AA"
    }
  }'
```

### End-to-end test in Postman

An E2E test/script is available. See [postman/README.md](postman/README.md).

## API Documentation

### OpenAPI/Swagger UI

http://localhost:8080/swagger-ui/index.html

### OpenAPI Specification

The complete API contract is defined in [openapi.yaml](src/main/resources/static/openapi.yaml).

This can be imported into Postman or any OpenAPI-compatible tool.

## Authentication

### How It Works

The API uses **JWT (JSON Web Token)** authentication:

1. **Register** a user (POST `/v1/users`) - No authentication required
2. **Login** (POST `/v1/auth/login`) to receive a JWT token
3. **Include the token** in subsequent requests via the `Authorization` header:
   ```
   Authorization: Bearer {your-jwt-token}
   ```

### Authentication Flow Example

```bash
# 1. Register a user
curl -X POST http://localhost:8080/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","email":"john@example.com","password":"Pass123!","phoneNumber":"+447700900123","address":{"line1":"123 Main St","town":"London","county":"London","postcode":"SW1A 1AA"}}'

# 2. Login to get JWT token
curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"john@example.com","password":"Pass123!"}'

# Response: {"token":"eyJhbGc...", "userId":"usr-abc123"}

# 3. Use token in subsequent requests
curl -X GET http://localhost:8080/v1/accounts \
  -H "Authorization: Bearer eyJhbGc..."
```

### Security Features

- **Password Salting/Hashing:** All passwords are hashed using BCrypt
- **Token Expiration:** JWT tokens expire after 1 hour
- **Resource Ownership:** Users can only access their own accounts and transactions
- **Authorization Checks:** All endpoints verify user ownership before returning data

## Testing

### Running Tests

```bash
# Run all tests
./gradlew test
```

### Test Strategy

The project uses **integration tests** with Testcontainers:

- Tests use real PostgreSQL database (via Testcontainers)
- Full request-response cycle testing
- Database state verification
- Tests are located in `src/test/java/com/barclays/eagle_bank_api/integration/`

### Test Coverage

All major functionality is covered:

- User registration, login, CRUD operations
- Account creation, listing, updates, deletion
- Transaction management (deposits/withdrawals)
- Authentication and authorization
- Business rules (insufficient funds, account deletion restrictions etc)
- Error handling and validation

## Architecture

### Design Approach

**API-First Development:**

- OpenAPI specification defines the API contract (`src/main/resources/static/openapi.yaml`)
- Code generation creates API interfaces and DTOs from the spec
- Controllers implement the generated interfaces
- Ensures implementation matches specification

**Layered Architecture:**

```
Controller Layer  → Handles HTTP requests/responses
    ↓
Service Layer     → Business logic and validation
    ↓
Repository Layer  → Data access (Spring Data JPA)
    ↓
Database          → PostgreSQL
```

### Technology Stack

- **Framework:** Spring Boot 3.5.6
- **Language:** Java 21
- **Database:** PostgreSQL
- **ORM:** Spring Data JPA / Hibernate
- **Security:** Spring Security + JWT
- **Build Tool:** Gradle (Kotlin DSL)
- **Testing:** JUnit 5, Testcontainers, Spring Boot Test
- **API Spec:** OpenAPI 3.1

### Key Design Decisions

**Why API-First?**

- Contract provided in tech task becomes source of truth
- Type-safe implementation
- Frontend/backend can work from same contract
- Validation defined once in OpenAPI spec

**Why Integration Tests?**

- Test real behavior, not mocks
- Catch integration issues early
- Database constraints validated
- With more time would implement unit tests for more coverage on core logic

**Why Postgres?**

- Robust, production-ready relational database
- Familiarity and widespread use
- Strong support in Spring Data JPA
- Easy to run locally with Docker
- Supports complex queries and transactions needed for banking app
- Scales well for future growth

**Why Spring?**

- Mature, widely-used Java framework
- Excellent support for REST APIs, security, data access
- Large ecosystem and community
- Rapid development with Spring Boot conventions

## Project Structure

```
src/main/java/com/barclays/eagle_bank_api/
├── api/              # Generated API interfaces (from OpenAPI)
├── model/            # Generated DTOs (from OpenAPI)
├── controller/       # REST controllers (implement generated APIs)
├── service/          # Business logic
├── repository/       # Data access (Spring Data JPA)
├── entity/           # JPA entities
├── domain/           # Value objects and domain types
├── security/         # JWT authentication & authorization
├── exception/        # Custom exceptions and error handling
└── config/           # Spring configuration

src/main/resources/
├── static/
│   └── openapi.yaml  # API specification (source of truth)
└── application.properties

src/test/java/com/barclays/eagle_bank_api/
└── integration/      # Integration tests

postman/
├── eagle-bank-e2e.postman_collection.json  # E2E test collection
└── README.md         # Postman usage instructions
```

## CI/CD Pipeline

The project includes a GitHub Actions workflow ([.github/workflows/ci-cd.yml](.github/workflows/ci-cd.yml)) that runs on
every push to main.

### Pipeline Stages

**1a. Build and Test**

- Builds the application with Gradle
- Runs all integration tests using Testcontainers

**1b. Code Quality**

- Checks code formatting using Spotless

**2. Deploy (Simulated)**

- Requires build-and-test and code-quality to pass
- Builds production JAR artifact
- Simulates deployment process (would deploy to actual environment in production)

### Running Locally

```bash
# Run the same checks that CI runs
./gradlew build test spotlessCheck

# Fix formatting issues
./gradlew spotlessApply
```

All checks must pass before code can be merged to main.

### Pre-Commit Hooks

The project includes a Git pre-commit hook (`.git/hooks/pre-commit`) that automatically runs before each commit:

**Checks performed:**

1. **Code Formatting** - Runs `spotlessCheck` to ensure code style is consistent
2. **Build & Tests** - Runs `./gradlew check` which includes compilation and all tests

**What happens:**

- If any check fails, the commit is blocked
- You'll see clear error messages indicating what needs to be fixed
- Run `./gradlew spotlessApply` to automatically fix formatting issues

**Installing the hook:**
The hook is already in `.git/hooks/pre-commit` and will run automatically. If you cloned the repo and it's not working,
ensure it's executable:

```bash
chmod +x .git/hooks/pre-commit
```

This ensures code quality before changes are committed and reduces CI failures.

## Configuration

Key configuration in `src/main/resources/application.properties`:

- **Database:** Auto-configured via Docker Compose
- **JWT Secret:** Configured for development (change for production)
- **JWT Expiration:** 1 hour
- **Schema Management:** Hibernate `create-drop` (recreates schema on startup)

## Additional Notes

### Database Indexes

The application includes indexes on:

- `accounts.user_id` - for listing user's accounts
- `transactions.account_number` - for listing account transactions

These are automatically created by Hibernate from JPA entity annotations.

### ID Generation

Custom ID formats are implemented:

- User IDs: `usr-{uuid}` (e.g., `usr-a1b2c3d4e5f6`)
- Transaction IDs: `tan-{uuid}` (e.g., `tan-x1y2z3a4b5c6`)
- Account numbers: `01XXXXXX` (e.g., `01234567`)

---