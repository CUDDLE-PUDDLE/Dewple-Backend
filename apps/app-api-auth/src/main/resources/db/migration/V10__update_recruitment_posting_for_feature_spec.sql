-- V10: 모집 공고 기능명세서 반영
-- 2026-03-30

-- ============================================================
-- 1. recruitment_posting 컬럼 추가
-- ============================================================

-- 1차 합격 공고일
ALTER TABLE recruitment_posting ADD COLUMN IF NOT EXISTS first_announcement_date DATE;

-- 기수 시작값 (0기 or 1기)
ALTER TABLE recruitment_posting ADD COLUMN IF NOT EXISTS generation_start VARCHAR(5) NOT NULL DEFAULT '1';

-- ============================================================
-- 2. recruitment_posting 컬럼 이름 변경
--    is_interview_required → has_second_interview (명세서: "2차 면접 여부")
-- ============================================================

ALTER TABLE recruitment_posting RENAME COLUMN is_interview_required TO has_second_interview;

-- ============================================================
-- 3. application 테이블 — 비회원 지원 제거
--    명세서: "회원만 지원 가능"
-- ============================================================

-- applicant_id를 NOT NULL로 변경 (기존 NULL 데이터가 있으면 먼저 정리 필요)
DELETE FROM application WHERE applicant_id IS NULL;
ALTER TABLE application ALTER COLUMN applicant_id SET NOT NULL;

-- guest_phone 컬럼 제거
ALTER TABLE application DROP COLUMN IF EXISTS guest_phone;
