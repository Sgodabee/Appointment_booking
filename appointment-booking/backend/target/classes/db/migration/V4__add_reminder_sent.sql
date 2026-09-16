-- ============================================================
-- V4: Add reminder_sent flag to appointments
-- Tracks whether the ~1-hour "appointment reminder" email has
-- already been dispatched, so the scheduler never sends duplicates.
-- ============================================================

ALTER TABLE appointments
    ADD COLUMN IF NOT EXISTS reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;

-- Partial index: the reminder scheduler only ever scans rows not yet reminded.
CREATE INDEX IF NOT EXISTS idx_appt_reminder_pending
    ON appointments (appointment_date, appointment_time)
    WHERE reminder_sent = FALSE;
