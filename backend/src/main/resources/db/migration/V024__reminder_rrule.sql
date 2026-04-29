-- Replace the enum recurrence values with iCalendar RRULE strings so users
-- can build patterns like "every weekday" or "first of every month" without
-- adding more enum slots. Legacy values get rewritten in place; the column
-- is widened to fit a representative RRULE.
ALTER TABLE reminders
    DROP CONSTRAINT IF EXISTS reminders_recurrence_check;

ALTER TABLE reminders
    ALTER COLUMN recurrence TYPE VARCHAR(255);

UPDATE reminders SET recurrence = 'FREQ=DAILY'   WHERE recurrence = 'daily';
UPDATE reminders SET recurrence = 'FREQ=WEEKLY'  WHERE recurrence = 'weekly';
UPDATE reminders SET recurrence = 'FREQ=MONTHLY' WHERE recurrence = 'monthly';
UPDATE reminders SET recurrence = 'FREQ=YEARLY'  WHERE recurrence = 'yearly';

-- Soft check: must look like an RRULE (or stay null). Catches typos but
-- doesn't validate semantics — that happens at parse time in
-- ReminderScheduler.advanceRemindAt.
ALTER TABLE reminders
    ADD CONSTRAINT reminders_recurrence_check
        CHECK (recurrence IS NULL OR recurrence ~ '^FREQ=');
