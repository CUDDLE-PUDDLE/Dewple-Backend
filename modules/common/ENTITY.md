[ERD 테이블 구조]
⸻

엔티티 공통 컬럼

대부분의 테이블에 공통으로 존재(또는 정책적으로 포함)하는 컬럼입니다.
	•	created_at (TIMESTAMPTZ, NOT NULL): 생성일시
	•	updated_at (TIMESTAMPTZ, NOT NULL): 수정일시
	•	status (VARCHAR(20), NOT NULL): 기본 ACTIVE, INACTIVE

⸻

User (회원)

사용자 계정/프로필의 마스터 엔티티 (전화번호 유니크, 여러 로그인 수단 연동 등 기능 명세 반영)
	•	PK: id (BIGSERIAL)
	•	user_id (VARCHAR(255), NULL): 실제 유저 구분용 ID(기능 명세에서 “실제 유저 구분은 user_id”)
	•	password (VARCHAR(255), NULL): 로컬 로그인 시
	•	profile_img (VARCHAR(255), NOT NULL)
	•	name (VARCHAR(255), NOT NULL)
	•	nickname (VARCHAR(255), NOT NULL)
	•	birthdate (DATE, NOT NULL)
	•	gender (VARCHAR(20), NOT NULL): MALE, FEMALE
	•	email (VARCHAR(50), NOT NULL)
	•	university (ENUM, NULL): 변경 가능(명세에 “바꿀 수 있음”)
	•	is_graduated (BOOLEAN, NULL)
	•	workplace (VARCHAR(100), NULL)
	•	phone (VARCHAR(20), NOT NULL, UNIQUE): 메인 유니크 키
	•	plan (VARCHAR(20), NOT NULL, DEFAULT FREE)
	•	self_introduction (TEXT, NOT NULL)
	•	mbti (ENUM, NOT NULL)
	•	reputation_score (DECIMAL(1,1), NULL, DEFAULT 5.0)

	•	관계
	•	1:N Social_Account (소셜 계정)
	•	N:M Category (관심사) via User_Category
	•	N:M Club (가입/운영) via Club_Member
	•	N:M Club (관심 동아리) via Club_Interest
	•	1:N Application (지원서 제출자)
	•	1:N Notice (작성자)
	•	1:N Activity (활동 생성자)
	•	N:M Activity (활동 참가자) via Activity_Participant

⸻

User_Category (회원-카테고리 / 관심사 매핑)

회원 관심사 N:M 매핑 테이블
	•	PK: id (BIGSERIAL)

	•	FK
	•	user_id (BIGINT, NOT NULL) → user(id)
	•	category_id (BIGINT, NOT NULL) → category(id)

	•	비고: “회원-카테고리 다대다 매핑”

⸻

Social_Account (소셜 계정)

하나의 유저에 여러 로그인 수단을 연결 (카카오/네이버/구글 등)
	•	PK: id (BIGSERIAL)
	•	FK: user_id (BIGINT, NOT NULL) → user(id)

	•	주요 컬럼
	•	provider (VARCHAR(20), NOT NULL): KAKAO, NAVER, GOOGLE
	•	provider_id (VARCHAR(100), NOT NULL, UNIQUE): 제공자 유저 식별자

⸻

Club (동아리)

동아리 기본 정보/설정의 마스터 엔티티
	•	PK: id (BIGSERIAL)
	•	FK: creator_id (BIGINT, NOT NULL) → user(id)

	•	주요 컬럼
	•	university (ENUM, NULL): 교내/연합/확장성 관련 메모 존재(향후 분리/확장 여지)
	•	name (VARCHAR(100), NOT NULL)
	•	description (TEXT, NULL)
	•	cover_img (VARCHAR(2048), NULL)
	•	landing_page (JSONB, NULL): 소개페이지 컴포넌트 배열 형태
	•	reputation_score (DECIMAL(1,2), NOT NULL, DEFAULT 5.0)
	•	gender (VARCHAR(20), NOT NULL): MALE, FEMALE (명세상 무관 값은 기능에서 언급되나 ERD엔 MALE/FEMALE만 표기)
	•	min_age (BIGINT, NULL), max_age (BIGINT, NULL)
	•	activity_type (ENUM, NOT NULL): ONLINE, OFFLINE, BOTH (DEFAULT BOTH)

	•	관계
	•	1:N Club_Member (동아리 구성원)
	•	1:N Club_Role (동아리 역할/권한)
	•	1:N Club_Generation (기수)
	•	1:N Club_Department (부서)
	•	N:M Category via Club_Category (태그/카테고리)
	•	N:M Region via Club_Region (활동 지역)
	•	1:N Recruitment_Posting (모집 공고)
	•	1:N Notice (공지)
	•	1:N Activity (동아리 활동)
	•	N:M User via Club_Interest (관심 동아리)
	•	1:N Club_Inquiry (문의)

