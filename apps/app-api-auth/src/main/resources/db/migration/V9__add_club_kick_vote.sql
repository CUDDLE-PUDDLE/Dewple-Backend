-- V9: 동아리 회원 내보내기 투표

CREATE TABLE IF NOT EXISTS club_kick_vote (
    id BIGSERIAL PRIMARY KEY,
    club_id BIGINT NOT NULL REFERENCES club(id),
    target_member_id BIGINT NOT NULL REFERENCES club_member(id),
    voter_id BIGINT NOT NULL REFERENCES users(id),
    is_approved BOOLEAN,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
