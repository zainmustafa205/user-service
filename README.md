# User Service

Handles user registration, login, and JWT-based authentication.

## Port
`8081`

## Tech Stack
- Spring Boot 3.5.15
- Spring Security + JWT (JJWT 0.12.6)
- Microsoft SQL Server
- Spring Cloud Netflix Eureka Client

## Database Setup
Create the database manually in SSMS before running:
```sql
CREATE DATABASE userservice_db;
```
Tables are auto-created by JPA on first run.

## Environment Variables
Set these before running the service:

| Variable         | Description            |
|------------------|------------------------|
| `DB_USERNAME`    | SQL Server username    |
| `DB_PASSWORD`    | SQL Server password    |
| `JWT_SECRET_KEY` | 256-bit hex secret key |

## API Endpoints

|Method| Endpoint                | Auth Required | Description            |
|------|-------------------------|---------------|------------------------|
| POST | `/v1/auth/register` | ❌            | Register new user      |
| POST | `/v1/auth/login`    | ❌            | Login, returns JWTnn   |
| GET  | `/v1/auth/me`       | ✅ Bearer     | Get current user info  |

## Running Locally
Make sure Eureka Server is running on port `8761` before starting this service.

```bash
./mvnw.cmd spring-boot:run
```

## Register Request Example
```json
{
  "fullName": "Zain Ahmed",
  "email": "zain@example.com",
  "password": "secret123"
}
```

## Login Request Example
```json
{
  "email": "zain@example.com",
  "password": "secret123"
}
```