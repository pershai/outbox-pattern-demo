-- Add missing columns to outbox_events table to match OutboxEventEntity

-- Add retry tracking columns
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS retry_count INTEGER DEFAULT 0;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS last_retry_at TIMESTAMPTZ;
ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000);

-- Fix payload column to use JSONB for better performance
ALTER TABLE outbox_events ALTER COLUMN payload TYPE JSONB USING payload::JSONB;

-- Rename id to event_id if needed (the entity uses event_id as the primary key)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'outbox_events' AND column_name = 'id'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'outbox_events' AND column_name = 'event_id' AND is_identity = 'NO'
    ) THEN
        -- Drop the old id column and use event_id as primary key
        ALTER TABLE outbox_events DROP CONSTRAINT IF EXISTS outbox_events_pkey;
        ALTER TABLE outbox_events DROP COLUMN IF EXISTS id;
        ALTER TABLE outbox_events ADD PRIMARY KEY (event_id);
    END IF;
END $$;
