# Capitec Branch Appointment Booking System

A full-stack web application that allows Capitec Bank customers to book, manage, and cancel branch appointments online — eliminating physical queues and improving the in-branch experience.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, React Hook Form, Yup, Tailwind CSS, Vite |
| Backend | Java 17, Spring Boot 3.2, Spring Security |
| Auth | JWT (HS256) — 8-hour employee sessions, 1-hour customer sessions |
| Database | PostgreSQL 16 with Flyway migrations |
| Email | JavaMail + Thymeleaf HTML templates |
| Security | AES-256-CBC (ID numbers), bcrypt cost-10 (PINs), SHA-256 (lookup index) |
| Container | Docker + Docker Compose |

---

## Prerequisites

Make sure the following are installed before you begin:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (v24+)
- [Docker Compose](https://docs.docker.com/compose/) (included with Docker Desktop)

You do **not** need Java, Node.js, or PostgreSQL installed locally — Docker handles everything.

---

## Running with Docker Compose

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd appointment-booking
```

### 2. Create your environment file

Copy the example file and fill in your values:

```bash
cp .env.example .env
```

Open `.env` and set the following:

```env
# Database password
DB_PASSWORD=your_secure_password

# JWT secret — must be a Base64-encoded string of at least 32 bytes
JWT_SECRET=Y2hhbmdlbWVpbnByb2R1Y3Rpb25rZXkxMjM0NTY3ODk=

# AES-256 encryption key — must be exactly 32 characters
ENCRYPTION_KEY=32charEncryptionKeyForAES256!!!X

# SMTP — leave blank to use Ethereal (email simulation, no real emails sent)
SMTP_HOST=smtp.ethereal.email
SMTP_PORT=587
SMTP_USER=
SMTP_PASS=
EMAIL_FROM=noreply@appointmentbooking.co.za
EMAIL_FROM_NAME=Appointment Booking System
```

> **Note:** The defaults in `.env.example` are safe for local development. Never commit a real `.env` file to version control.

### 3. Build and start all services

```bash
docker compose up --build
```

This will:
1. Pull the PostgreSQL 16 image
2. Build the Spring Boot backend (downloads Maven dependencies, compiles, packages)
3. Build the React frontend (installs npm packages, runs Vite build, serves via Nginx)
4. Run Flyway migrations automatically on backend startup
5. Seed default employee and customer accounts

> The first build takes 3–5 minutes due to Maven dependency downloads. Subsequent builds are faster thanks to Docker layer caching.

### 4. Open the application

| Service | URL |
|---|---|
| Frontend (React app) | http://localhost:3000 |
| Backend API | http://localhost:5000/api/v1 |
| API Health check | http://localhost:5000/actuator/health |
| Swagger UI | http://localhost:5000/swagger-ui.html |
| PostgreSQL | `localhost:5432` — db: `appointment_booking`, user: `postgres` |

---

## Default Credentials

### Employee (Admin Dashboard)

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@1234` | ADMIN |
| `branch.staff` | `Staff@1234` | EMPLOYEE |

### Customers (Booking Sign-in)

| SA ID Number | PIN |
|---|---|
| `9001015009087` | `12345` |
| `9301255611082` | `54321` |

> These are seeded automatically on first boot by `EmployeeSeeder` and `CustomerSeeder`.

---

## Useful Docker Compose Commands

```bash
# Start all services in the background
docker compose up -d

# View logs for all services
docker compose logs -f

# View logs for a specific service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres

# Stop all services (keeps data volumes)
docker compose down

# Stop all services and delete all data (fresh start)
docker compose down -v

# Rebuild a single service without restarting others
docker compose up --build backend

# Open a shell inside the backend container
docker compose exec backend sh

# Open a psql shell inside the database container
docker compose exec postgres psql -U postgres -d appointment_booking
```

---

## Project Structure

```
appointment-booking/
├── backend/                   # Java 17 Spring Boot API
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/appointmentbooking/
│   │   │   │   ├── controller/        # REST controllers
│   │   │   │   ├── service/           # Business logic (split by concern)
│   │   │   │   ├── domain/            # JPA entities and enums
│   │   │   │   ├── dto/               # Request/response DTOs
│   │   │   │   ├── repository/        # Spring Data JPA repositories
│   │   │   │   ├── security/          # JWT filter and config
│   │   │   │   ├── mapper/            # MapStruct mappers
│   │   │   │   └── util/              # Encryption, reference generator
│   │   │   └── resources/
│   │   │       ├── db/migration/      # Flyway SQL migrations (V1–V4)
│   │   │       └── templates/email/   # Thymeleaf email templates
│   │   └── test/                      # JUnit 5 + Mockito unit tests
│   └── Dockerfile
├── frontend/                  # React 18 SPA
│   ├── src/
│   │   ├── api/               # Axios API client + interceptors
│   │   ├── components/        # Reusable UI components + layout
│   │   ├── context/           # AuthContext (customer) + EmployeeAuthContext
│   │   ├── hooks/             # useBranches, useAvailableSlots
│   │   ├── pages/             # BookPage, AdminPage, EmployeeLoginPage, etc.
│   │   └── utils/             # Validation schemas, formatters
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

---

## Database Migrations

Flyway migrations run automatically when the backend starts. They are located in `backend/src/main/resources/db/migration/`:

| Migration | Description |
|---|---|
| `V1__initial_schema.sql` | Branches, appointments, audit log tables and seed branches |
| `V2__customers.sql` | Customers table for booking sign-in |
| `V3__employees.sql` | Employees table for admin dashboard access |
| `V4__add_expired_status.sql` | Adds `EXPIRED` to the appointments status check constraint |

---

## Environment Variables Reference

| Variable | Default | Description |
|---|---|---|
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `JWT_SECRET` | *(dev default)* | Base64-encoded HS256 signing secret (min 32 bytes) |
| `ENCRYPTION_KEY` | *(dev default)* | AES-256 key for ID number encryption (exactly 32 chars) |
| `JWT_EMPLOYEE_EXPIRY` | `28800` | Employee token validity in seconds (8 hours) |
| `JWT_CUSTOMER_EXPIRY` | `3600` | Customer token validity in seconds (1 hour) |
| `SMTP_HOST` | `smtp.ethereal.email` | SMTP server hostname |
| `SMTP_PORT` | `587` | SMTP server port |
| `SMTP_USER` | *(empty)* | SMTP username |
| `SMTP_PASS` | *(empty)* | SMTP password |
| `EMAIL_FROM` | `noreply@...` | Sender email address |
| `CORS_ORIGIN` | `http://localhost:3000` | Allowed CORS origin for the frontend |