⸻

Club_Department (동아리-부서)

동아리 내부 부서(팀/포지션) 정의
	•	PK: id (BIGSERIAL)
	•	FK: club_id (BIGINT, NOT NULL) → club(id)
	•	주요 컬럼: name (VARCHAR(100), NOT NULL)

⸻

Club_Category (동아리-카테고리 매핑)

동아리 태그/카테고리 N:M 매핑
	•	PK: id (BIGSERIAL)

	•	FK
	•	club_id (BIGINT, NOT NULL) → club(id)
	•	category_id (BIGINT, NOT NULL) → category(id)

⸻

Club_Region (동아리-지역 매핑)

동아리 활동 지역 N:M 매핑
	•	PK: id (BIGSERIAL)

	•	FK
	•	club_id (BIGINT, NOT NULL) → club(id)
	•	region_id (BIGINT, NOT NULL) → region(id)

⸻

Club_Inquiry (동아리 문의)

동아리 대상 익명 문의/답글 구조(부모-자식) 지원
	•	PK: id (BIGSERIAL)

	•	FK
	•	club_id (BIGINT, NOT NULL)
	•	creator_id (BIGINT, NOT NULL) → user(id)
	•	parent_id (BIGINT, NULL) → inquiry(id) (문서 표기 기준: inquiry로 되어 있으나 엔티티명은 club_inquiry로 보입니다)
	•	주요 컬럼
	•	anonymity (VARCHAR(50), NOT NULL): 익명 닉네임
	•	content (TEXT, NOT NULL)

⸻

Club_Role (동아리 역할)

동아리 내 역할 + 권한 비트마스킹
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (club_id, name)

	•	FK: club_id (BIGINT, NOT NULL) → club(id) (ON DELETE CASCADE)

	•	주요 컬럼
	•	name (VARCHAR(50), NOT NULL)
	•	permissions (BIGINT, NOT NULL): 권한 비트마스킹

	•	권한(문서 정의)
	•	소개/태그 변경
	•	활동 생성/수정
	•	동아리 네트워킹(타 동아리 채팅)
	•	문의 답변
	•	지원서(모집) 제작/수정/열람
	•	합격 여부 결정
	•	회원 관리(역할 부여/초대/퇴출 등)

⸻

Club_Generation (동아리 기수)

동아리 기수 정보(예: 1기/2기…)
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (club_id, generation_no)

	•	FK: club_id (BIGINT, NOT NULL) → club(id) (ON DELETE CASCADE)

	•	주요 컬럼
	•	generation_no (INT, NOT NULL)
	•	start_date (DATE, NOT NULL)
	•	end_date (DATE, NULL): 기수 구분 없으면 NULL 허용

⸻

Club_Member (동아리-회원)

동아리 가입/활동 상태 + 운영진 역할 연결
	•	PK: id (BIGSERIAL)

	•	제약
	•	UNIQUE (club_id, user_id)
	•	CHECK: member_level='STAFF' 인 경우 role_id IS NOT NULL 강제 (문서에 member_level 컬럼 표기는 없지만 제약으로 존재)

	•	FK (club_id, role_id) → club_role (club_id, id) (문서 제약 설명)

	•	FK
	•	club_id (BIGINT, NOT NULL) → club(id) (ON DELETE CASCADE 권장)
	•	user_id (BIGINT, NOT NULL) → user(id)
	•	role_id (BIGINT, NULL) → club_role(id) (운영진일 때 권장)
	•	join_generation_id (BIGINT, NULL) → club_generation(id)

	•	주요 컬럼
	•	activity_status (VARCHAR(20), NOT NULL): ACTIVE, DORMANT, GRADUATED, KICKEDOUT, LEFT / DEFAULT ACTIVE

