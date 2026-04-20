# ERD

### 엔티티 공통 구조

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 생성일시 | created_at | TIMESTAMPTZ | N |  |
| 수정일시 | updated_at | TIMESTAMPTZ | N |  |
| 상태 | status | VARCHAR(20) | N | 기본 : `ACTIVE`, `INACTIVE`
테이블마다 필요한 경우 커스텀 추가 |

### 회원 User

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 유저 아이디 | user_id | VARCHAR(255) | Y |  |
| 비밀번호 | password | VARCHAR(255) | Y |  |
| 프로필 사진 | profile_img | VARCHAR(255) | N |  |
| 이름 | name | VARCHAR(255) | N |  |
| 닉네임 | nickname | VARCHAR(255) | N |  |
| 생년월일 | birthdate | DATE | N |  |
| 성별 | gender | VARCHAR(20) | N | MALE, FEMALE |
| 이메일 | email | VARCHAR(50) | N |  |
| 대학교 | university | ENUM | Y | 바꿀 수 있음 |
| 졸업 여부 | is_graduated | BOOLEAN | Y |  |
| 직장 | workplace | VARCHAR(100) | Y |  |
| 인증 여부 |  | BOOLEAN | N | 추후에 본인인증이 붙을 경우 고려 |
| 전화번호 | phone | VARCHAR(20) | N | UQ. 메인 유니크 키 |
| 플랜: enum | plan | VARCHAR(20) | N | default: `FREE` |
| 자기소개 | self_introduction | TEXT | N |  |
| MBTI | mbti | ENUM | N |  |
| 평판 | reputation_score | DECIMAL(1, 1) | Y | DEFAULT 5.0 |
| 내 정보 공개 여부 |  |  |  |  |

### 인증 코드 Verification

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 공개 아이디 | public_id | UUID | N |  |
| 회원 ID | user_id | BIGINT | Y | FK → `user(id)` |
| 인증 완료 토큰 | verificated_token | VARCHAR(2048) | Y |  |
| 인증 대상 | verification_target | VARCHAR(255) | N |  |
| 인증 코드 | verification_code | VARCHAR(2048) | N | FK → `category(id)` |
| 인증 방식 | verification_type | VARCHAR(100) | N | PHONE, EMAIL |
| 만료 시간 | expire_at | TIMESTAMPTZ | N |  |
| 인증 목적 | verification_purpose | VARCHAR(50) | N | `SIGN_UP`, `CHANGE_PHONE`, `GUEST_APPLICATION` |

### 회원-카테고리(관심사)

- 회원-카테고리 다대다 매핑 테이블

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 회원 ID | user_id | BIGINT | N | FK → `user(id)` |
| 카테고리 ID | category_id | BIGINT | N | FK → `category(id)` |

### 소셜 Social_Account

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 회원 ID | user_id | BIGINT | N | FK → `user(id)` |
| 제공자 | provider | VARCHAR(20) | N | KAKAO, NAVER, GOOGLE |
| 제공자 ID | provider_id | VARCHAR(100) | N | UNIQUE |

### 동아리-회원 Club_Member

- UNIQUE (club_id, user_id)
- CHECK: `member_level='STAFF'` 인 경우 `role_id IS NOT NULL` 강제

