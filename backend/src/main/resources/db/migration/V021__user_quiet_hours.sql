ALTER TABLE users
    ADD COLUMN quiet_hours_start TIME NULL,
    ADD COLUMN quiet_hours_end   TIME NULL;
-- timezone column already added in V014; reused here to interpret the
-- quiet-hours window in the user's local time.
