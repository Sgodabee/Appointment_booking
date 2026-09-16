-- ============================================================
-- V1: Initial Schema — Appointment Booking System
-- Managed by Flyway. Never edited manually after applying.
-- ============================================================

-- ─── Branches ─────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS branches (
    id              BIGSERIAL    PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    province        VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(150),
    operating_hours TEXT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ─── Appointments ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS appointments (
    id               BIGSERIAL    PRIMARY KEY,
    reference_number VARCHAR(20)  NOT NULL,
    customer_name    VARCHAR(200) NOT NULL,
    customer_email   VARCHAR(255) NOT NULL,
    customer_phone   VARCHAR(20)  NOT NULL,
    id_number        TEXT,                    -- AES-256-CBC encrypted; never plaintext
    branch_id        BIGINT       NOT NULL REFERENCES branches(id) ON DELETE RESTRICT,
    service_type     VARCHAR(50)  NOT NULL,
    appointment_date DATE         NOT NULL,
    appointment_time TIME         NOT NULL,
    notes            VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED'
                         CHECK (status IN ('PENDING','CONFIRMED','CANCELLED','COMPLETED','NO_SHOW')),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_appointments_reference UNIQUE (reference_number)
);

-- ─── Audit Log ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS appointment_audit_log (
    id             BIGSERIAL   PRIMARY KEY,
    appointment_id BIGINT      NOT NULL REFERENCES appointments(id) ON DELETE CASCADE,
    action         VARCHAR(50) NOT NULL,
    old_status     VARCHAR(20),
    new_status     VARCHAR(20),
    changed_by     VARCHAR(100) DEFAULT 'system',
    changed_at     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─── Indexes ──────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_appt_branch_date  ON appointments(branch_id, appointment_date);
CREATE INDEX IF NOT EXISTS idx_appt_reference    ON appointments(reference_number);
CREATE INDEX IF NOT EXISTS idx_appt_email        ON appointments(customer_email);
CREATE INDEX IF NOT EXISTS idx_appt_status       ON appointments(status);
CREATE INDEX IF NOT EXISTS idx_audit_appointment ON appointment_audit_log(appointment_id);

-- ─── Auto-update updated_at ───────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER branches_updated_at
    BEFORE UPDATE ON branches
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER appointments_updated_at
    BEFORE UPDATE ON appointments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ─── Seed: 5 sample branches ──────────────────────────────────────────────────
INSERT INTO branches (name, address, city, province, phone, email, operating_hours) VALUES
(
    'Cape Town City Centre',
    '10 Adderley Street', 'Cape Town', 'Western Cape',
    '+27 21 000 0001', 'capetown@appointments.co.za',
    '{"monday":"08:00-17:00","tuesday":"08:00-17:00","wednesday":"08:00-17:00","thursday":"08:00-17:00","friday":"08:00-17:00","saturday":"09:00-13:00","sunday":"closed"}'
),
(
    'Sandton',
    '5 Sandton Drive', 'Johannesburg', 'Gauteng',
    '+27 11 000 0002', 'sandton@appointments.co.za',
    '{"monday":"08:00-17:00","tuesday":"08:00-17:00","wednesday":"08:00-17:00","thursday":"08:00-17:00","friday":"08:00-17:00","saturday":"09:00-13:00","sunday":"closed"}'
),
(
    'Durban Central',
    '22 Smith Street', 'Durban', 'KwaZulu-Natal',
    '+27 31 000 0003', 'durban@appointments.co.za',
    '{"monday":"08:00-17:00","tuesday":"08:00-17:00","wednesday":"08:00-17:00","thursday":"08:00-17:00","friday":"08:00-17:00","saturday":"09:00-13:00","sunday":"closed"}'
),
(
    'Pretoria Central',
    '100 Church Street', 'Pretoria', 'Gauteng',
    '+27 12 000 0004', 'pretoria@appointments.co.za',
    '{"monday":"08:00-17:00","tuesday":"08:00-17:00","wednesday":"08:00-17:00","thursday":"08:00-17:00","friday":"08:00-17:00","saturday":"09:00-13:00","sunday":"closed"}'
),
(
    'Port Elizabeth',
    '45 Main Street', 'Gqeberha', 'Eastern Cape',
    '+27 41 000 0005', 'pe@appointments.co.za',
    '{"monday":"08:00-17:00","tuesday":"08:00-17:00","wednesday":"08:00-17:00","thursday":"08:00-17:00","friday":"08:00-17:00","saturday":"09:00-13:00","sunday":"closed"}'
)
ON CONFLICT DO NOTHING;
