-- V4: 기능명세서 기반 스키마 변경
-- 2026-03-20

-- ============================================================
-- 1. users 테이블 변경
-- ============================================================

-- 본인인증 완료 여부
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT false;

-- 마지막 로그인 시점 (장기 미접속 판정)
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

-- 아이디 변경 시점 (7일 쿨다운 추적)
ALTER TABLE users ADD COLUMN IF NOT EXISTS user_id_changed_at TIMESTAMPTZ;

-- 소프트삭제 시점 (1주일 후 하드삭제 판정)
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- 이메일 인증 여부
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_email_verified BOOLEAN NOT NULL DEFAULT false;

-- 별점 기본값 변경: 5.0 → NULL (별점 미수신 시 null)
ALTER TABLE users ALTER COLUMN reputation_score DROP DEFAULT;
ALTER TABLE users ALTER COLUMN reputation_score SET DEFAULT NULL;
UPDATE users SET reputation_score = NULL WHERE reputation_score = 5.0;

-- ============================================================
-- 2. club 테이블 변경
-- ============================================================

-- 숨김동아리 (검색 미노출)
ALTER TABLE club ADD COLUMN IF NOT EXISTS is_hidden BOOLEAN NOT NULL DEFAULT false;

-- 본인인증 필수 동아리
ALTER TABLE club ADD COLUMN IF NOT EXISTS is_verification_required BOOLEAN NOT NULL DEFAULT false;

-- 설립일 (캘린더 기념일 표시)
ALTER TABLE club ADD COLUMN IF NOT EXISTS founded_date DATE;

-- 관심동아리 하트 수
ALTER TABLE club ADD COLUMN IF NOT EXISTS like_count INTEGER NOT NULL DEFAULT 0;

-- 교내동아리 개념 삭제 (연합회로 이동) - university 컬럼 제거
ALTER TABLE club DROP COLUMN IF EXISTS university;

-- ============================================================
-- 3. club_role 테이블 변경
-- ============================================================

-- 운영진 여부 플래그
ALTER TABLE club_role ADD COLUMN IF NOT EXISTS is_staff BOOLEAN NOT NULL DEFAULT false;

-- 기본 역할 여부 (삭제 불가)
ALTER TABLE club_role ADD COLUMN IF NOT EXISTS is_default BOOLEAN NOT NULL DEFAULT false;

-- ============================================================
-- 4. club_member 테이블 변경
-- ============================================================

-- 활동 종료일 (수료 판정 기준)
ALTER TABLE club_member ADD COLUMN IF NOT EXISTS activity_end_date DATE;

-- ============================================================
-- 5. organization 테이블 변경
-- ============================================================

-- 연합회 종류
ALTER TABLE organization ADD COLUMN IF NOT EXISTS type VARCHAR(30) NOT NULL DEFAULT 'OTHER';

-- 활동방식
ALTER TABLE organization ADD COLUMN IF NOT EXISTS activity_type VARCHAR(30) NOT NULL DEFAULT 'BOTH';

-- 설립일
ALTER TABLE organization ADD COLUMN IF NOT EXISTS founded_date DATE;

-- ============================================================
-- 6. activity 테이블 변경
-- ============================================================

-- 연합회 모임
ALTER TABLE activity ADD COLUMN IF NOT EXISTS organization_id BIGINT;
DO $$ BEGIN
    ALTER TABLE activity ADD CONSTRAINT fk_activity_organization
        FOREIGN KEY (organization_id) REFERENCES organization(id);
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;

-- 출석체크 방식 (QR/GPS/MANUAL/CODE)
ALTER TABLE activity ADD COLUMN IF NOT EXISTS attendance_check_method VARCHAR(20);

-- 비상연락처
ALTER TABLE activity ADD COLUMN IF NOT EXISTS emergency_contact VARCHAR(100);

-- 참여 취소 기한 (모임 시작 n일 전)
ALTER TABLE activity ADD COLUMN IF NOT EXISTS cancel_deadline_days INTEGER NOT NULL DEFAULT 1;

-- 지원 마감일 변경 횟수 (최대 2회)
ALTER TABLE activity ADD COLUMN IF NOT EXISTS deadline_change_count INTEGER NOT NULL DEFAULT 0;

-- 지원 마감일
ALTER TABLE activity ADD COLUMN IF NOT EXISTS application_deadline TIMESTAMPTZ;

-- 1차 참여 공고일
ALTER TABLE activity ADD COLUMN IF NOT EXISTS result_date TIMESTAMPTZ;

-- 지원서 생성 여부 (false=선착순)
ALTER TABLE activity ADD COLUMN IF NOT EXISTS has_application_form BOOLEAN NOT NULL DEFAULT false;

-- ============================================================
-- 7. activity_participant 테이블 변경
-- ============================================================

-- 참여자 역할 (LEADER/MANAGER/PARTICIPANT)
ALTER TABLE activity_participant ADD COLUMN IF NOT EXISTS role VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANT';

-- 선착순 대기열 순번
ALTER TABLE activity_participant ADD COLUMN IF NOT EXISTS waitlist_order INTEGER;

-- ============================================================
-- 8. recruitment_posting 테이블 변경
-- ============================================================

-- 비상연락처
ALTER TABLE recruitment_posting ADD COLUMN IF NOT EXISTS emergency_contact VARCHAR(100);

-- 지원 마감일 변경 횟수
ALTER TABLE recruitment_posting ADD COLUMN IF NOT EXISTS deadline_change_count INTEGER NOT NULL DEFAULT 0;

-- 추가합격 기간 종료일
ALTER TABLE recruitment_posting ADD COLUMN IF NOT EXISTS extra_acceptance_end_date TIMESTAMPTZ;

-- ============================================================
-- 9. club_role permissions 비트 마이그레이션
--    기존: 8번=MANAGE_MEMBER(1<<7), 9번=MANAGE_FEDERATION(1<<8)
--    변경: 8번=VIEW_APPLICATION(1<<7), 9번=MANAGE_MEMBER(1<<8), 10번=MANAGE_FEDERATION(1<<9)
--
--    비트 시프트: 기존 8번(1<<7)→9번(1<<8), 기존 9번(1<<8)→10번(1<<9)
--    새 8번(1<<7)은 기존에 없으므로 0
-- ============================================================

-- 먼저 기존 MANAGE_FEDERATION(1<<8) 비트를 10번(1<<9)으로 이동
UPDATE club_role SET permissions = (
    -- 기존 1~7번 비트는 유지 (하위 7비트: 0x7F)
    (permissions & 127)
    -- 기존 8번(MANAGE_MEMBER, 1<<7) → 9번(1<<8)
    | (CASE WHEN (permissions & 128) != 0 THEN 256 ELSE 0 END)
    -- 기존 9번(MANAGE_FEDERATION, 1<<8) → 10번(1<<9)
    | (CASE WHEN (permissions & 256) != 0 THEN 512 ELSE 0 END)
) WHERE permissions > 0;
