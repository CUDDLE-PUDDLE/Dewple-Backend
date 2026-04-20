-- Organization: LocalDateTime → OffsetDateTime (TIMESTAMP → TIMESTAMPTZ)
-- 이미 TIMESTAMPTZ인 경우 안전하게 스킵
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'organization' AND column_name = 'dissolution_requested_at'
        AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE organization ALTER COLUMN dissolution_requested_at TYPE TIMESTAMPTZ USING dissolution_requested_at AT TIME ZONE 'UTC';
        ALTER TABLE organization ALTER COLUMN dissolution_approved_at TYPE TIMESTAMPTZ USING dissolution_approved_at AT TIME ZONE 'UTC';
        ALTER TABLE organization ALTER COLUMN scheduled_delete_at TYPE TIMESTAMPTZ USING scheduled_delete_at AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Club: LocalDateTime → OffsetDateTime (TIMESTAMP → TIMESTAMPTZ)
DO $$ BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'club' AND column_name = 'deletion_requested_at'
        AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE club ALTER COLUMN deletion_requested_at TYPE TIMESTAMPTZ USING deletion_requested_at AT TIME ZONE 'UTC';
        ALTER TABLE club ALTER COLUMN deletion_approved_at TYPE TIMESTAMPTZ USING deletion_approved_at AT TIME ZONE 'UTC';
        ALTER TABLE club ALTER COLUMN scheduled_delete_at TYPE TIMESTAMPTZ USING scheduled_delete_at AT TIME ZONE 'UTC';
    END IF;
END $$;