⸻

Member_Generation (멤버-기수)

멤버가 어떤 기수로 활동했는지 매핑(이력성 목적)
	•	PK: id (BIGSERIAL)

	•	FK
	•	member_id (BIGINT, NOT NULL) → user(id) (문서 표기 기준)
	•	generation_id (BIGINT, NOT NULL) → category(id) (문서 표기상 category로 되어 있어 혼동 여지가 있습니다. 의도는 club_generation일 가능성이 높습니다.)

	•	비고: ERD에 “TODO/정합성”류 메모가 일부 존재

⸻

Member_Department (멤버-동아리부서)

멤버의 부서 소속 매핑
	•	PK: id (BIGSERIAL)

	•	FK
	•	member_id (BIGINT, NOT NULL) → user(id)
	•	department_id (BIGINT, NOT NULL) → category(id) (문서 표기상 category로 되어 있으나 실제는 club_department일 가능성이 높습니다)

	•	비고: “멤버의 동아리와 부서의 동아리 통일 제약 TODO”

⸻

Club_Interest (관심 동아리)

유저가 하트를 누른 ‘관심 동아리’ 매핑
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (club_id, user_id)

	•	FK
	•	club_id (BIGINT, NOT NULL) → club(id) (ON DELETE CASCADE 권장)
	•	user_id (BIGINT, NOT NULL) → user(id)

⸻

Recruitment_Posting (모집 공고)

동아리 모집 공고 본문/기간/상태/버전 포인터
	•	PK: id (BIGSERIAL)

	•	FK
	•	club_id (BIGINT, NULL) → club(id) (ON DELETE CASCADE)
	•	activity_id (BIGINT, NULL)
	•	generation_id (BIGINT, NULL) → generation(id) (문서 표기 기준: generation 테이블명은 club_generation과 불일치 가능)
	•	creator_id (BIGINT, NOT NULL) → club_member(id)

	•	주요 컬럼
	•	title (VARCHAR(100), NOT NULL)
	•	description (TEXT, NULL)
	•	theme_color (VARCHAR(100), NULL)
	•	edit_window_basis (VARCHAR(20), NOT NULL): DEPLOYED, SUBMITTED
	•	edit_window_days (INT, NOT NULL)
	•	capacity (INT, NULL): 제한 없으면 NULL 또는 큰 값
	•	status (VARCHAR(20), NOT NULL): DRAFT, OPEN, CLOSED, ARCHIVED 등
	•	recent_recruitment_version (BIGINT, NOT NULL): 최신 지원 양식 버전 포인터
	•	start_at (TIMESTAMPTZ, NOT NULL)
	•	end_at (TIMESTAMPTZ, NOT NULL), CHECK end_at > start_at

⸻

Recruitment_Process (지원 절차)

모집 공고의 단계(서류/면접/최종 등) 정의
	•	PK: id (BIGSERIAL)

	•	FK: posting_id (BIGINT, NOT NULL) → posting(id) (ON DELETE CASCADE)

	•	주요 컬럼
	•	order (INT, NOT NULL): 단계 순서
	•	name (VARCHAR(50), NOT NULL)
	•	description (TEXT, NULL)
	•	process_type (VARCHAR(100), NOT NULL): DOCUMENT, INTERVIEW, FINAL …
	•	start_at/end_at (TIMESTAMPTZ, NULL), CHECK end_at > start_at 권장

⸻

Recruitment_Schema (지원 양식 스키마/버전)

특정 절차에 대한 ‘지원서 폼’ 버전 저장
	•	PK: id (BIGSERIAL)

	•	FK: recruitment_process_id (BIGINT, NOT NULL) → recruitment_process(id) (ON DELETE CASCADE)

	•	주요 컬럼
	•	version (BIGINT, NOT NULL)
	•	application_form (JSONB, NULL): 공고 시점 폼 스냅샷(선택). 질문 컴포넌트를 주로 쓰면 생략 가능

⸻

Recruitment_Department (공고-부서)

