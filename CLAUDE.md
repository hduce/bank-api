# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Eagle Bank API is a Spring Boot 3.5.6 REST API for banking operations built with Java 21. The API provides user management, account operations, and transaction processing with JWT authentication. The implementation must conform to the OpenAPI specification defined in `openapi.yaml`.

## Development Commands

### Code Generation

The project uses OpenAPI Generator to generate API interfaces and DTOs from `openapi.yaml`:

```bash
# Generate API interfaces and model classes from OpenAPI spec
./gradlew openApiGenerate

# The generated code is placed in build/generated/src/main/java/
# - API interfaces: com.barclays.eagle_bank_api.api
# - Model classes (DTOs): com.barclays.eagle_bank_api.model
```

**Important**: The code generation happens automatically before compilation, so you typically don't need to run this manually. However, if you modify `openapi.yaml`, you can run this command to regenerate the code.

### Running the Application
```bash
# Start PostgreSQL database (required before running app)
docker compose up

# Run the application
./gradlew bootRun

# Run with hot reload (Spring DevTools enabled)
./gradlew bootRun --continuous
```

### Testing
```bash
# Run all tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.barclays.eagle_bank_api.YourTestClass"

# Run a specific test method
./gradlew test --tests "com.barclays.eagle_bank_api.YourTestClass.testMethod"

# Run tests with coverage
./gradlew test jacocoTestReport
```

### Building
```bash
# Build the project
./gradlew build

# Build without running tests
./gradlew build -x test

# Clean and build
./gradlew clean build
```

### Database Management
```bash
# Flyway migrations run automatically on startup

# To manually apply migrations
./gradlew flywayMigrate

# Check migration status
./gradlew flywayInfo

# Clean database (development only)
./gradlew flywayClean
```

## Architecture

### Package Structure

The codebase follows standard Spring Boot layered architecture under `com.barclays.eagle_bank_api`:

