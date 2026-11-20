CREATE TABLE account (
    account_id text PRIMARY KEY,
    owner_id text,
    kind text NOT NULL CHECK (kind IN ('CLIENT', 'INTERNAL')),
    status text NOT NULL CHECK (status IN ('ACTIVE', 'INACTIVE')),
    currency char(3) NOT NULL CHECK (currency = 'MAD'),
    booked_minor bigint NOT NULL DEFAULT 0,
    CHECK ((kind = 'CLIENT' AND owner_id IS NOT NULL AND booked_minor >= 0)
        OR (kind = 'INTERNAL' AND owner_id IS NULL))
);

CREATE TABLE fund_hold (
    order_id uuid PRIMARY KEY,
    account_id text NOT NULL REFERENCES account (account_id),
    amount_minor bigint NOT NULL CHECK (amount_minor > 0),
    currency char(3) NOT NULL CHECK (currency = 'MAD'),
    status text NOT NULL CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED'))
);

CREATE INDEX fund_hold_active_account_idx ON fund_hold (account_id) WHERE status = 'ACTIVE';

CREATE TABLE account_action (
    order_id uuid NOT NULL,
    action text NOT NULL CHECK (action IN ('RESERVATION', 'POSTING', 'RELEASE')),
    intent jsonb NOT NULL CHECK (jsonb_typeof(intent) = 'object'),
    result jsonb NOT NULL CHECK (jsonb_typeof(result) = 'object'),
    recorded_at timestamptz NOT NULL,
    PRIMARY KEY (order_id, action)
);

CREATE TABLE journal_entry (
    entry_id uuid PRIMARY KEY,
    kind text NOT NULL CHECK (kind IN ('OPENING', 'PAYMENT', 'CORRECTION')),
    order_id uuid,
    corrects_entry_id uuid REFERENCES journal_entry (entry_id),
    recorded_at timestamptz NOT NULL,
    CHECK ((kind = 'CORRECTION') = (corrects_entry_id IS NOT NULL)),
    CHECK (corrects_entry_id IS DISTINCT FROM entry_id),
    CHECK (kind <> 'PAYMENT' OR order_id IS NOT NULL)
);

CREATE UNIQUE INDEX journal_entry_payment_order_idx ON journal_entry (order_id)
    WHERE kind = 'PAYMENT';

CREATE TABLE journal_line (
    entry_id uuid NOT NULL REFERENCES journal_entry (entry_id),
    line_number integer NOT NULL CHECK (line_number > 0),
    account_id text NOT NULL REFERENCES account (account_id),
    amount_minor bigint NOT NULL CHECK (amount_minor <> 0),
    currency char(3) NOT NULL CHECK (currency = 'MAD'),
    PRIMARY KEY (entry_id, line_number)
);

CREATE FUNCTION forbid_journal_mutation() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Journal records are immutable' USING ERRCODE = '23514';
END;
$$;

CREATE TRIGGER journal_entry_immutable BEFORE UPDATE OR DELETE ON journal_entry
    FOR EACH ROW EXECUTE FUNCTION forbid_journal_mutation();
CREATE TRIGGER journal_line_immutable BEFORE UPDATE OR DELETE ON journal_line
    FOR EACH ROW EXECUTE FUNCTION forbid_journal_mutation();

CREATE FUNCTION check_journal_balance() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM journal_line WHERE entry_id = NEW.entry_id
        HAVING count(*) >= 2 AND sum(amount_minor) = 0
    ) THEN
        RAISE EXCEPTION 'Journal entry % must have balanced lines', NEW.entry_id
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER journal_entry_balanced AFTER INSERT ON journal_entry
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_journal_balance();
CREATE CONSTRAINT TRIGGER journal_line_balanced AFTER INSERT ON journal_line
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION check_journal_balance();