모집 공고에서 모집하는 부서(포지션) 매핑
	•	PK: id (BIGSERIAL)

	•	FK
	•	recruitment_id (BIGINT, NOT NULL) → recruitment(id)
	•	department_id (VARCHAR(100), NOT NULL) → department(id)

	•	비고: 문서 내 테이블명/타겟 테이블명이 club_department와 다르게 표기되어 있어, 실제 구현 시 명칭 정리가 필요합니다.

⸻

Form_Component (질문 컴포넌트)

지원서 질문을 컴포넌트화하여 재사용(부모-자식 조건부 노출 포함)
	•	PK: id (BIGSERIAL)

	•	제약
	•	UNIQUE (posting_id, component_key) (문서 표기상 posting_id이나 컬럼 리스트에는 club_id가 등장하여 불일치 가능)
	•	INDEX (posting_id, sort_order) (동일)

	•	FK
	•	club_id (BIGINT, NOT NULL) → club(id) (ON DELETE CASCADE)
	•	department_id (BIGINT, NULL) → department(id)
	•	parent_id (BIGINT, NULL) → form_component(id)

	•	주요 컬럼
	•	parent_select_number (BIGINT, NULL): 부모 컴포넌트의 특정 선택지 번호일 때 노출
	•	component_key (VARCHAR(32), NOT NULL): 공고 내 유니크 키
	•	question (TEXT, NOT NULL)
	•	input_type (VARCHAR(30), NOT NULL): textarea 등 (ENUM/체크 권장)
	•	required (BOOLEAN, NOT NULL, DEFAULT false)
	•	max_length (INT, NULL)
	•	config (JSONB, NULL): options, placeholder 등 확장
	•	sort_order (INT, NOT NULL, DEFAULT 0)

⸻

Application (지원서)

지원자가 제출(또는 임시저장)한 응답 저장
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (posting_id, applicant_id) (“지원은 1회/최신 1건만 유지” 모델)

	•	FK
	•	recruitment_schema_id (BIGINT, NOT NULL) → recruitment_schema(id) (ON DELETE CASCADE)
	•	applicant_id (BIGINT, NOT NULL) → user(id)

	•	주요 컬럼
	•	answers (JSONB, NOT NULL, DEFAULT '[]'::jsonb): [{key, answer}]
	•	interview_start_date (TIMESTAMP, NULL)
	•	interview_location (VARCHAR(255), NULL)
	•	status (VARCHAR(20), NOT NULL): SUBMITTED, ACCEPTED, REJECTED, TEMPORARY, FIX

⸻

Category (카테고리)

관심 분야/태그용 계층 구조 카테고리
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (name)

	•	FK: parent_id (BIGSERIAL, NULL) → category(id)

	•	주요 컬럼: name (VARCHAR(50), NOT NULL)

⸻

Region (지역)

지역 계층 구조(대분류/소분류) + 무관 포함 가능
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (city, name) (문서에 city가 언급되나 컬럼 리스트에는 미표기)

	•	FK: parent_id (BIGSERIAL, NULL) → region(id)

	•	주요 컬럼: name (VARCHAR(50), NOT NULL) 예: 신촌, 혜화, 회기 등

⸻

Notice (공지)

동아리 공지 + 댓글(부모-자식) 구조
	•	PK: id (BIGSERIAL)

	•	FK
	•	club_id (BIGINT, NOT NULL) → club(id) (ON DELETE CASCADE 권장)
	•	creator_id (BIGINT, NOT NULL) → user(id)
	•	parent_id (BIGINT, NULL): 댓글인 경우 부모 공지 id

	•	주요 컬럼
	•	name (VARCHAR(100), NULL): 제목
	•	content (TEXT, NOT NULL)

⸻

Vote (투표)

공지에 붙는 투표(익명/다중선택/수정가능/기간)
	•	PK: id (BIGSERIAL)

	•	FK
	•	notice_id (BIGINT, NOT NULL) → notice(id)
	•	creator_id (BIGINT, NOT NULL) → user(id)

	•	주요 컬럼
	•	name (VARCHAR(100), NULL)
	•	description (TEXT, NULL)
	•	is_anonymous (BOOLEAN, NOT NULL)
	•	is_multi (BOOLEAN, NOT NULL)
	•	is_modifiable (BOOLEAN, NOT NULL)
	•	start_at (TIMESTAMPTZ, NOT NULL)
	•	end_at (TIMESTAMPTZ, NOT NULL), CHECK end_at > start_at 권장

