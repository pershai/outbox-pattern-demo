-- Rename table to match OutboxEventEntity
ALTER TABLE IF EXISTS outbox RENAME TO outbox_events;

-- Add missing columns
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS last_retry_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000);

-- Rename indices if they exist (optional but good for consistency)
ALTER INDEX IF EXISTS idx_outbox_status_occurred_at RENAME TO idx_outbox_events_status_occurred_at;
ALTER INDEX IF EXISTS idx_outbox_status_sent_at RENAME TO idx_outbox_events_status_sent_at;
