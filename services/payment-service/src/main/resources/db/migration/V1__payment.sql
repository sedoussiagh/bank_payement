CREATE TABLE payment_order (
    order_id uuid PRIMARY KEY,
    client_id text NOT NULL,
    idempotency_key varchar(128) COLLATE "C" NOT NULL CHECK (idempotency_key ~ '^[!-~]{1,128}$'),
    biller_id text NOT NULL,
    invoice_ref text NOT NULL,
    invoice_version bigint NOT NULL CHECK (invoice_version >= 0),
    account_id text NOT NULL,
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL CHECK (currency = 'MAD'),
    status text NOT NULL CHECK (status IN ('RESERVING', 'AWAITING_BILLER', 'RECONCILING',
                                           'POSTING', 'RELEASING', 'SUCCEEDED', 'FAILED')),
    reason_code text,
    biller_decision_ref text,
    posting_id text,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    UNIQUE (client_id, idempotency_key)
);

CREATE TABLE payment_saga (
    order_id uuid PRIMARY KEY REFERENCES payment_order (order_id),
    next_action text,
    next_attempt_at timestamptz,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    lease_until timestamptz,
    reconciling_since timestamptz,
    alert_due_at timestamptz,
    alerted_at timestamptz,
    CHECK ((next_action IS NULL) = (next_attempt_at IS NULL))
);

CREATE INDEX payment_saga_attempt_due_idx ON payment_saga (next_attempt_at)
    WHERE next_action IS NOT NULL;
CREATE INDEX payment_saga_alert_due_idx ON payment_saga (alert_due_at)
    WHERE alerted_at IS NULL AND alert_due_at IS NOT NULL;

CREATE TABLE payment_receipt (
    receipt_id uuid PRIMARY KEY,
    order_id uuid NOT NULL UNIQUE REFERENCES payment_order (order_id),
    biller_id text NOT NULL,
    invoice_ref text NOT NULL,
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL CHECK (currency = 'MAD'),
    succeeded_at timestamptz NOT NULL
);