- **api/** (generated) - API interfaces generated from OpenAPI spec
  - `AccountApi` - Bank account operations interface
  - `TransactionApi` - Transaction operations interface
  - `UserApi` - User management interface
  - These are generated at build time and should not be modified

- **model/** (generated) - DTOs generated from OpenAPI spec
  - Request DTOs: `CreateUserRequest`, `UpdateUserRequest`, etc.
  - Response DTOs: `UserResponse`, `BankAccountResponse`, `TransactionResponse`
  - List wrappers: `ListBankAccountsResponse`, `ListTransactionsResponse`
  - Error responses: `ErrorResponse`, `BadRequestErrorResponse`
  - All include proper validation annotations (`@NotNull`, `@Valid`, `@Email`, etc.)
  - These are generated at build time and should not be modified

- **controller/** - REST API endpoint implementations (`@RestController`)
  - Implement the generated API interfaces (e.g., `AccountController implements AccountApi`)
  - `AccountController` - Bank account CRUD operations
  - `TransactionController` - Transaction operations (deposit/withdrawal)
  - `UserController` - User management

- **services/** - Business logic layer (`@Service`)
  - Implements core banking logic
  - Transaction atomicity and balance validation
  - User-account ownership enforcement

- **repositories/** - Data access layer (`@Repository`, Spring Data JPA)
  - Interfaces extending `JpaRepository`
  - Custom query methods as needed

- **entities/** - JPA entities (`@Entity`)
  - `User` - User information with embedded address
  - `BankAccount` - Account details with balance
  - `Transaction` - Immutable transaction records
  - Relationships: User (1-to-many) BankAccount (1-to-many) Transaction


- **security/** - JWT authentication (`@Configuration`)
  - `JwtTokenProvider` - Token generation and validation
  - `JwtAuthenticationFilter` - Request authentication filter
  - `SecurityConfig` - Spring Security configuration
  - `UserDetailsServiceImpl` - Custom user details service

- **config/** - Application configuration
  - Security configuration
  - Bean definitions

- **exception/** - Custom exception handling (`@ControllerAdvice`)
  - `GlobalExceptionHandler` - Centralized exception handling
  - Custom exceptions (InsufficientFundsException, ResourceNotFoundException, etc.)

### Domain Model

**User Entity:**
- ID format: `usr-[A-Za-z0-9]+` (custom generated, not auto-increment)
- Contains embedded Address object (line1-3, town, county, postcode)
- Phone number validation: International format `^\+[1-9]\d{1,14}$`
- Email validation required
- Cannot be deleted if associated with bank accounts (409 Conflict)

**BankAccount Entity:**
- Account number format: `01\d{6}` (starts with "01" + 6 digits)
- Sort code: Fixed value "10-10-10"
- Account type: Currently only "personal" supported (enum for future expansion)
- Balance: Double with 2 decimal places, range 0.00 to 10,000.00
- Currency: GBP only
- Many-to-one relationship with User

**Transaction Entity:**
- ID format: `tan-[A-Za-z0-9]+` (custom generated)
- Type: "deposit" or "withdrawal" (enum)
- Amount: Double with 2 decimal places, range 0.00 to 10,000.00
- Optional reference field (transaction note)
- Stores userId to track who initiated the transaction
- **Immutable**: No update or delete operations allowed (audit trail compliance)
- Many-to-one relationship with BankAccount

### Security Architecture

**JWT Authentication:**
- All endpoints except `POST /v1/users` (registration) require authentication
- Bearer token format with JWT
- Token should contain user ID and expiration
- Authentication filter validates token on each request
- User can only access their own resources (ownership validation)

**Authorization Rules:**
- Users cannot access other users' accounts or transactions
- Return 403 Forbidden for unauthorized access attempts
- Return 401 Unauthorized for missing/invalid tokens

### Database Schema

Flyway migrations are located in `src/main/resources/db/migration/`:

Expected tables:
- `users` - User information with embedded address columns
- `bank_accounts` - Account details with user_id foreign key
- `transactions` - Transaction records with account_number and user_id foreign keys

Indexes should be created on:
- `bank_accounts.user_id` (for listing user accounts)
- `transactions.account_number` (for listing account transactions)
- Custom ID fields for lookup performance

### API Design Patterns

**URL Structure:**
- All endpoints prefixed with `/v1/` for versioning
- RESTful resource naming (accounts, transactions, users)
- Nested routes for transactions: `/v1/accounts/{accountNumber}/transactions`

**HTTP Methods:**
- POST: Create resources (201 Created on success)
- GET: Retrieve resources (200 OK)
- PATCH: Partial updates (200 OK with updated resource)
- DELETE: Remove resources (204 No Content on success)

**Error Handling:**
- 400 Bad Request: Validation errors with detailed field-level feedback
- 401 Unauthorized: Missing or invalid authentication
- 403 Forbidden: User lacks permission to access resource
- 404 Not Found: Resource doesn't exist
- 409 Conflict: Business rule violation (e.g., deleting user with accounts)
- 422 Unprocessable Entity: Insufficient funds for withdrawal
- 500 Internal Server Error: Unexpected errors

**Validation:**
BadRequestErrorResponse includes:
```json
{
  "message": "Request validation failed",
  "details": [
    {
      "field": "email",
      "message": "must be a valid email address",
      "type": "validation_error"
    }
  ]
}
```

### Business Logic Rules

**Account Creation:**
- Generate account number in format `01\d{6}` (unique)
- Set sort code to "10-10-10"
- Initialize balance to 0.00
- Associate with authenticated user

**Transaction Processing:**
- **Deposits**: Add amount to account balance
- **Withdrawals**: Check sufficient funds before processing (422 if insufficient)
- Record userId from JWT token in transaction
- Update account balance atomically
- Transaction creation must be within database transaction scope
- Balance must not exceed 10,000.00 after deposit

**User Deletion:**
- Check if user has any associated bank accounts
- Return 409 Conflict with appropriate message if accounts exist
- Only allow deletion if no accounts exist

### ID Generation Strategy

Custom ID generation required (not database auto-increment):
- Users: `usr-` prefix + random alphanumeric string
- Transactions: `tan-` prefix + random alphanumeric string
- Accounts: `01` prefix + 6-digit sequential or random number

Consider using UUID-based generation or database sequences for uniqueness.

## Key Technical Details

**Java Version:** 21 (LTS)
**Spring Boot:** 3.5.6
**Database:** PostgreSQL (via Docker Compose)
**Build Tool:** Gradle with Kotlin DSL
**Package Name:** `com.barclays.eagle_bank_api` (note: underscore due to invalid hyphen in original)

**Dependencies:**
- Spring Data JPA (database ORM)
- Spring Security (authentication/authorization)
- Spring Web (REST API)
- Flyway (database migrations)
- PostgreSQL driver
- Spring Boot DevTools (hot reload)
- Testcontainers (isolated test databases)

**Testing Strategy:**
- JUnit 5 (Jupiter)
- Spring Boot Test framework
- Testcontainers for integration tests with PostgreSQL
- Spring Security Test for authentication testing

## OpenAPI Specification & Code Generation

The complete API contract is defined in `openapi.yaml` at the project root. All implementation must conform to this specification.

**Code Generation Workflow**:
1. API interfaces and DTOs are automatically generated from `openapi.yaml` during build
2. Generated code is placed in `build/generated/src/main/java/`
3. Controllers implement the generated API interfaces
4. The generator includes all validation annotations from the OpenAPI spec
5. Generated code should never be manually edited - modify `openapi.yaml` instead

**Implementation Pattern**:
```java
@RestController
public class AccountController implements AccountApi {
    @Override
    public ResponseEntity<BankAccountResponse> createAccount(
            CreateBankAccountRequest request) {
        // Your implementation here
    }
}
```

**Benefits**:
- Type-safe API implementation
- Automatic validation from OpenAPI spec
- OpenAPI documentation annotations included
- Compile-time checking that implementation matches spec
- No manual DTO creation needed

## Database Configuration

Development database via Docker Compose (`compose.yaml`):
- Database: mydatabase
- User: myuser
- Password: secret
- Port: 5432 (mapped)

For tests, Testcontainers automatically provisions isolated PostgreSQL instances.