```sql
FOREIGN KEY (club_id, role_id)
REFERENCES club_role (club_id, id)
```

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 회원-동아리 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` (ON DELETE CASCADE 권장) |
| 회원 ID | user_id | BIGINT | N | FK → `user(id)` |
| 역할 ID | role_id | BIGINT | Y | FK → `club_role(id)` (운영진일 때 지정 권장) |
| 가입 기수 ID | join_generation_id | BIGINT | Y | FK → `club_generation(id)` |
| 활동상태 | activity_status | VARCHAR(20) | N | enum 권장: `ACTIVE`, `DORMANT`, `GRADUATED`, `KICKEDOUT`, `LEFT` / DEFAULT `ACTIVE` |

### 멤버-기수 Member_Generation

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 멤버 ID | member_id | BIGINT | N | FK → `club_member(id)` |
| 기수 ID | generation_id | BIGINT | N | FK → `generation(id)` |

### 멤버-동아리부서 Member_Department

```sql
TODO: 멤버의 동아리와 부서의 동아리 통일 제약
```

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 멤버 ID | member_id | BIGINT | N | FK → `club_member(id)` |
| 부서 ID | department_id | BIGINT | N | FK → `club_department(id)`  |

### 동아리 Club

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 동아리 ID | id | BIGSERIAL | N | PK |
| 생성자 ID | creator_id | BIGINT | N | FK → `user(id)` |
| 연합회 ID | organization_id | BIGINT | Y | FK → organization(id) |
| 대학교 | university | ENUM | Y | 못 바꿈. 연합으론 바꿀 수 있음 - null로 바꾸면 연합 동아리? 아님 교내 동아리 여부 컬럼 추가?
→ 나중에 확장성 고려하면 지역이랑 관련해서 테이블로 따로 빼기 |
| 이름 | name | VARCHAR(100) | N |  |
| 설명 | description | TEXT | Y |  |
| 대표사진 URL | cover_img | VARCHAR(2048) | Y |  |
| 소개페이지 | landing_page | JSONB | Y | `[
  {
    "type": "title",
    "content": "제목입니다",
    "font": "Pretendard",
    "style": "BOLD",
    "color": "#000000",
    "size": 14
  },
  {
    "type": "text",
    "content": "~~~",
    "font": "Pretendard",
    "style": "BOLD",
    "color": "#222222",
    "size": 11
  },
  ...
]` |
| 평판 | reputation_score | DECIMAL(1,2) | N | DEFAULT 5.0 |
| 성별 | gender | VARCHAR(20) | N | MALE, FEMALE |
| 최저 연령대 | min_age | BIGINT | Y |  |
| 최고 연령대 | max_age | BIGINT | Y |  |
| 활동 방식 | activity_type | ENUM | N | `ONLINE`, `OFFLINE`, BOTH
default `BOTH` |
| 공개 여부 | is_public | BOOLEAN | N | default `TRUE` |

### 연합회 Organization

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 연합회 ID | id | BIGSERIAL | N | PK |
| 생성자 ID | creator_id | BIGINT | N | FK → `user(id)` |
| 이름 | name | VARCHAR(100) | N |  |
| 설명 | description | TEXT | Y |  |
| 대표사진 URL | cover_img | VARCHAR(2048) | Y |  |
| 소개페이지 | landing_page | JSONB | Y | `[
  {
    "type": "title",
    "content": "제목입니다",
    "font": "Pretendard",
    "style": "BOLD",
    "color": "#000000",
    "size": 14
  },
  {
    "type": "text",
    "content": "~~~",
    "font": "Pretendard",
    "style": "BOLD",
    "color": "#222222",
    "size": 11
  },
  ...
]` |
| 공개 여부 | is_public | BOOLEAN | N | default `TRUE` |

### 동아리-부서 Club_Department

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` |
| 부서명 | name | VARCHAR(100) | N |  |

### 동아리-카테고리 Club_Category

- 동아리-카테고리 다대다 매핑 테이블

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` |
| 카테고리 ID | category_id | BIGINT | N | FK → `category(id)` |

### 동아리-지역 Club_Region

- 동아리-지역 다대다 매핑 테이블

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` |
| 지역 ID | region_id | BIGINT | N | FK → `region(id)` |

### 동아리-문의 Club_Inquiry

- 익명임

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 문의 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK |
| 작성자 ID | creator_id | BIGINT | N | FK → `user(id)` |
| 익명 닉네임 | anonymity_nickname | VARCHAR(50) | N | 익명 |
| 내용 | content | TEXT | N |  |
| 부모 ID | parent_id | BIGINT | Y | FK → `inquiry(id)` |

### 동아리-역할 Club_Role

- `UNIQUE (club_id, name)`

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 역할 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` (ON DELETE CASCADE) |
| 역할 이름 | name | VARCHAR(50) | N | 동아리 내 역할명 |
| 접근권한 | permissions | BIGINT | N | 권한 비트마스킹
ex) 110 → 첫번째,두번쨰 권한만 허용 |
- 권한
    
    
    | 권한 | 설명 |
    | --- | --- |
    | 동아리 소개/태그 변경 | 소개페이지 및 태그 수정 |
    | 활동 생성/수정 | 동아리 활동(소모임) 생성/수정 |
    | 동아리 네트워킹 | 타 동아리와 채팅 권한 |
    | 문의 답변 | 동아리 문의 답변/수정/삭제 |
    | 지원서 제작/수정/열람 | 모집 공고/지원서 관리 |
    | 합격 여부 결정 | 지원자 합/불 처리 |
    | 회원 관리 | 역할 부여/초대/퇴출 등 |

### 동아리 기수 Club_Generation

- `UNIQUE (club_id, generation_no)`

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 기수 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` (ON DELETE CASCADE) |
| 기수 | generation_no | INT | N | 예: 1, 2, 3… |
| 시작일 | start_date | DATE | N |  |
| 종료일 | end_date | DATE | Y | 기수 구분이 없는 동아리의 경우 NULL 허용(선택) |

