CREATE TABLE IF NOT EXISTS products (
    sku TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    available_quantity INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_sku TEXT NOT NULL REFERENCES products (sku),
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

CREATE TABLE IF NOT EXISTS processed_events (
    event_id   UUID         NOT NULL,
    handler    VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (event_id, handler)
);

CREATE INDEX IF NOT EXISTS idx_outbox_status_occurred_at ON outbox(status, occurred_at);
CREATE INDEX IF NOT EXISTS idx_outbox_status_sent_at ON outbox(status, sent_at);
CREATE INDEX IF NOT EXISTS idx_processed_events_processed_at ON processed_events(processed_at);

INSERT INTO products (sku, name, available_quantity)
    VALUES ('PIZZA-MARG', 'Margherita Pizza', 50)
ON CONFLICT (sku) DO NOTHING;
