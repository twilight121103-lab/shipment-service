-- ============================================================================
-- Shipment Service - initial schema
-- PostgreSQL
--
-- Tables:
--   shipments           - the aggregate root
--   outbox_events       - reliable event publication (transactional outbox)
--   idempotency_keys    - idempotent creation
--   tracking_sequence   - backing sequence for readable tracking numbers
-- ============================================================================

CREATE TABLE shipments
(
    id                      UUID PRIMARY KEY,
    tracking_number         VARCHAR(32) UNIQUE,
    status                  VARCHAR(32)    NOT NULL,

    sender_name             VARCHAR(200)   NOT NULL,
    sender_phone            VARCHAR(32),
    sender_email            VARCHAR(200),

    recipient_name          VARCHAR(200)   NOT NULL,
    recipient_phone         VARCHAR(32),
    recipient_email         VARCHAR(200),

    pickup_street           VARCHAR(200)   NOT NULL,
    pickup_city             VARCHAR(100)   NOT NULL,
    pickup_postal_code      VARCHAR(10)    NOT NULL,
    pickup_country          VARCHAR(3)     NOT NULL,

    delivery_street         VARCHAR(200)   NOT NULL,
    delivery_city           VARCHAR(100)   NOT NULL,
    delivery_postal_code    VARCHAR(10)    NOT NULL,
    delivery_country        VARCHAR(3)     NOT NULL,

    length_cm               NUMERIC(10, 2) NOT NULL,
    width_cm                NUMERIC(10, 2) NOT NULL,
    height_cm               NUMERIC(10, 2) NOT NULL,
    weight_kg               NUMERIC(10, 2) NOT NULL,

    delivery_type           VARCHAR(32)    NOT NULL,
    estimated_delivery_date DATE           NOT NULL,

    created_at              TIMESTAMPTZ    NOT NULL,
    updated_at              TIMESTAMPTZ    NOT NULL,
    version                 BIGINT         NOT NULL DEFAULT 0,

    CONSTRAINT ck_shipment_status CHECK (status IN (
        'CREATED', 'CONFIRMED', 'PICKUP_ASSIGNED', 'PICKED_UP', 'IN_TRANSIT',
        'OUT_FOR_DELIVERY', 'DELIVERED', 'DELIVERY_FAILED', 'RETURNED', 'CANCELLED'
    )),
    CONSTRAINT ck_delivery_type CHECK (delivery_type IN ('STANDARD', 'EXPRESS', 'SAME_DAY')),
    CONSTRAINT ck_positive_weight CHECK (weight_kg > 0),
    CONSTRAINT ck_positive_length CHECK (length_cm > 0),
    CONSTRAINT ck_positive_width CHECK (width_cm > 0),
    CONSTRAINT ck_positive_height CHECK (height_cm > 0)
);

-- Index for efficient lookup and pagination by status/date.
CREATE INDEX idx_shipments_status_created ON shipments (status, created_at DESC);

-- Tracking number lookup is covered by the UNIQUE constraint above.

-- ----------------------------------------------------------------------------
-- Outbox events
-- ----------------------------------------------------------------------------
CREATE TABLE outbox_events
(
    id              BIGSERIAL PRIMARY KEY,
    event_id        UUID         NOT NULL,
    aggregate_type  VARCHAR(64)  NOT NULL,
    aggregate_id    VARCHAR(64)  NOT NULL,
    event_type      VARCHAR(64)  NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count     INT          NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ  NULL,
    published_at    TIMESTAMPTZ  NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT uq_outbox_event_id UNIQUE (event_id)
);

-- Claiming worker selects from PENDING/FAILED whose retry time has passed.
CREATE INDEX idx_outbox_due ON outbox_events (status, next_attempt_at)
    WHERE status IN ('PENDING', 'FAILED');

-- ----------------------------------------------------------------------------
-- Idempotency keys
-- ----------------------------------------------------------------------------
CREATE TABLE idempotency_keys
(
    id             BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    resource_type  VARCHAR(64)  NOT NULL,
    resource_id    VARCHAR(64),
    response_body  TEXT,
    state          VARCHAR(16)  NOT NULL DEFAULT 'IN_FLIGHT',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    expires_at     TIMESTAMPTZ  NOT NULL,

    CONSTRAINT uq_idempotency_key UNIQUE (idempotency_key),
    CONSTRAINT ck_idempotency_state CHECK (state IN ('IN_FLIGHT', 'COMPLETED'))
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys (expires_at);

-- ----------------------------------------------------------------------------
-- Tracking numbers: a readable per-year sequence, e.g. SLV-2026-000001
-- ----------------------------------------------------------------------------
CREATE SEQUENCE tracking_number_seq START WITH 1 INCREMENT BY 1 NO CYCLE;