### 관심 동아리 Club_Interest

- UNIQUE (club_id, user_id)

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` (ON DELETE CASCADE 권장) |
| 회원 ID | user_id | BIGINT | N | FK → `user(id)` |

### 공고 Recruitment_Posting

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 공고 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | Y | FK → `club(id)` (ON DELETE CASCADE) |
| 활동 ID | activity_id | BIGINT | Y |  |
| 기수 ID | generation_id | BIGINT | Y | FK → `generation(id)` |
| 작성자 ID | creator_id | BIGINT | N | FK → `user(id)` |
| 공고 제목 | title | VARCHAR(100) | N |  |
| 공고 설명 | description | TEXT | Y |  |
| 테마 컬러 | theme_color | VARCHAR(100) | Y |  |
| 모집 인원 | capacity | INT | Y | 제한 없으면 NULL 또는 큰 값 |
| 상태 | status | VARCHAR(20) | N | enum 권장: `DRAFT`, `OPEN`, `CLOSED`, `ARCHIVED` 등 |
| 최신 지원 양식 버전 | recent_recruitment_version | BIGINT | N |  |
| 시작 일시 | start_at | TIMESTAMPTZ | N |  |
| 종료 일시 | end_at | TIMESTAMPTZ | N | `end_at > start_at` CHECK 권장 |
| 조회수 | view_count | BIGINT | N | DEFAULT = 0 |
- [x]  2차 면접 관련

### 지원 절차 Recruitment_Process

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 절차 ID | id | BIGSERIAL | N | PK |
| 공고 ID | posting_id | BIGINT | N | FK → `posting(id)` (ON DELETE CASCADE) |
| 절차 순서 | process_order | INT | N |  |
| 절차명 | name | VARCHAR(50) | N |  |
| 설명 | description | TEXT | Y |  |
| 절차 종류 | process_type | VARCHAR(100) | N | `DOCUMENT`, `INTERVIEW`, `FINAL`, … |
| 시작 일시 | start_at | TIMESTAMPTZ | Y |  |
| 종료 일시 | end_at | TIMESTAMPTZ | Y | `end_at > start_at` CHECK 권장 |
- [ ]  공고 만들 때 운영진이 지원 절차도 입력하도록 해야 함 : 사용자에게 서류, 1차 면접, 2차 면접 등 몇 개의 선택지를 주고 그 중에 순서대로 선택

### 지원양식 Recruitment_Schema

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 양식 ID | id | BIGSERIAL | N | PK |
| 지원절차 ID | recruitment_process_id | BIGINT | N | FK → `recruitment_process(id)` (ON DELETE CASCADE) |
| 버전 | version | BIGINT | N |  |
| 지원 양식 | application_form | JSONB | Y | 공고 시점의 폼 전체 스냅샷 저장용(선택). 질문 컴포넌트 테이블을 주로 쓰면 생략 가능 |

```json
{
	{
		"question": "자기소개를 작성해주세요",
    "type": "textarea",
    "required": true,
    "max_length": 500
  },
  {
    "question": "지원 동기를 작성해주세요",
    "type": "textarea",
    "required": true,
    "max_length": 300
  }
}
```

```json
// 면접일 떄 - 전체 면접 일정
// to 프론트: 따로 주세요
{
	"startDate": "2026-01-11",
	"endDate": "2026-01-14",
	"startTime": "",
	"endTime": "",
	"location": ["경영관 4호실", "새천년과 502호", "공학관 101호"]
}
```

### 공고-부서 Recruitment_Department

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 공고 ID | recruitment_id | BIGINT | N | FK → `recruitment(id)` |
| 부서 ID | department_id | VARCHAR(100) | N | FK → `department(id)` |

### 질문 컴포넌트 Form_Component

- `UNIQUE (posting_id, component_key)`
- `INDEX (posting_id, sort_order)`

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 질문 컴포넌트 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` (ON DELETE CASCADE) |
| 부서 ID | department_id | BIGINT | Y | FK → `department(id)` |
| 부모 컴포넌트 ID | parent_id | BIGINT | Y | FK → `form_component(id)` |
| 부모 컴포넌트 선택 번호 | parent_select_number | BIGINT | Y | 이 질문을 보여주는 경우에 대한 부모 컴포넌트 선지 번호 |
| 컴포넌트 키 | component_key | VARCHAR(32) | N | 예: `"d89wjw7by16k2083"` / 공고 내 유니크 |
| 질문 | question | TEXT | N |  |
| 타입 | input_type | VARCHAR(30) | N | 예: `textarea` 등(ENUM/체크제약 권장) |
| 필수 여부 | required | BOOLEAN | N | DEFAULT false |
| 최대 길이 | max_length | INT | Y | textarea 등에서 사용 |
| 추가 설정 | config | JSONB | Y | 선택지(options), placeholder 등 확장 필드 |
| 정렬 순서 | sort_order | INT | N | DEFAULT 0 |
- 지원서 component
    
    ```json
    
    {
    	"key": "d89wjw7by16k2083"
      "question": "자기소개를 작성해주세요",
      "type": "textarea",
      "required": true,
      "max_length": 500
    }
    ```
    

