ALTER TABLE users
    ADD COLUMN notification_prefs JSONB NOT NULL DEFAULT '{}'::jsonb;