⸻

Vote_Component (투표 항목)

투표의 선택지(항목) 엔티티
	•	PK: id (BIGSERIAL)

	•	FK: vote_id (BIGINT, NOT NULL) → vote(id)

	•	주요 컬럼: content (TEXT, NOT NULL)

⸻

Vote_Participant (투표 참여자)

누가 어떤 항목에 투표했는지(선택 결과)
	•	PK: id (BIGSERIAL)

	•	FK
	•	member_id (BIGINT, NOT NULL) → club_member(id)
	•	vote_component_id (BIGINT, NOT NULL) → vote_component(id)

⸻

Activity (활동/소모임)

동아리 활동(또는 확장 시 개인/연합회 활동) 기본 엔티티
	•	PK: id (BIGSERIAL)

	•	FK
	•	club_id (BIGINT, NULL) → club(id) (ON DELETE CASCADE 권장)
	•	creator_id (BIGINT, NOT NULL) → user(id)

	•	주요 컬럼
	•	open_type (VARCHAR(30), NOT NULL): PUBLIC, PRIVATE 권장
	•	name (VARCHAR(100), NOT NULL)
	•	start_at/end_at (TIMESTAMPTZ, NOT NULL), CHECK end_at > start_at 권장

	•	확장 메모
	•	dtype별 상세(투표/회식/공연/번개 등)는 activity_vote, activity_dining 같은 1:1 서브테이블로 확장 가능

⸻

Activity_Participant (활동-참여자)

활동 참가자 매핑 + 정산 완료 여부
	•	PK: id (BIGSERIAL)

	•	제약: UNIQUE (activity_id, participant_id)

	•	FK
	•	activity_id (BIGINT, NOT NULL) → activity(id) (ON DELETE CASCADE)
	•	participant_id (BIGINT, NOT NULL) → user(id)

	•	주요 컬럼
	•	is_settlement_completed (BOOLEAN, NULL): 정산이 없으면 NULL

⸻

Chat_Room (채팅방)

동아리 간 네트워킹 채팅방(동아리 2개 연결)
	•	PK: id (BIGSERIAL)

	•	FK
	•	club1_id (BIGINT, NOT NULL) → club(id)
	•	club2_id (BIGINT, NOT NULL) → club(id)

	•	주요 컬럼: name (VARCHAR(100), NOT NULL)

	•	비고: “참여자는 join해서 가져오기”(별도 참가자 테이블 가능성)

⸻

Chat_Message (채팅 메시지)

채팅방 내 메시지(송/수신자 club_member 기준)
	•	PK: id (BIGSERIAL)

	•	FK
	•	chat_room_id (BIGINT, NOT NULL) → chat_room(id)
	•	sender_id (BIGINT, NOT NULL) → club_member(id)
	•	receiver_id (BIGINT, NOT NULL) → club_member(id)

	•	주요 컬럼: content (TEXT, NOT NULL)

⸻

[BaseEntity]

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
	
	@CreatedDate
	@Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime createdAt;
	
	@LastModifiedDate
	@Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
	private OffsetDateTime updatedAt;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	privateBaseStatusstatus= BaseStatus.ACTIVE;
	
	publicvoidinactivate() {
	this.status = BaseStatus.INACTIVE;
	    }
	
	public void activate() {
	this.status = BaseStatus.ACTIVE;
	    }
}

[BaseStatus]
public enum BaseStatus {
    ACTIVE,
    INACTIVE
}

[지시]
위 ERD 구조에 맞는 JPA 엔티티를 /src/modules/common/entity/ 내에 각각 별도의 파일로 생성해줘. 각 클래스의 이름은 대문자로 시작하고 카멜케이스를 따르며, 테이블명은 모두 소문자고 스네이크 케이스를 따라. VARCHAR 타입이며 값이 정해져 있는 컬럼들의 타입은 별도의 파일에서 enum 타입으로 분리해주고, 모든 엔티니는 BaseEntity를 상속해.