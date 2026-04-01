# Recruitment 모듈 변경사항

> 기능명세서 + dewple-ERD-changes.md 기반으로 현재 구현 대비 변경/추가가 필요한 사항을 정리합니다.

---

## 1단계: 엔티티 & DB 스키마 변경 ✅

### 1-1. RecruitmentPosting 엔티티

| 작업 | 필드 | 타입 | 설명 | 완료 |
|------|------|------|------|------|
| **추가** | `firstAnnouncementDate` | `DATE` | 1차 합격 공고일 (ERD 변경 9번) | ✅ |
| **추가** | `generationStart` | `VARCHAR(5)`, DEFAULT `'1'` | 기수 시작값: `'0'` 또는 `'1'` (ERD 변경 9번) | ✅ |
| **추가** | `emergencyContact` | `VARCHAR(100)` | V4에서 DB 추가됨 — 엔티티에 필드 반영 | ✅ |
| **추가** | `deadlineChangeCount` | `INT`, DEFAULT `0` | V4에서 DB 추가됨 — 엔티티에 필드 반영 (검증 로직은 4단계) | ✅ |
| **추가** | `extraAcceptanceEndDate` | `TIMESTAMPTZ` | V4에서 DB 추가됨 — 엔티티에 필드 반영 | ✅ |
| **리네임** | `isInterviewRequired` → `hasSecondInterview` | - | ERD `has_second_interview`와 통일. DB 컬럼명도 함께 변경 | ✅ |

### 1-2. Application 엔티티

| 작업 | 필드 | 타입 | 설명 | 완료 |
|------|------|------|------|------|
| **추가** | `ApplicationStatus.WAITLISTED` | enum 값 | 합격예비 상태 (명세서 190번) | ✅ |
| **제거** | `guestPhone` 기반 비회원 지원 | - | 명세서: "회원만 지원 가능" → 엔티티/서비스/컨트롤러/DTO/테스트/SecurityConfig 일괄 제거 | ✅ |

### 1-3. Flyway 마이그레이션

V10 마이그레이션 작성 완료 (`V10__update_recruitment_posting_for_feature_spec.sql`) ✅

```sql
-- recruitment_posting 컬럼 추가
ALTER TABLE recruitment_posting ADD COLUMN first_announcement_date DATE;
ALTER TABLE recruitment_posting ADD COLUMN generation_start VARCHAR(5) NOT NULL DEFAULT '1';
-- 컬럼 리네임
ALTER TABLE recruitment_posting RENAME COLUMN is_interview_required TO has_second_interview;
```

---

## 2단계: 합격 처리 로직 변경 (임팩트 최대) ✅

### 현재 상태 전이

```
TEMPORARY → SUBMITTED → ACCEPTED
                      → REJECTED
```

### 명세서 요구 상태 전이

```
TEMPORARY → SUBMITTED → ACCEPTED
                      → REJECTED → WAITLISTED → ACCEPTED
                      → WAITLISTED → ACCEPTED
                                   → REJECTED
```

### 변경 항목

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 합격예비(`WAITLISTED`) 상태 | 없음 | `ApplicationStatus` enum에 `WAITLISTED` 추가 | ✅ (1단계) |
| 불합격→합격 직접 변경 불가 | 제한 없음 | `REJECTED → ACCEPTED` 전이 차단 검증 추가 | ✅ |
| 불합격→합격예비→합격 경로만 허용 | 없음 | 상태 전이 매트릭스 검증 로직 구현 | ✅ |
| 모집 종료 전까지 상태 변경 가능 | 존재 | 기존 로직 유지 | ✅ |
| 합격예비자 → 추가합격 기간 설정 (1~14일) | `extraAcceptanceEndDate` 컬럼 존재 | 추가합격 기간 설정 API + 기간 내 `WAITLISTED → ACCEPTED` 허용 | 6단계에서 처리 |

### 수정 대상 파일

- `ApplicationStatus.java` — `WAITLISTED` 값 추가
- `ApplicationManageService.changeApplicationStatus()` — 상태 전이 검증 로직
- `ApplicationManageService.batchChangeApplicationStatus()` — 동일 검증 적용
- `ChangeApplicationStatusRequest` — `WAITLISTED` 허용
- `RecruitmentErrorCode` — 상태 전이 위반 에러코드 추가

---

## 3단계: 지원 철회 & 재지원

