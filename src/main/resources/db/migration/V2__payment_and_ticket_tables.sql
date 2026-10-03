CREATE TABLE IF NOT EXISTS tickets (
    ticket_id UUID PRIMARY KEY,
    ticket_number VARCHAR(255) NOT NULL,
    booking_id UUID NOT NULL,
    event_id UUID NOT NULL,
    slot_id UUID NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    qr_payload VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT uk_tickets_number UNIQUE (ticket_number)
);

CREATE INDEX IF NOT EXISTS idx_tickets_booking_tenant ON tickets (booking_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_tickets_customer_tenant ON tickets (customer_id, tenant_id);
