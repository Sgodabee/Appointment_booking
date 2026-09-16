-- ============================================================
-- V2: Customers table for appointment booking sign-in
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    id                   BIGSERIAL    PRIMARY KEY,
    full_name            VARCHAR(200) NOT NULL,
    email                VARCHAR(255) NOT NULL,
    phone                VARCHAR(20)  NOT NULL,

    -- SHA-256 hex digest of the plain ID number.
    -- Used as a fast, deterministic lookup index.
    id_number_hash       VARCHAR(64)  NOT NULL,

    -- AES-256-CBC encrypted ID number (same scheme as appointments.id_number).
    -- Format: "ivHex:base64Ciphertext"
    id_number_encrypted  TEXT         NOT NULL,

    -- bcrypt hash of the customer's 5-digit remote PIN.
    pin_hash             VARCHAR(255) NOT NULL,

    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_customers_id_hash UNIQUE (id_number_hash),
    CONSTRAINT uq_customers_email   UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_customers_id_hash ON customers(id_number_hash);

CREATE TRIGGER customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Seed data is inserted by CustomerSeeder on first boot
-- so that id_number_encrypted and pin_hash are generated
-- correctly using the live encryption key and PasswordEncoder.
