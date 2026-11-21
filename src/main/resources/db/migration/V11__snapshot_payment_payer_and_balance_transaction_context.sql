ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS payer_id BIGINT REFERENCES customers(id),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64),
    ADD COLUMN IF NOT EXISTS performed_by_user_id BIGINT REFERENCES users(id);

UPDATE payments payment
SET payer_id = account.customer_id
FROM balance_accounts account
WHERE payment.balance_account_id = account.id
  AND payment.payer_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_idempotency_key
    ON payments (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE balance_transactions
    ADD COLUMN IF NOT EXISTS payment_id BIGINT REFERENCES payments(id),
    ADD COLUMN IF NOT EXISTS performed_by_user_id BIGINT REFERENCES users(id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_balance_transactions_payment_id
    ON balance_transactions (payment_id)
    WHERE payment_id IS NOT NULL;