| 규칙 | 현재 | 필요 작업 | 상태 |
|------|------|-----------|------|
| 합격/불합격/합격예비 결정 전까지만 철회 | `ACCEPTED`, `REJECTED`만 차단 | `WAITLISTED`도 철회 차단 목록에 추가 | ✅ (2단계에서 함께 처리) |
| 철회 후 재지원 가능 | soft delete 처리 | 재지원 시 기존 INACTIVE 지원서를 무시하는지 쿼리 확인 |

### 수정 대상 파일

- `ApplicationService.withdrawApplication()` — `WAITLISTED` 철회 차단 추가
- `ApplicationRepositoryCustom` 쿼리 — 재지원 시 INACTIVE 지원서 제외 확인

---

## 4단계: 마감일 변경 제한 ✅

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 지원 마감일 변경 최대 2회 | `deadlineChangeCount` 컬럼만 존재, 로직 미구현 | 마감일 변경 시 카운트 증가 + 2회 초과 예외 처리 |

### 구현 방안

- `RecruitmentService.updateRecruitment()` 또는 별도 `changeDeadline()` 메서드
- `RecruitmentPosting`에 `changeDeadline(OffsetDateTime newEndAt)` 도메인 메서드 추가
  - `deadlineChangeCount >= 2`이면 `BusinessException` throw
  - 카운트 증가 + `endAt` 변경

### 수정 대상 파일

- `RecruitmentPosting.java` — `changeDeadline()` 메서드 추가
- `RecruitmentService.java` — 마감일 변경 로직
- `RecruitmentErrorCode` — `DEADLINE_CHANGE_LIMIT_EXCEEDED` 추가
- `UpdateRecruitmentRequest` — `endAt` 필드 추가 (현재 title, content, form만 수정 가능)

---

## 5단계: 공고 수정 규칙 변경 ✅

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 버전관리 안함 | `recentRecruitmentVersion` + `RecruitmentSchema` 버전 시스템 | 명세서와 충돌 — 제거 또는 내부용 유지 결정 필요 |
| 컴포넌트 추가/삭제만 가능 | 삭제 불가 (`FORM_COMPONENT_REMOVAL_NOT_ALLOWED`) | 명세서는 삭제도 허용 → 삭제 차단 로직 제거 |
| 동시 편집 방지 (5분 타임아웃) | 미구현 | 편집 잠금 메커니즘 구현 (DB 또는 Redis) |

### 수정 대상 파일

- `RecruitmentService.updateRecruitment()` — 컴포넌트 삭제 차단 로직 제거
- `RecruitmentErrorCode` — `FORM_COMPONENT_REMOVAL_NOT_ALLOWED` 제거 또는 미사용 처리

### 동시 편집 방지 (별도 작업)

- `RecruitmentPosting`에 `editLockedBy` (Long), `editLockedAt` (OffsetDateTime) 컬럼 추가
- 수정 시작 시 잠금 획득, 5분 타임아웃 후 자동 해제
- 잠금 중 다른 유저 접근 시 예외 반환

---

## 6단계: 모집 종료 프로세스 ✅ (스케줄러 제외)

### 현재 구현

- `closeRecruitment()` — OPEN → CLOSED로 상태 변경만 수행

### 명세서 요구사항

```
모집 종료 요청
  ├─ 합격예비자 없음 → 즉시 CLOSED
  └─ 합격예비자 있음 → 추가합격 기간 설정 (1~14일)
                       ├─ 기간 내: WAITLISTED → ACCEPTED 허용
                       └─ 기간 만료 → 자동 최종 CLOSED
```

### 필요 작업

| 작업 | 설명 |
|------|------|
| 종료 시 `WAITLISTED` 지원자 존재 여부 체크 | 있으면 추가합격 기간 입력 요구 |
| 추가합격 기간 설정 API | `extraAcceptanceEndDate` 저장 (1~14일 범위 검증) |
| 추가합격 기간 중 상태 변경 허용 | `WAITLISTED → ACCEPTED/REJECTED`만 허용 |
| 기간 만료 후 자동 종료 | 스케줄러 또는 이벤트 기반 처리 |

### 수정 대상 파일

- `RecruitmentService.closeRecruitment()` — 합격예비자 체크 + 추가합격 기간 로직
- `CloseRecruitmentRequest` DTO 추가 — `additionalAcceptanceDays` (1~14)
- `RecruitmentErrorCode` — `WAITLISTED_APPLICANTS_EXIST`, `INVALID_ADDITIONAL_PERIOD` 추가
- 스케줄러 (app-worker 또는 `@Scheduled`) — 추가합격 기간 만료 자동 처리

---