### 작성 시작한 지원 Application

- `UNIQUE (posting_id, applicant_id)` (지원은 1회/최신 1건만 유지하는 모델)
- 만약 “버전별 이력”이 필요하면 별도 `application_revision` 테이블로 분리 권장

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 지원서 ID | id | BIGSERIAL | N | PK |
| 지원 양식 ID | recruitment_schema_id | BIGINT | N | FK → `recruitment_schema(id)` (ON DELETE CASCADE) |
| 지원자 ID | applicant_id | BIGINT | Y | FK → `user(id)` |
| 비회원 전화번호 | guest_phone | VARCHAR(20) | Y | 비회원 전화번호 |
| 답변 | answers | JSONB | N | DEFAULT `'[]'::jsonb` / 예: `[{"key":"...","answer":"안녕"}]` |
| 면접 시작 일시 | interview_start_date | TIMESTAMP | Y | 확정된 면접 일시 |
| 면접 장소 | interview_location | VARCHAR(255) | Y | 확정된 면접 장소 |
| 상태 | application_status | VARCHAR(20) | N | enum: `SUBMITTED`, `ACCEPTED`, `REJECTED`, `TEMPORARY` ,
`FIX` |
- 답변(수정 가능)
    
    ```json
    [
    	{
    		"key":"d89wjw7by16k2083",
    		"answer":"안녕"
    	},
    	{
    		"key":"d89wjw7by16k2083",
    		"answer":"안녕"
    	},
    ]
    ```
    
- 지원 절차에 대한 사용자의 지원 정보

### 카테고리 Category

- UNIQUE (name)

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 카테고리 ID | id | BIGSERIAL | N | PK |
| 부모 id | parent_id | BIGSERIAL | Y | Fk → `category` |
| 이름 | name | VARCHAR(50) | N | 문자열 or ENUM 고민 |
| 계층 | depth | BIGINT | N | 1 → 시/도
2 → 구/군 |

### 지역 Region

- UNIQUE (city, name)
- 무관도 포함

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 지역 ID | id | BIGSERIAL | N | PK |
| 부모 ID | parent_id | BIGSERIAL | Y | FK → `region` |
| 이름 | name | VARCHAR(50) | N | 예: 신촌, 혜화, 회기 등 |

### 공지 Notice

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 공지 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | N | FK → `club(id)` (ON DELETE CASCADE 권장) |
| 작성자 ID | creator_id | BIGINT | N | FK → `club_member(id)` |
| 제목 | name | VARCHAR(100) | Y |  |
| 내용 | content | TEXT | N |  |
| 부모 ID | parent_id | BIGINT | Y | 댓글인 경우 부모 공지 id 존재 |
- 투표 기능

### 투표 Vote

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 투표 ID | id | BIGSERIAL | N | PK |
| 공지 ID | notice_id | BIGINT | N | FK → `notice(id)`  |
| 작성자 ID | creator_id | BIGINT | N | FK → `user(id)` |
| 제목 | name | VARCHAR(100) | Y |  |
| 설명 | description | TEXT | Y |  |
| 익명 여부 | is_anonymous | BOOLEAN | N |  |
| 다중 선택 여부 | is_multi | BOOLEAN | N |  |
| 항목 추가 가능 여부 | is_modifiable | BOOLEAN | N |  |
| 시작 일시 | start_at | TIMESTAMPTZ | N |  |
| 종료 일시 | end_at | TIMESTAMPTZ | N | `end_at > start_at` CHECK 권장 |

### 투표-컴포넌트 Vote_Component

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 투표 컴포넌트 ID | id | BIGSERIAL | N | PK |
| 투표 ID | vote_id | BIGINT | N | FK → `vote(id)` |
| 내용 | content | TEXT | N |  |

