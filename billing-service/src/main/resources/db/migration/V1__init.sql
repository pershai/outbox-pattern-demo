CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    amount NUMERIC NOT NULL,
    status TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID NOT NULL,
    handler TEXT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, handler)
);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT,
    status VARCHAR(50) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    sent_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_occurred_at ON outbox_events(status, occurred_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_status_sent_at ON outbox_events(status, sent_at);
CREATE INDEX IF NOT EXISTS idx_processed_events_processed_at ON processed_events(processed_at);
