-- V6: 연합회 생성/해산/역할 기능

ALTER TABLE organization ADD COLUMN IF NOT EXISTS purpose VARCHAR(1000);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';
ALTER TABLE organization ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS contact_email VARCHAR(100);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS contact_preference VARCHAR(10);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS target_clubs_description VARCHAR(500);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS target_club_ids JSONB;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS category_ids JSONB;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS region_ids JSONB;

ALTER TABLE organization ADD COLUMN IF NOT EXISTS dissolution_status VARCHAR(20) NOT NULL DEFAULT 'NONE';
ALTER TABLE organization ADD COLUMN IF NOT EXISTS dissolution_reason VARCHAR(500);
ALTER TABLE organization ADD COLUMN IF NOT EXISTS dissolution_requested_at TIMESTAMP;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS dissolution_approved_at TIMESTAMP;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS scheduled_delete_at TIMESTAMP;
ALTER TABLE organization ADD COLUMN IF NOT EXISTS is_sanction_deletion BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS organization_role (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id),
    name VARCHAR(50) NOT NULL,
    permissions BIGINT NOT NULL DEFAULT 0,
    is_staff BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS organization_member (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organization(id),
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES organization_role(id),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