## 7단계: 데이터 보관 (CSV 자료실 저장)

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 자료실에 CSV 저장 | 미구현 | Archive(자료실) 모듈 의존 — 자료실 구현 후 연동 |
| 보관 설정: `AUTO`/`NEVER`/`ASK` | ERD: `Club.applicationSaveSetting` | Club 엔티티에 필드 추가 |
| 모집 종료 시 설정에 따라 CSV 자동 생성 | 미구현 | 종료 프로세스에서 분기 처리 |
| 탈퇴 유저의 지원서 응답 삭제 | 미구현 | 회원 탈퇴 이벤트에서 `application.answers` null 처리 |

### 의존성

- `Archive` 모듈 (미구현) — `ArchiveFolder`, `ArchiveFile` 테이블
- CSV 변환 유틸리티
- 회원 탈퇴 이벤트 리스너

---

## 8단계: 기수 자동 증가 ✅

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 기수 auto increase | `generationId` FK로 `ClubGeneration` 참조 | 공고 생성 시 마지막 기수 +1 자동 설정 |
| 수동 변경 가능 | 미확인 | 기수 번호 수동 입력 허용 |
| `generationStart` (0기/1기) | 없음 | `RecruitmentPosting`에 컬럼 추가 + 기수 계산 시 시작값 반영 |

### 수정 대상 파일

- `RecruitmentService.createRecruitment()` — 기수 자동 증가 로직
- `CreateRecruitmentRequest` — 기수 수동 입력 옵션 추가

---

## 9단계: 권한 매핑 세분화 ✅

### 현재 권한

| 현재 | 용도 |
|------|------|
| `MANAGE_RECRUITMENT` | 지원서 열람/관리 |
| `DECIDE_ADMISSION` | 합격 결정 |

### 명세서 18종 권한 중 모집 관련

| 비트 | 권한명 | 현재 매핑 | 확인 사항 |
|------|--------|-----------|-----------|
| 6 | 지원서제작/수정 | 공고 생성/수정에 사용 | 별도 분리 필요 여부 |
| 7 | 지원자합격결정 | `DECIDE_ADMISSION` | OK |
| 8 | 지원서답변열람 | `MANAGE_RECRUITMENT` | 7번 보유 시 자동 포함 로직 확인 (명세서: "7번 → 8번 자동 포함") |

### 수정 대상 파일

- `Permission.java` (common 모듈) — 18종 비트마스크 값 정의 확인
- `@RequireClubPermission` 사용처 — 권한 매핑 재확인
- `ApplicationManageController` — 열람은 8번, 합격결정은 7번으로 분리

---

## 10단계: 공고 수정 시 지원자 영향 처리 ✅

> 명세서 G120 (공고 수정 세부 규칙)에서 도출된 항목들

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 추가된 필수입력 컴포넌트 미작성 시 기존 지원자는 **임시저장 상태로 전환** | 미구현 | `updateRecruitment` 시 기존 SUBMITTED 지원자들의 답변을 새 폼과 비교, 필수 누락 시 `TEMPORARY`로 전환 |
| 삭제된 컴포넌트의 답변은 **운영진 페이지에서도 삭제** | 미구현 | `updateRecruitment` 시 삭제된 컴포넌트 key를 파악하고, 기존 지원자 답변 JSON에서 해당 key 제거 |
| 2차 면접 여부 변경 가능 **(참→거짓: when2meet 자동 삭제, 거짓→참: 자동 추가)** | `updateRecruitment`에서 `hasSecondInterview` 변경 미지원 | `UpdateRecruitmentCommand`에 `hasSecondInterview` 필드 추가 + 면접 프로세스/when2meet 컴포넌트 자동 생성/삭제 |
| 컴포넌트 **수정 불가** (추가/삭제만) | 미검증 | 기존 컴포넌트의 question/config 변경을 감지하여 차단하는 검증 로직 추가 |

### 수정 대상 파일

- `RecruitmentService.updateRecruitment()` — 전체 로직 확장
- `UpdateRecruitmentCommand` — `hasSecondInterview` 필드 추가
- `UpdateRecruitmentRequest` — `hasSecondInterview` 필드 추가
- `ApplicationRepository` — 공고별 SUBMITTED 지원자 일괄 조회 메서드 추가
- `Application.updateApplicationStatus()` — 기존 지원자 상태 전환에 사용

---

## 11단계: 지원서 임시저장 규칙

