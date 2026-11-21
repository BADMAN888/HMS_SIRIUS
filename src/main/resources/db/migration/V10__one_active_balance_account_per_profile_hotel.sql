DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM balance_accounts
        WHERE status = 'ACTIVE'
        GROUP BY customer_id, hotel_id
        HAVING COUNT(*) > 1
    ) THEN
        CREATE UNIQUE INDEX IF NOT EXISTS uq_balance_account_active_profile_hotel
            ON balance_accounts (customer_id, hotel_id)
            WHERE status = 'ACTIVE';
    ELSE
        RAISE NOTICE 'Skipping active balance account unique index because existing duplicates need manual review';
    END IF;
END
$$;
