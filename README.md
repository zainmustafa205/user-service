# User Service

Authentication and user management microservice for the E-Commerce Microservices system — handles user registration, login, JWT token issuance, and user identity resolution for downstream services. Built as part of an industrial-level Spring Boot microservices portfolio project.

![Java](https://img.shields.io/badge/Java-17-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.15-brightgreen) ![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2024.0.1-blue) ![SQL Server](https://img.shields.io/badge/Database-SQL%20Server-red) ![License](https://img.shields.io/badge/License-MIT-lightgrey)

## 📖 Overview

`user-service` is one of Six microservices in a larger e-commerce system. It owns the **user and authentication domain** — registering new users, validating credentials at login, issuing signed JWT tokens, and exposing user identity endpoints that other services call to verify callers. It is the **only service in the system that issues JWTs** — all other services only validate tokens signed here.

> This service is part of a larger system. See the [main project README](https://github.com/zainmustafa205/ecommerce-microservices) for the full architecture and links to all repositories.

### Part of the E-Commerce Microservices Ecosystem

| Service | Responsibility | Port |
|---|---|---|
| eureka-server | Service discovery/registry | 8761 |
| api-gateway | Single entry point, routing | 8080 |
| **user-service** | **Authentication, JWT issuance, user management** | **8081** |
| product-service | Product catalog, CRUD, filtering | 8082 |
| order-service | Orders, cart, calls product-service, user-service & payment-service via Feign | 8083 |
| payment-service | Simulated payment gateway flow | 8084 |

## 🏗️ Architecture & Design Decisions

This service follows a strict layered architecture:

```
Controller → Service → Repository → Database
                ↓
        Spring Security Filter Chain
                ↓
           JWT Auth Filter → JwtService → SecurityContext
```

Key design decisions and the reasoning behind them:

| Decision | Reasoning |
|---|---|
| **This service is the sole JWT issuer in the system** | Centralizing token issuance means all other services share one signing secret and one token format. If each service issued its own tokens, secret management and token validation would fragment across the system. |
| **`userId` embedded as a JWT claim** | Downstream services (order-service, payment-service) need the caller's database ID without making an extra network call to user-service on every request. Embedding it in the token payload makes identity resolution zero-cost for other services. |
| **BCrypt password hashing** | Industry-standard adaptive hashing — salted per password, computationally expensive to brute-force. Raw or MD5/SHA hashing is never acceptable for user credentials. |
| **Stateless session management (`STATELESS`)** | No `HttpSession` is created or stored. Every request is authenticated from the JWT alone — consistent with a distributed, horizontally scalable microservices architecture where sticky sessions would break load balancing. |
| **`UserDetails` implemented directly on the `User` entity** | Avoids a redundant adapter class. The `User` entity is the single source of truth for identity — Spring Security reads authorities and credentials directly from it without an extra mapping layer. |
| **Custom `JwtAuthenticationFilter` runs before `UsernamePasswordAuthenticationFilter`** | Ensures JWT-based authentication is evaluated on every request before Spring Security's default form-login logic. Requests with a valid token are authenticated in the `SecurityContext` before any authorization check runs. |
| **`/v1/auth/**` permitted without authentication, all other routes require auth** | Register and login are necessarily public — a user cannot authenticate before they have credentials. Every other endpoint (e.g. `/v1/users/{id}`) requires a valid token, so internal Feign calls from other services must present a JWT. |
| **Base64 decoding of the JWT secret (`Decoders.BASE64.decode()`)** | The secret is stored and shared as a Base64-encoded string (generated via `openssl rand -base64 32`). Decoding it to raw bytes before building the signing key is mandatory — passing the Base64 string as raw bytes produces a different key, silently breaking token validation in every service that tries to verify the token. |
| **DTOs for all API boundaries** | Entities are never serialized directly into responses. Request and response shapes are explicitly defined and decoupled from the persistence model — protecting internal field names and preventing accidental exposure of sensitive fields like `password`. |

## 🛠️ Tech Stack

- Java 17
- Spring Boot 3.5.15
- Spring Cloud 2024.0.1 (Eureka Client)
- Spring Data JPA (Hibernate)
- Spring Security (stateless, JWT-based)
- JJWT 0.12.6
- Spring Validation (`@Valid`)
- Microsoft SQL Server
- Lombok
- Maven

## 📁 Project Structure

```
user-service/
├── src/main/java/com/ecommerce/userservice/
│   ├── UserServiceApplication.java
│   ├── config/
│   │   └── SecurityConfig.java          # Filter chain, BCrypt bean, AuthenticationManager
│   ├── controller/
│   │   └── AuthController.java          # Register, login, /me, /users/{id}
│   ├── dto/
│   │   ├── request/
│   │   │   ├── RegisterRequest.java
│   │   │   └── LoginRequest.java
│   │   └── response/
│   │       ├── AuthResponse.java
│   │       └── UserResponse.java
│   ├── entity/
│   │   └── User.java                    # Implements UserDetails
│   ├── enums/
│   │   └── Role.java                    # ROLE_USER, ROLE_ADMIN
│   ├── exception/
│   │   ├── GlobalExceptionHandler.java
│   │   └── UserAlreadyExistsException.java
│   ├── repository/
│   │   └── UserRepository.java
│   ├── security/
│   │   ├── JwtService.java              # Token generation and validation
│   │   ├── JwtAuthenticationFilter.java # Per-request JWT verification
│   │   └── UserDetailsServiceImpl.java
│   └── service/
│       ├── AuthService.java
│       └── impl/
│           └── AuthServiceImpl.java
└── src/main/resources/
    └── application.yml
```

## 🔌 API Endpoints

### Public Endpoints (no token required)

| Method | Endpoint | Description | Request Body |
|---|---|---|---|
| POST | `/v1/auth/register` | Register a new user — returns a signed JWT | `RegisterRequest` |
| POST | `/v1/auth/login` | Authenticate with credentials — returns a signed JWT | `LoginRequest` |

### Protected Endpoints (Bearer token required)

| Method | Endpoint | Description |
|---|---|---|
| GET | `/v1/auth/me` | Get the currently authenticated user's profile |
| GET | `/v1/users/{id}` | Get a user by ID — called internally by other services via Feign |

> `GET /v1/users/{id}` is primarily used by `order-service` to verify that a user record exists before processing an order. The caller must present a valid JWT (forwarded automatically by the Feign `RequestInterceptor` in the calling service).

## 📦 Sample Request/Response

**Register** — `POST /v1/auth/register`

```json
{
  "fullName": "Zain Ahmed",
  "email": "zain@example.com",
  "password": "secret123"
}
```

**Response — `201 Created`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "email": "zain@example.com",
  "role": "ROLE_USER"
}
```

**Login** — `POST /v1/auth/login`

```json
{
  "email": "zain@example.com",
  "password": "secret123"
}
```

**Response — `200 OK`**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "email": "zain@example.com",
  "role": "ROLE_USER"
}
```

**Error Response — `409 Conflict`** *(duplicate email on register)*

```json
{
  "timestamp": "2026-07-13T10:14:22",
  "status": 409,
  "error": "Conflict",
  "message": "User already exists with email: zain@example.com"
}
```

**Error Response — `401 Unauthorized`** *(wrong credentials on login)*

```json
{
  "timestamp": "2026-07-13T10:15:01",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

**Get Current User** — `GET /v1/auth/me`
`Authorization: Bearer <token>`

**Response — `200 OK`**

```json
{
  "id": 7,
  "fullName": "Zain Ahmed",
  "email": "zain@example.com",
  "role": "ROLE_USER"
}
```

## ⚙️ Setup & Installation

### Prerequisites

- Java 17+
- Maven 3.8+
- Microsoft SQL Server running locally (or an accessible instance)
- `eureka-server` running on port 8761

### 1. Clone the repository

```bash
git clone https://github.com/zainmustafa205/user-service.git
cd user-service
```

### 2. Create the database

SQL Server requires manual database creation — JPA/Hibernate only creates tables, not the database itself:

```sql
CREATE DATABASE userservice_db;
```

Tables (`users`) are created automatically by Hibernate on first startup via `ddl-auto: update`.

### 3. Set environment variables

```bash
export DB_USERNAME=${DB_user_name}
export DB_PASSWORD=${DB_Password_user}
export JWT_SECRET_KEY=${JWT_SECRET}
```

Generate a secure secret key (run once, store and reuse across all services):

```bash
openssl rand -base64 32
```

> **Critical:** The `JWT_SECRET_KEY` **must be identical** across every service that validates tokens (`order-service`, `product-service`, `payment-service`). A mismatch causes JWT validation to fail silently with no obvious error at startup. The key must also be decoded with `Decoders.BASE64.decode()` — not as raw bytes — in every service.


### 4. Run the application

```bash
mvn spring-boot:run
```

The service starts on port `8081` and registers itself with Eureka at `http://localhost:8761`.

### 5. Verify registration

Visit `http://localhost:8761` and confirm `USER-SERVICE` appears in the list of registered instances.

## 🔒 Configuration Notes

- `spring.jpa.hibernate.ddl-auto` is set to `update` — suitable for development. Use migration tools (Flyway/Liquibase) for production.
- This service is the **only JWT issuer** in the system. All other services validate tokens using the same shared secret but never generate new ones.
- The JWT payload includes `userId` as a custom claim — downstream services extract this to identify the caller without making an additional Feign call to this service.
- The auth controller base path is `/v1/auth` (not `/api/v1/auth`) — the API Gateway strips the `/api/users` prefix via `StripPrefix=2` before forwarding, so the service receives requests at `/v1/...` directly.
- `403 Forbidden` on login usually means the `SecurityConfig` `permitAll()` path does not match the controller's actual base path — both must be in sync.

## 🗺️ Roadmap

- [ ] Add email verification on registration
- [ ] Add password reset flow
- [ ] Expose a token introspection endpoint for service-to-service validation
- [ ] Restrict `/v1/users/{id}` to service-role callers only
- [ ] Add Flyway migrations for schema versioning
- [ ] Add unit and integration tests

## 📄 License

This project is part of a personal portfolio and is available under the MIT License.

## 🔗 Related Repositories

- [eureka-server](https://github.com/zainmustafa205/eureka-server)
- [api-gateway](https://github.com/zainmustafa205/api-gateway)
- [product-service](https://github.com/zainmustafa205/product-service)
- [order-service](https://github.com/zainmustafa205/order-service)
- [payment-service](https://github.com/zainmustafa205/payment-service)
