-- V7: TIMESTAMP → TIMESTAMPTZ 타입 수정 (Hibernate TIMESTAMP_UTC 매핑)

ALTER TABLE organization ALTER COLUMN dissolution_requested_at TYPE TIMESTAMPTZ;
ALTER TABLE organization ALTER COLUMN dissolution_approved_at TYPE TIMESTAMPTZ;
ALTER TABLE organization ALTER COLUMN scheduled_delete_at TYPE TIMESTAMPTZ;

ALTER TABLE organization_role ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE organization_role ALTER COLUMN updated_at TYPE TIMESTAMPTZ;

ALTER TABLE organization_member ALTER COLUMN created_at TYPE TIMESTAMPTZ;
ALTER TABLE organization_member ALTER COLUMN updated_at TYPE TIMESTAMPTZ;
