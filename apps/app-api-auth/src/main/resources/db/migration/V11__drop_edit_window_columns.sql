-- 지원서 수정 기간을 지원 마감일 기준으로 단순화 (editWindowBasis/editWindowDays 제거)
ALTER TABLE recruitment_posting DROP COLUMN IF EXISTS edit_window_basis;
ALTER TABLE recruitment_posting DROP COLUMN IF EXISTS edit_window_days;
