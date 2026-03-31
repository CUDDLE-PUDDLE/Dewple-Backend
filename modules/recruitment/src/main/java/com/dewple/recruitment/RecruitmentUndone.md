# Recruitment 모듈 — 미결정/보류 사항

> 기능명세서에서 불명확하거나, 기획 확인이 필요한 사항을 정리합니다.

---

## 1. 지원서 수정 가능 기간 (`editWindowBasis`/`editWindowDays`)

### 현재 구현

- `editWindowBasis` (SUBMITTED / DEPLOYED) + `editWindowDays`로 지원서 수정 마감 기한을 계산
- SUBMITTED: 지원 제출일로부터 N일
- DEPLOYED: 공고 배포일로부터 N일

### 명세서 내용

- 공고 수정 관련 명세에서 **지원서 수정 가능 기간에 대한 직접적 언급이 없음**
- 공고 수정 시 "기존 답변은 유지", "추가된 필수입력 컴포넌트 미작성 시 기존 지원자는 임시저장 상태로 전환" 등의 규칙만 명시

### 결정 필요 사항

- [ ] 지원서 수정은 **지원 마감일까지** 가능한 것으로 단순화할지?
- [ ] 아니면 기존 `editWindowBasis`/`editWindowDays` 방식을 유지할지?
- [ ] 단순화 시 `editWindowBasis`, `editWindowDays` 컬럼 및 관련 로직 제거 필요

---

## 2. 공고 수정 가능 시점

### 명세서 내용 (G120)

- 버전관리 하지 않음 (최종 수정 폼만 유지)
- 컴포넌트 추가/삭제만 가능 (컴포넌트 수정 불가)
- 2차 면접 여부 변경 가능
- 제목/내용 수정 가능
- 수정 시 기존 지원자에게 알림 발송

### 불명확한 사항

- [ ] 수정 가능 **시작 시점**: OPEN(모집 중)이 된 직후부터? 아니면 DRAFT 포함?
- [ ] 수정 가능 **종료 시점**: 지원 마감일까지? 모집 종료(CLOSED)까지? 추가합격 기간 중에도?
- [ ] "기존 지원자에게 알림 발송" → 지원자가 존재하는 OPEN 상태에서의 수정을 전제하는 것으로 보임

### 추정

- OPEN 상태에서 수정 가능, CLOSED 이후 수정 불가가 자연스러움
- 기획 확인 필요

---

## 3. 동시 편집 방지 구현 방식

### 명세서 내용

- 수정 중 다른 유저 접근 차단
- 잠금 해제: 세션 종료 시 즉시 + 5분 타임아웃

### 구현 선택지

| 방식 | 장점 | 단점 |
|------|------|------|
| **DB 기반** (`editLockedBy`, `editLockedAt` 컬럼) | 인프라 추가 불필요, 즉시 구현 가능 | 세션 종료 감지 불가 (5분 타임아웃에 의존), 폴링 필요 |
| **Redis 기반** (분산 잠금) | TTL로 자동 만료, 세션 종료 시 즉시 해제 가능 (WebSocket 연동) | Redis 인프라 필요, 복잡도 증가 |

### 결정 필요 사항

- [ ] DB 기반으로 먼저 구현하고 추후 Redis로 전환? 아니면 처음부터 Redis?
- [ ] 세션 종료 시 즉시 해제 — WebSocket heartbeat 방식? 아니면 프론트에서 `beforeunload` 시 API 호출?
- [ ] 적용 범위: 모집 공고만? 소개페이지, 지원서 등도 동시 편집 방지 필요?

---

## 4. 공고 수정 시 기존 지원자 알림 발송

### 명세서 내용

- 공고 수정 시 기존 지원자에게 알림 발송

### 보류 사유

- `Notification` 모듈이 미구현 상태
- 알림 인프라 (FCM 푸시 / 알림톡 / SMS / 인앱) 연동 필요
- 명세서 알림 정책 (3계층: 필수/서비스운영/마케팅) 구현 후 연동

### 구현 시 참고

- `RecruitmentService.updateRecruitment()` 완료 후 이벤트 발행
- `@EventListener` 또는 SQS 메시지로 비동기 알림 처리

---

## 5. 임시저장 지원서 마감일 자동 삭제

### 명세서 내용

- 지원서 임시저장: 지원 마감일까지 보존 (마감 후 자동 삭제/정리)

### 보류 사유

- 스케줄러 인프라 필요 (`app-worker` 또는 `@Scheduled`)
- 마감일 도래한 공고의 TEMPORARY 상태 지원서를 일괄 soft-delete

### 구현 시 참고

```java
// 마감일 경과한 공고의 임시저장 지원서 정리
applicationRepository.findTemporaryApplicationsWithExpiredPostings()
    .forEach(Application::inactivate);
```

---

## 6. 추가합격 기간 만료 자동 종료

### 명세서 내용

- 합격예비자 추가합격 기간 (1~14일) 만료 후 자동 최종 종료

### 보류 사유

- 스케줄러 인프라 필요
- `extraAcceptanceEndDate`가 경과한 CLOSED 공고를 최종 ARCHIVED로 전환
- 남은 WAITLISTED 지원자는 최종 REJECTED 처리 여부 — 기획 확인 필요

---

## 7. 지원서 응답 PDF 다운로드

### 명세서 내용

- 지원서 응답 PDF 다운로드 → 개인 자료실에 수동 업로드 보관 가능

### 보류 사유

- PDF 생성 라이브러리 선정 필요 (iText, OpenPDF, Flying Saucer 등)
- 개인 자료실 (Archive 모듈) 미구현
- S3 업로드 연동 필요

---

## 8. 탈퇴 유저 지원서 응답 삭제

### 명세서 내용

- 탈퇴 유저의 게시물/댓글은 유지 (유저명 → '(알 수 없음)'), **지원서 응답은 삭제**

### 보류 사유

- 회원 탈퇴 이벤트 시스템 미구현 (Spring `@EventListener` 또는 도메인 이벤트)
- 탈퇴 시 해당 유저의 모든 `application.answers`를 null 처리
- soft-delete 유예 기간 (1주일) 후 hard-delete 시점에 실행할지, soft-delete 시점에 실행할지 결정 필요

---

## 9. 데이터 보관 — CSV 자료실 저장

### 명세서 내용

- 자료실에 CSV 저장 (자동보관/미보관/매번선택)
- `Club.applicationSaveSetting`: `AUTO`, `NEVER`, `ASK`

### 보류 사유

- `Archive` 모듈 (`ArchiveFolder`, `ArchiveFile` 테이블) 미구현
- CSV 변환 유틸리티 필요
- 모집 종료 프로세스에서 설정에 따라 분기 처리
