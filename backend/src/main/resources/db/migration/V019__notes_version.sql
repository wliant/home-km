-- Optimistic-concurrency token for the multi-device editor; see
-- gaps/sync/conflict-resolution.md. Hibernate's @Version increments this
-- on every UPDATE; controllers reject mismatched expectedVersion with
-- 409 CONFLICT_STALE.
ALTER TABLE notes
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