> 명세서: "지원서 임시저장: 회원만, 공고당 1개, 지원 마감일까지 보존"

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 공고당 임시저장 1개 | 기존 TEMPORARY 찾아서 갱신하므로 사실상 1개 유지 | ✅ 이미 구현됨 (검증만 확인) |
| 지원 마감일까지 보존 (마감 후 자동 삭제) | 미구현 | 스케줄러 필요 → [RecruitmentUndone.md](RecruitmentUndone.md) |

---

## 12단계: 지원서 응답 PDF 다운로드

> 명세서: "지원서 응답 PDF 다운로드 → 개인 자료실에 수동 업로드 보관 가능"

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 지원서 응답 PDF 다운로드 | 미구현 | PDF 생성 라이브러리 + 다운로드 API 추가 → [RecruitmentUndone.md](RecruitmentUndone.md) |

---

## 13단계: 탈퇴 유저 지원서 응답 삭제

> 명세서: "탈퇴 유저의 게시물/댓글은 유지, 지원서 응답은 삭제"

| 규칙 | 현재 | 필요 작업 |
|------|------|-----------|
| 탈퇴 시 지원서 응답 삭제 | 미구현 | 회원 탈퇴 이벤트에서 `application.answers`를 null 처리 → [RecruitmentUndone.md](RecruitmentUndone.md) |

---

## 확인 필요 사항 (의사결정)

| # | 질문 | 선택지 | 답 | 상태 |
|---|------|--------|----|----|
| 1 | 게스트(비회원) 지원 제거? | 명세서: "회원만 지원 가능" → `guestPhone`, 게스트 API 제거 / 유지 | 제거 | 결정됨 (후속 단계에서 처리) |
| 2 | 버전 관리 시스템 유지? | 명세서: "버전관리 안함" → `RecruitmentSchema` 버전 제거 / 내부용 유지 | 제거 | 결정됨 (후속 단계에서 처리) |
| 3 | `editWindowBasis`/`editWindowDays` 유지? | 마감일까지 수정 가능으로 단순화 | 제거 | ✅ 완료 — 마감일 기준으로 단순화, 컬럼 제거 |
| 4 | `isInterviewRequired` → `hasSecondInterview` 이름 변경? | ERD 문서와 통일 여부 | `hasSecondInterview`로 통일 | ✅ 완료 |
| 5 | 동시 편집 방지 구현 시점? | DB 기반 (즉시) vs Redis 기반 (인프라 추가 필요) | 불명확 | → [RecruitmentUndone.md](RecruitmentUndone.md) #3 |             

---

## 우선순위

| 순위 | 단계 | 영향 범위 | 난이도 | 상태 |
|------|------|-----------|--------|------|
| **P0** | 1단계: 엔티티 & DB 마이그레이션 | 엔티티, 마이그레이션 | 하 | ✅ 완료 |
| **P0** | 2단계: 합격예비 상태 + 상태 전이 | 엔티티, 서비스, 컨트롤러, 테스트 | 중 | ✅ 완료 |
| **P1** | 3단계: 지원 철회 + 재지원 | 서비스 | 하 | ✅ 완료 (2단계에서 함께) |
| **P1** | 4단계: 마감일 변경 제한 | 서비스 | 하 | ✅ 완료 |
| **P1** | 5단계: 공고 수정 규칙 (컴포넌트 삭제 허용) | 서비스 | 하 | ✅ 완료 |
| **P1** | 6단계: 모집 종료 프로세스 | 서비스, 스케줄러 | 중 | ✅ 서비스 완료 (스케줄러 별도) |
| **P2** | 8단계: 기수 자동 증가 | 서비스 | 하 | ✅ 완료 |
| **P2** | 9단계: 권한 매핑 세분화 | 횡단 | 중 | ✅ 완료 |
| **P1** | 10단계: 공고 수정 시 지원자 영향 처리 | 서비스 | 중 | ✅ 완료 |
| **P2** | 11단계: 임시저장 규칙 검증 | 서비스 | 하 | 일부 구현 (스케줄러 보류) |
| **P3** | 7단계: 데이터 보관 (CSV) | 자료실 모듈 의존 | 상 | 보류 (Archive 모듈 미구현) |
| **P3** | 12단계: 지원서 응답 PDF | PDF 라이브러리 | 중 | 보류 |
| **P3** | 13단계: 탈퇴 유저 응답 삭제 | 이벤트 리스너 | 하 | 보류 |
| **P3** | 5단계: 동시 편집 방지 | 인프라 | 중 | 보류 ([RecruitmentUndone.md](RecruitmentUndone.md)) |