### 투표-참여자 Vote_Participant

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 투표 참여자 ID | id | BIGSERIAL | N | PK |
| 멤버 ID | member_id | BIGINT | N | FK → `club_member(id)` |
| 선택한 컴포넌트 ID | vote_component_id | BIGINT | N | FK → `vote_component(id)` |

### 활동 Activity

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 활동 ID | id | BIGSERIAL | N | PK |
| 동아리 ID | club_id | BIGINT | Y | FK → `club(id)` (ON DELETE CASCADE 권장) |
| 생성자 ID | creator_id | BIGINT | N | FK → `user(id)` |
| 공개 범위 | open_type | VARCHAR(30) | N | enum 권장: `PUBLIC`, `PRIVATE` |
| 이름 | name | VARCHAR(100) | N |  |
| 시작 일시 | start_at | TIMESTAMPTZ | N |  |
| 종료 일시 | end_at | TIMESTAMPTZ | N | `end_at > start_at` CHECK 권장 |
- dtype별 상세(투표/회식/공연/번개 등)
    - `activity_vote`, `activity_dining` 같은 1:1 서브 테이블로 확장

### 활동-참여자 Activity_Participant

- UNIQUE (activity_id, participant_id)

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 활동-참여자 ID | id | BIGSERIAL | N | PK |
| 활동 ID | activity_id | BIGINT | N | FK →`(id)` (ON DELETE CASCADE) |
| 참여자 ID | participant_id | BIGINT | N | FK → `user(id)` |
| 정산 완료 여부 | is_settlement_completed | BOOLEAN | Y | 정산이 없으면 null |

### 채팅방 Chat_Room

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 채팅방 ID | id | BIGSERIAL | N | PK |
| 동아리1 ID | club1_id | BIGINT | N | FK → `club(id)` |
| 동아리2 ID | club2_id | BIGINT | N | FK → `club(id)` |
| 이름 | name | VARCHAR(100) | N |  |
- 참여자는 join해서 가져오기

### 채팅메시지 Chat_Message

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 채팅메시지 ID | id | BIGSERIAL | N | PK |
| 채팅방 ID | chat_room_id | BIGINT | N | FK → `chat_room(id)` |
| 보낸 유저 ID | sender_id | BIGINT | N | FK → `club_member(id)` |
| 받은 유저 ID | receiver_id | BIGINT | N | FK → `club_member(id)` |
| 내용 | content | TEXT | N |  |
- 운영진(N):운영진(N)

---

폼 json

- 지원서 질문 컴포넌트

```json
{
	"key": "d89wjw7by16k2083"
  "question": "자기소개를 작성해주세요",
  "type": "textarea",
  "required": true,
  "max_length": 500
}
```

- 지원서(답변)

```json
[
	{
		"key":"d89wjw7by16k2083",
		"answer":"안녕"
	},
	{
		"key":"d89wjw7by16k2083",
		"answer":"안녕"
	},
]
```

- 공고

```json
{
	"questions": ["d89wjw7by16k2083", "d89wjw7by16k2083"]
}
```

---

### 알림톡 Talk

https://api.ncloud-docs.com/docs/ai-application-service-sens-alimtalkv2

- 관심 동아리에서 공고가 올라온 경우
- 지원 완료
- 지원 합격 / 불합격

각 경우에 대해 알림톡 템플릿? 내용 저장 → yml 파일에 정의. db에 저장은 X

![image.png](ERD/image.png)

---

# 추후

### 대학교

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 아이디 | id | BIGSERIAL | N | PK |
| 이름 | name | VARCHAR(50) | N |  |
| 지역 id | provider | VARCHAR(20) | N |  |

### 정산 Event_Settlement

| 논리명 | 물리명 | 타입 | Null | 제약조건 / 설명 |
| --- | --- | --- | --- | --- |
| 정산 ID | id | BIGSERIAL | N | PK |
| 활동 ID | event_id | BIGINT | N | FK → `event(id)` (ON DELETE CASCADE) |
| 정산 금액 | amount | BIGINT | N |  |
| 1인 부담금 | amount_per_person | BIGINT | N |  |
| 상태 | status | ENUM | N | INPROCESS, FINISHED |
| 시작 일시 | start_at | TIMESTAMPTZ | N |  |
| 종료 일시 | end_at | TIMESTAMPTZ | N | `end_at > start_at` CHECK 권장 |
- 종료 일시 이후에도 정산되지 않은 참가자는 평판 깎임?
    - ㅇㅇ
- 카카오톡 정산하기랑 연동 알아보기

- 질문 하나를 컴포넌트화 → 재사용
    - 폼 작성하고 저장할 때 기존에 없는 질문 컴포넌트면 저장
    - 유사 질문 추천 (추후)