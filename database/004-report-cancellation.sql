DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'issues'::regclass AND conname = 'issues_status_check'
    ) THEN
        ALTER TABLE issues
            DROP CONSTRAINT issues_status_check,
            ADD CONSTRAINT issues_status_open_or_cancelled_check
                CHECK (status IN ('OPEN', 'CANCELLED'));
    END IF;
END
$$;
