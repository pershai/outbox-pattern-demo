CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY,
    customer_email TEXT NOT NULL,
    amount NUMERIC NOT NULL,
    product_sku TEXT NOT NULL,
    quantity INTEGER NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS outbox (
    event_id UUID PRIMARY KEY,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('NEW', 'SENT', 'ERROR')),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_occurred_at ON outbox (status, occurred_at);
CREATE INDEX IF NOT EXISTS idx_outbox_status_sent_at ON outbox(status, sent_at);
