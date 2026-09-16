-- ============================================================
-- V3: Employees table for admin dashboard access control
-- ============================================================

CREATE TABLE IF NOT EXISTS employees (
    id            BIGSERIAL    PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(255) NOT NULL,   -- bcrypt
    full_name     VARCHAR(200) NOT NULL,
    role          VARCHAR(30)  NOT NULL DEFAULT 'EMPLOYEE',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_employees_username UNIQUE (username)
);

CREATE INDEX IF NOT EXISTS idx_employees_username ON employees(username);

CREATE TRIGGER employees_updated_at
    BEFORE UPDATE ON employees
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed data is inserted by EmployeeSeeder on first boot
-- so password_hash is generated correctly using the live PasswordEncoder.
