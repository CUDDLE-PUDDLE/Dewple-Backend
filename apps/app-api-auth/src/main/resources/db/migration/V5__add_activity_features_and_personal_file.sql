-- V5: 개인 자료실 테이블 추가
-- 2026-03-23

CREATE TABLE IF NOT EXISTS personal_file (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    original_name   VARCHAR(500) NOT NULL,
    stored_key      VARCHAR(1000) NOT NULL,
    file_size       BIGINT NOT NULL,
    content_type    VARCHAR(100),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_personal_file_user_id ON personal_file(user_id);

-- 모임관리자 초대 코드
ALTER TABLE activity ADD COLUMN IF NOT EXISTS manager_invite_code VARCHAR(36) UNIQUE;

-- 모임 생애주기 상태 (RECRUITING/IN_PROGRESS/ENDED/CANCELLED/DELETED)
ALTER TABLE activity ADD COLUMN IF NOT EXISTS lifecycle_status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING';

-- 모임 참여자 공지
CREATE TABLE IF NOT EXISTS activity_notice (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES activity(id),
    author_id       BIGINT NOT NULL REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    content         TEXT NOT NULL,
    image_urls      JSONB,
    comment_count   INTEGER NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_activity_notice_activity_id ON activity_notice(activity_id);

-- 모임 공지 댓글
CREATE TABLE IF NOT EXISTS activity_notice_comment (
    id              BIGSERIAL PRIMARY KEY,
    notice_id       BIGINT NOT NULL REFERENCES activity_notice(id),
    author_id       BIGINT NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_activity_notice_comment_notice_id ON activity_notice_comment(notice_id);

-- 모임 문의
CREATE TABLE IF NOT EXISTS activity_inquiry (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES activity(id),
    author_id       BIGINT NOT NULL REFERENCES users(id),
    content         TEXT NOT NULL,
    is_anonymous    BOOLEAN NOT NULL DEFAULT false,
    answer          TEXT,
    answered_by_id  BIGINT REFERENCES users(id),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_activity_inquiry_activity_id ON activity_inquiry(activity_id);

-- 모임 신고
CREATE TABLE IF NOT EXISTS activity_report (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES activity(id),
    reporter_id     BIGINT NOT NULL REFERENCES users(id),
    category        VARCHAR(30) NOT NULL,
    reason          TEXT NOT NULL,
    report_status   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    snapshot        JSONB NOT NULL,
    resolved_at     TIMESTAMPTZ,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_activity_report_activity_id ON activity_report(activity_id);

-- 별점 평가
CREATE TABLE IF NOT EXISTS star_rating (
    id              BIGSERIAL PRIMARY KEY,
    activity_id     BIGINT NOT NULL REFERENCES activity(id),
    rater_id        BIGINT NOT NULL REFERENCES users(id),
    ratee_id        BIGINT NOT NULL REFERENCES users(id),
    score           DECIMAL(2,1) NOT NULL,
    is_no_show      BOOLEAN NOT NULL DEFAULT false,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (activity_id, rater_id, ratee_id)
);

CREATE INDEX IF NOT EXISTS idx_star_rating_ratee_id ON star_rating(ratee_id);
