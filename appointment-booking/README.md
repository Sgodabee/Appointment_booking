# Appointment Booking System

A production-grade, full-stack web application that allows customers to schedule branch appointments and receive simulated email confirmations.

**Backend:** Java 17 · Spring Boot 3.2 · PostgreSQL · Flyway · JUnit 5  
**Frontend:** React 18 · Vite 5 · Tailwind CSS 3  
**Infrastructure:** Docker · Docker Compose · Nginx

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Tech Stack](#tech-stack)
3. [Security Features](#security-features)
4. [Project Structure](#project-structure)
5. [Prerequisites](#prerequisites)
6. [Quick Start — Docker (Recommended)](#quick-start--docker-recommended)
7. [Local Development (without Docker)](#local-development-without-docker)
8. [Environment Variables](#environment-variables)
9. [Database Migrations](#database-migrations)
10. [API Reference](#api-reference)
11. [Running Tests](#running-tests)
12. [Design Decisions](#design-decisions)
13. [Application Pages](#application-pages)
14. [Production Checklist](#production-checklist)

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Browser / Mobile                              │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ HTTP
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│   Frontend  (React 18 + Vite + Tailwind CSS)  — Nginx on port 3000  │
│                                                                      │
│   Pages: Home · Book (3-step wizard) · Confirmation ·               │
│          My Appointment · Cancel · Admin Dashboard                  │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ REST  /api/v1
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│   Backend  (Java 17 · Spring Boot 3.2)  — port 5000                 │
│                                                                      │
│   Controller → Service → Repository → JPA (Hibernate)               │
│                                                                      │
│   Layers:                                                            │
│     controller/   Thin HTTP layer — @Valid, ApiResponse<T>           │
│     service/      Business logic, encryption, email, audit           │
│     repository/   Spring Data JPA, JPQL named parameters only        │
│     domain/       JPA entities, enums (no business logic)            │
│     dto/          Request/Response POJOs + Bean Validation           │
│     mapper/       MapStruct compile-time mappers                     │
│     util/         EncryptionService, ReferenceGenerator, TimeSlots   │
│     security/     Spring Security — stateless, CORS, headers         │
│     exception/    Typed exceptions + GlobalExceptionHandler          │
└───────────────────────────────┬──────────────────────────────────────┘
                                │ JDBC (HikariCP pool)
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│   PostgreSQL 16  — port 5432                                        │
│                                                                      │
│   Tables: branches · appointments · appointment_audit_log           │
│   Migrations: Flyway (V1__initial_schema.sql)                       │
└──────────────────────────────────────────────────────────────────────┘
```

### Clean Architecture Layers (Backend)

| Layer           | Package              | Responsibility                                       |
|-----------------|----------------------|------------------------------------------------------|
| **Controller**  | `controller/`        | Parse HTTP input, call service, format response      |
| **Service**     | `service/`           | Business rules, orchestration, side-effects (email)  |
| **Repository**  | `repository/`        | All DB access — JPQL named params, zero raw SQL      |
| **Domain**      | `domain/model`       | JPA entities with lifecycle annotations              |
| **Enums**       | `domain/enums`       | AppointmentStatus, ServiceType                       |
| **DTO**         | `dto/request|response` | API contracts + Bean Validation constraints        |
| **Mapper**      | `mapper/`            | MapStruct — compile-time null-safe mapping           |
| **Util**        | `util/`              | EncryptionService, ReferenceGenerator, TimeSlots     |
| **Validation**  | `validation/`        | @ValidSaId custom annotation + Luhn validator        |
| **Security**    | `security/`          | SecurityFilterChain, CORS, headers                   |
| **Exception**   | `exception/`         | Typed exceptions + GlobalExceptionHandler            |
| **Config**      | `config/`            | AppProperties (startup-validated typed config)       |

---

## Tech Stack

| Area         | Technology                                                         |
|--------------|--------------------------------------------------------------------|
| Language     | Java 17 (LTS)                                                      |
| Framework    | Spring Boot 3.2.5                                                  |
| Persistence  | Spring Data JPA · Hibernate · HikariCP                             |
| Database     | PostgreSQL 16                                                      |
| Migrations   | Flyway 10                                                          |
| Validation   | Jakarta Bean Validation 3 (Hibernate Validator)                    |
| Security     | Spring Security 6 (stateless)                                      |
| Email        | Spring Mail · Thymeleaf HTML templates · Ethereal (simulated)      |
| Mapping      | MapStruct 1.5 (compile-time, zero reflection)                      |
| Boilerplate  | Lombok 1.18                                                        |
| Testing      | JUnit 5 · Mockito · AssertJ                                        |
| Frontend     | React 18 · Vite 5 · Tailwind CSS 3 · React Hook Form · Yup · Axios |
| Container    | Docker (multi-stage) · Docker Compose · Nginx 1.27                 |

---

## Security Features

### SQL Injection Prevention

Every database interaction uses **JPQL named parameters** (`:param`). Spring Data JPA compiles these to `PreparedStatement` objects — user input is **never** concatenated into a SQL string at any point in the codebase.

```java
// ✅ Every query looks like this — parameterized
@Query("SELECT a FROM Appointment a WHERE a.referenceNumber = :ref")
Optional<Appointment> findByReferenceNumber(@Param("ref") String referenceNumber);

// ❌ This pattern does not exist anywhere in the codebase
// "SELECT * FROM appointments WHERE ref = '" + ref + "'"
```

### Input Validation (Defence in Depth)

All input is validated on **two independent layers**:

| Layer | Tool | What it checks |
|---|---|---|
| **Frontend** | Yup schema | Format, required fields, SA ID Luhn |
| **Backend** | Bean Validation (`@Valid`) | All constraints re-checked server-side — frontend validation is never trusted |

Custom constraints written from scratch:
- **`@ValidSaId`** — 13-digit format + **Luhn checksum algorithm** for SA ID numbers
- Regex patterns on name (`^[a-zA-Z\s'\-]+$`), phone, time, reference number
- `@Future` on appointment date (must be in the future)
- `@Size` limits on all string fields

### AES-256 Encryption at Rest

SA ID numbers are **AES-256-CBC encrypted** in `EncryptionService` before the JPA entity is saved. The plaintext ID **never reaches the database**.

```
Customer submits ID → EncryptionService.encrypt()
    → Generates 16-byte random IV (SecureRandom)
    → AES/CBC/PKCS5Padding cipher
    → Stores "ivHex:base64Ciphertext" in DB column
    → Plaintext destroyed

API Response → idNumber field is EXCLUDED by AppointmentMapper
    → Encrypted value never returned to any client
```

Key points:
- Random IV per encryption call → same plaintext produces different ciphertext every time (prevents frequency analysis)
- Key sourced from `ENCRYPTION_KEY` env var, validated at startup (≥ 32 chars)
- Uses JDK `javax.crypto` — no third-party crypto library
- `AppointmentMapper` explicitly omits `idNumber` from all API responses

### Other Security Measures

- **Spring Security headers** — X-Frame-Options: DENY, X-Content-Type-Options: nosniff, Referrer-Policy, Cache-Control
- **CORS whitelist** — only configured origins accepted (`CORS_ORIGIN` env var)
- **Stateless session** — `SessionCreationPolicy.STATELESS`, no server-side session
- **GlobalExceptionHandler** — programming errors return a generic 500; stack traces and internal details are **never** exposed to clients (only logged server-side)
- **Non-root Docker container** — runs as `appuser`, not `root`
- **`@EnableAsync` email** — email failures never cause booking failures; errors logged only
- **Audit log** — every status change is immutably recorded in `appointment_audit_log`

---

## Project Structure

```
appointment-booking/
├── docker-compose.yml
├── .env.example
├── .gitignore
├── README.md
│
├── backend/                              ← Java 17 Spring Boot
│   ├── Dockerfile                        ← Multi-stage (JDK build → JRE runtime)
│   ├── mvnw                              ← Maven wrapper
│   ├── pom.xml                           ← Spring Boot 3.2.5, Java 17
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/appointmentbooking/
│   │   │   │   ├── AppointmentBookingApplication.java
│   │   │   │   ├── config/
│   │   │   │   │   └── AppProperties.java         ← Typed, startup-validated config
│   │   │   │   ├── controller/
│   │   │   │   │   ├── AppointmentController.java
│   │   │   │   │   ├── BranchController.java
│   │   │   │   │   └── HealthController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── enums/  AppointmentStatus, ServiceType
│   │   │   │   │   └── model/  Appointment, Branch, AppointmentAuditLog
│   │   │   │   ├── dto/
│   │   │   │   │   ├── request/  BookAppointmentRequest, CancelAppointmentRequest,
│   │   │   │   │   │             UpdateStatusRequest
│   │   │   │   │   └── response/ AppointmentResponse, BranchResponse,
│   │   │   │   │                 AvailabilityResponse, ApiResponse<T>
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── SlotUnavailableException.java
│   │   │   │   │   └── EncryptionException.java
│   │   │   │   ├── mapper/
│   │   │   │   │   ├── AppointmentMapper.java     ← idNumber excluded
│   │   │   │   │   └── BranchMapper.java
│   │   │   │   ├── repository/
│   │   │   │   │   ├── AppointmentRepository.java ← All JPQL named params
│   │   │   │   │   ├── BranchRepository.java
│   │   │   │   │   └── AppointmentAuditLogRepository.java
│   │   │   │   ├── security/
│   │   │   │   │   └── SecurityConfig.java
│   │   │   │   ├── service/
│   │   │   │   │   ├── AppointmentService.java
│   │   │   │   │   ├── BranchService.java
│   │   │   │   │   └── EmailService.java          ← @Async, Thymeleaf templates
│   │   │   │   ├── util/
│   │   │   │   │   ├── EncryptionService.java     ← AES-256-CBC, random IV
│   │   │   │   │   ├── ReferenceGenerator.java    ← SecureRandom
│   │   │   │   │   └── TimeSlotGenerator.java
│   │   │   │   └── validation/
│   │   │   │       ├── ValidSaId.java             ← Custom annotation
│   │   │   │       └── SaIdValidator.java         ← Luhn algorithm
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       ├── db/migration/
│   │   │       │   └── V1__initial_schema.sql
│   │   │       └── templates/email/
│   │   │           ├── confirmation.html
│   │   │           └── cancellation.html
│   │   └── test/
│   │       └── java/com/appointmentbooking/
│   │           ├── service/
│   │           │   ├── AppointmentServiceTest.java  ← 13 Mockito cases
│   │           │   ├── BranchServiceTest.java
│   │           │   └── GlobalExceptionHandlerTest.java
│   │           ├── util/
│   │           │   ├── EncryptionServiceTest.java   ← 10 cases
│   │           │   ├── ReferenceGeneratorTest.java
│   │           │   └── TimeSlotGeneratorTest.java
│   │           └── validation/
│   │               └── SaIdValidatorTest.java       ← 8 cases
│
└── frontend/                             ← React 18 + Vite + Tailwind
    ├── Dockerfile                        ← Multi-stage (Node build → Nginx)
    ├── nginx.conf
    ├── src/
    │   ├── pages/   Home, Book, Confirmation, Lookup, Cancel, Admin, 404
    │   ├── api/     Axios client + appointments.js + branches.js
    │   ├── hooks/   useBranches, useAvailableSlots
    │   ├── components/  layout/ + ui/ (Spinner, Alert, Badge, StepIndicator)
    │   └── utils/   validation.js (Yup + SA ID Luhn), formatters.js
    └── public/
```

---

## Prerequisites

| Tool           | Minimum Version | Check Command            |
|----------------|-----------------|--------------------------|
| Docker         | 24+             | `docker --version`       |
| Docker Compose | 2.20+           | `docker compose version` |
| Java           | 17+             | `java -version`          |
| Maven          | 3.9+            | `mvn -version`           |
| Node.js        | 18+             | `node --version`         |

---

## Quick Start — Docker (Recommended)

```bash
# 1. Clone the repository
git clone <your-repo-url>
cd appointment-booking

# 2. Create your .env file
cp .env.example .env
# Edit .env — set strong values for DB_PASSWORD and ENCRYPTION_KEY

# 3. Build and start all three services
docker compose up --build

# Application URLs:
#   Frontend  → http://localhost:3000
#   API       → http://localhost:5000/api/v1
#   DB        → localhost:5432  (user: postgres)
```

Flyway runs migrations automatically on backend startup. Five sample branches are seeded on first run.

### Useful Docker commands

```bash
# Run in background
docker compose up -d --build

# View logs
docker compose logs -f backend
docker compose logs -f frontend

# Run backend unit tests inside the container
docker compose exec backend ./mvnw test

# Connect to the database
docker compose exec postgres psql -U postgres -d appointment_booking

# Stop (keep data)
docker compose down

# Stop and wipe all data
docker compose down -v
```

---

## Local Development (without Docker)

### 1. PostgreSQL

```sql
-- Create the database
CREATE DATABASE appointment_booking;
```

### 2. Backend

```bash
cd backend

# Copy and configure environment
cp src/main/resources/application-dev.yml /tmp/check   # review dev defaults
# Edit src/main/resources/application-dev.yml or set env vars:
export DB_PASSWORD=your_password
export ENCRYPTION_KEY=32charEncryptionKeyForAES256!!!X
export SPRING_PROFILES_ACTIVE=dev

# Run (Flyway migrates automatically on startup)
./mvnw spring-boot:run

# API available at: http://localhost:5000/api/v1
# Health check:    http://localhost:5000/actuator/health
```

### 3. Frontend

```bash
cd frontend
npm install

# Configure API URL
echo "VITE_API_BASE_URL=http://localhost:5000/api/v1" > .env

npm run dev
# UI available at: http://localhost:3000
```

---

## Environment Variables

### Backend

| Variable               | Required | Default                        | Description                                  |
|------------------------|----------|--------------------------------|----------------------------------------------|
| `SPRING_PROFILES_ACTIVE` | No     | `dev`                          | `dev` or `prod`                              |
| `PORT`                 | No       | `5000`                         | HTTP server port                             |
| `DB_HOST`              | Yes      | `localhost`                    | PostgreSQL host                              |
| `DB_PORT`              | No       | `5432`                         | PostgreSQL port                              |
| `DB_NAME`              | Yes      | `appointment_booking`          | Database name                                |
| `DB_USER`              | Yes      | `postgres`                     | Database username                            |
| `DB_PASSWORD`          | Yes      | —                              | Database password                            |
| `DB_POOL_MAX`          | No       | `20`                           | HikariCP max pool size                       |
| `ENCRYPTION_KEY`       | Yes      | —                              | **≥ 32 chars** — AES-256 key for ID numbers  |
| `SMTP_HOST`            | No       | `smtp.ethereal.email`          | SMTP server (blank = simulated)              |
| `SMTP_PORT`            | No       | `587`                          | SMTP port                                    |
| `SMTP_USER`            | No       | —                              | SMTP username (blank = use Ethereal)         |
| `SMTP_PASS`            | No       | —                              | SMTP password                                |
| `EMAIL_FROM`           | No       | `noreply@appointmentbooking.co.za` | Sender address                           |
| `CORS_ORIGIN`          | No       | `http://localhost:3000`        | Allowed CORS origin(s), comma-separated      |

### Frontend

| Variable            | Required | Default                          | Description              |
|---------------------|----------|----------------------------------|--------------------------|
| `VITE_API_BASE_URL` | Yes      | `http://localhost:5000/api/v1`   | Backend API base URL     |

---

## Database Migrations

Managed by **Flyway**. Migrations run automatically on startup.

```
src/main/resources/db/migration/
└── V1__initial_schema.sql    ← Tables, indexes, triggers, 5 seed branches
```

To run migrations manually:

```bash
cd backend
./mvnw flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/appointment_booking \
  -Dflyway.user=postgres -Dflyway.password=your_password
```

### Schema

```
branches
  id, name, address, city, province, phone, email,
  operating_hours (TEXT/JSON), is_active, created_at, updated_at

appointments
  id, reference_number (UNIQUE), customer_name, customer_email,
  customer_phone, id_number (AES-256 encrypted TEXT),
  branch_id (FK → branches), service_type (ENUM stored as VARCHAR),
  appointment_date, appointment_time, notes, status (CHECK constraint),
  created_at, updated_at

appointment_audit_log
  id, appointment_id (FK → appointments), action, old_status,
  new_status, changed_by, changed_at
```

---

## API Reference

Base URL: `http://localhost:5000/api/v1`

All responses use the `ApiResponse<T>` envelope:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "pagination": { "total": 25, "page": 1, "size": 20, "totalPages": 2 }
}
```

### Appointments

| Method | Endpoint                              | Description                          |
|--------|---------------------------------------|--------------------------------------|
| `POST`   | `/appointments`                     | Book a new appointment               |
| `GET`    | `/appointments/availability`        | Get available time slots             |
| `GET`    | `/appointments/{reference}`         | Look up by reference number          |
| `POST`   | `/appointments/cancel`              | Cancel an appointment                |
| `GET`    | `/appointments/admin/list`          | List all appointments (admin)        |
| `PATCH`  | `/appointments/admin/{id}/status`   | Update status (admin)                |

### Branches

| Method | Endpoint        | Description              |
|--------|-----------------|--------------------------|
| `GET`  | `/branches`     | List all active branches |
| `GET`  | `/branches/{id}`| Get branch by ID         |

### Health

| Method | Endpoint            | Description                       |
|--------|---------------------|-----------------------------------|
| `GET`  | `/api/v1/health`    | DB status + uptime                |
| `GET`  | `/actuator/health`  | Spring Boot health (Liveness/Readiness) |

### Book Appointment — Request Body

```json
{
  "customerName":    "Jane Doe",
  "customerEmail":   "jane@example.com",
  "customerPhone":   "+27 82 123 4567",
  "idNumber":        "8001015009087",
  "branchId":        1,
  "serviceType":     "ACCOUNT_OPENING",
  "appointmentDate": "2026-10-15",
  "appointmentTime": "09:00",
  "notes":           "Please bring proof of address."
}
```

**Service types:** `ACCOUNT_OPENING` · `LOAN_APPLICATION` · `CARD_SERVICES` · `GENERAL_ENQUIRY` · `DOCUMENT_SUBMISSION` · `INVESTMENT_ADVICE`

### Successful Booking — Response `201`

```json
{
  "success": true,
  "message": "Appointment booked successfully. A confirmation has been sent to your email.",
  "data": {
    "id": 1,
    "referenceNumber": "APB-20261015-A3F7",
    "customerName": "Jane Doe",
    "customerEmail": "jane@example.com",
    "branchId": 1,
    "branchName": "Cape Town City Centre",
    "serviceType": "Account Opening",
    "appointmentDate": "2026-10-15",
    "appointmentTime": "09:00",
    "status": "CONFIRMED"
  }
}
```

### Validation Error — Response `422`

```json
{
  "success": false,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "errors": [
    { "field": "customerEmail", "message": "Invalid email address" },
    { "field": "idNumber",      "message": "Invalid South African ID number" }
  ]
}
```

---

## Running Tests

```bash
cd backend

# Run all unit tests
./mvnw test

# Run with coverage report (output in target/site/jacoco)
./mvnw test jacoco:report

# Run a specific test class
./mvnw test -Dtest=AppointmentServiceTest

# Run inside Docker
docker compose exec backend ./mvnw test
```

### Test Coverage Summary

| Test Class                      | Cases | What it covers                                              |
|---------------------------------|-------|-------------------------------------------------------------|
| `SaIdValidatorTest`             | 8     | Luhn algorithm, 13-digit format, null/blank optional field  |
| `EncryptionServiceTest`         | 10    | AES-256 round-trip, non-deterministic IV, null handling, malformed input |
| `ReferenceGeneratorTest`        | 3     | Format regex, date inclusion, uniqueness across 200 calls   |
| `TimeSlotGeneratorTest`         | 6     | Weekday (18 slots), Saturday (8), Sunday (0), taken exclusion |
| `AppointmentServiceTest`        | 13    | Book (happy path, branch not found, slot taken, ID encrypted, email resilience), lookup, availability, cancel, status update |
| `BranchServiceTest`             | 4     | getAllBranches, getById, not-found error                     |
| `GlobalExceptionHandlerTest`    | 4     | 404/409/500 mapping, no internal leak verification          |
| **Total**                       | **48**| All pure unit tests — no DB, no Spring context needed       |

All tests use **Mockito** for dependency isolation. No real database or network calls are made during testing.

---

## Design Decisions

### Java 17 + Spring Boot 3.2
Spring Boot 3.x requires Java 17 as the minimum. Java 17 is the current LTS release with virtual threads support (Project Loom), record classes, and sealed interfaces. Spring Boot 3.2 provides first-class GraalVM native image support for future optimisation.

### Why JPQL named parameters instead of native SQL?
Spring Data JPA's `@Query` with `:param` placeholders are compiled to `PreparedStatement` by Hibernate. Unlike native SQL string building, parameterized JPQL makes SQL injection **structurally impossible** regardless of what is passed as a parameter value. The pattern is enforced consistently across all repository methods.

### Why `javax.crypto` AES-256 instead of a library?
Java's built-in `javax.crypto` with `AES/CBC/PKCS5Padding` provides AES-256 without any third-party dependency. A fresh `SecureRandom` IV is generated per encryption call, stored as a hex prefix (`ivHex:base64Ciphertext`), meaning the same plaintext always produces a unique ciphertext — preventing frequency analysis attacks on the stored column.

### Why MapStruct over manual mapping or ModelMapper?
MapStruct generates mapping code **at compile time**. This means:
- Zero runtime reflection (faster than ModelMapper)
- Compile errors if fields are renamed — mapping breaks are caught early
- The `idNumber` field exclusion in `AppointmentMapper` is explicit and auditable

### Why Flyway instead of `ddl-auto: create`?
Flyway provides versioned, idempotent, auditable schema migrations. `ddl-auto: create` or `update` is dangerous in production — it can silently drop or alter columns. Flyway's `baseline-on-migrate` allows the first migration to run cleanly on an existing database.

### Why async email with `@EnableAsync`?
SMTP calls can take 1–5 seconds. Making the HTTP booking response wait for email delivery would significantly degrade perceived performance. `@Async` moves the email call to a separate thread pool. If SMTP fails, the error is logged but the booking is already committed — email failure must never cause a booking failure.

### Why is the Admin dashboard unauthenticated?
For the evaluation submission, admin endpoints are publicly accessible so reviewers can test the full feature set without setting up credentials. The `SecurityConfig` is structured so that adding `@PreAuthorize("hasRole('ADMIN')")` to the admin endpoints, combined with JWT filter configuration, is a minimal change.

### Multi-stage Docker build
- **Build stage** (`eclipse-temurin:17-jdk-alpine`): Maven downloads dependencies (cached by Docker layer), compiles, and packages the fat JAR.
- **Runtime stage** (`eclipse-temurin:17-jre-alpine`): Only the JRE and the fat JAR. No Maven, no source code, no JDK in production. Image is ~180 MB vs ~400 MB for a full JDK image.
- Runs as non-root `appuser` with `-XX:+UseContainerSupport` and `-XX:MaxRAMPercentage=75.0`.

---

## Application Pages

| Page              | Route          | Description                                            |
|-------------------|----------------|--------------------------------------------------------|
| Home              | `/`            | Hero, features, services overview, how-it-works        |
| Book Appointment  | `/book`        | 3-step wizard: details → schedule → review & confirm   |
| Confirmation      | `/confirmation`| Reference card, email preview link, reminder checklist |
| My Appointment    | `/lookup`      | Look up any appointment by reference number            |
| Cancel            | `/cancel`      | Cancel by reference number + email verification        |
| Admin Dashboard   | `/admin`       | Filter/view all appointments, update statuses inline   |
| 404               | `/*`           | Friendly not-found page                                |

---

## Production Checklist

- [ ] Set strong `DB_PASSWORD` and `ENCRYPTION_KEY` via a secrets manager (AWS Secrets Manager, HashiCorp Vault)
- [ ] Configure real SMTP credentials (`SMTP_USER`, `SMTP_PASS`) for production email delivery
- [ ] Add HTTPS/TLS termination to Nginx (Let's Encrypt / cert-manager)
- [ ] Add JWT authentication to `/api/v1/appointments/admin/**` endpoints
- [ ] Set `CORS_ORIGIN` to your actual domain (not `localhost`)
- [ ] Enable Flyway `out-of-order: false` and protect migrations from manual edits
- [ ] Configure log aggregation (ELK / CloudWatch / Datadog) — logs are structured JSON in prod profile
- [ ] Set `management.endpoint.health.show-details: when-authorized` for the Actuator endpoint

---

*Built as a Software Engineer evaluation submission — Java 17 · Spring Boot 3.2 · Production-grade patterns applied throughout.*
