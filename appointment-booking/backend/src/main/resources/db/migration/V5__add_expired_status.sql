-- ============================================================
-- V5: Add EXPIRED to the appointments status check constraint.
-- The EXPIRED status is set automatically by the nightly expiry
-- scheduler when an appointment date passes without a status update.
-- ============================================================

ALTER TABLE appointments
    DROP CONSTRAINT IF EXISTS appointments_status_check;

ALTER TABLE appointments
    ADD CONSTRAINT appointments_status_check
        CHECK (status IN ('PENDING','CONFIRMED','CANCELLED','COMPLETED','NO_SHOW','EXPIRED'));
