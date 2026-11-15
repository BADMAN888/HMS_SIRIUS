DO $$
BEGIN
    IF to_regclass('public.balance_accounts') IS NOT NULL THEN
        ALTER TABLE balance_accounts
            DROP CONSTRAINT IF EXISTS uk_balance_account_customer_hotel;
    END IF;
END
$$;
