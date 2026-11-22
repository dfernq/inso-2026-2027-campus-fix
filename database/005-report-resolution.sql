DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'issues'::regclass
          AND conname = 'issues_status_open_or_cancelled_check'
    ) THEN
        ALTER TABLE issues ADD COLUMN resolution_note text;
        ALTER TABLE issues
            DROP CONSTRAINT issues_status_open_or_cancelled_check,
            ADD CONSTRAINT issues_status_workflow_check
                CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CANCELLED')),
            ADD CONSTRAINT issues_resolution_note_check CHECK (
                (status = 'RESOLVED' AND resolution_note IS NOT NULL
                    AND char_length(btrim(resolution_note)) BETWEEN 1 AND 2000)
                OR (status <> 'RESOLVED' AND resolution_note IS NULL)
            );
    END IF;
END
$$;
