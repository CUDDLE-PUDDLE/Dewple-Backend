# Dewple Server

> 동아리 · 모임 · 연합회를 하나로 잇는 커뮤니티 플랫폼

## 🫧 소개

**Dewple**은 동아리 모집 · 운영부터 연합회를 통한 소속 동아리 통합 운영까지, **하나의 플랫폼**에서 해결할 수 있는 서비스입니다.

대학교 · 기업 · 지역사회 등 다양한 커뮤니티가 더 쉽고 체계적으로 활동할 수 있도록 돕습니다.

---

## ✨ 주요 기능

| 도메인 | 설명 |
|--------|------|
| 🏠 **동아리** | 생성 · 모집 공고 · 지원서 · 합격 처리 · 역할/권한 관리 |
| 🎯 **모임** | 개인/동아리/연합회 단위 모임 생성, **선착순 대기열**, 출석체크 |
| 🏛️ **연합회** | 소속 동아리 통합 운영, 역할/권한 관리, **대표 위임** |
| 👤 **회원** | 소셜/자체 로그인, 프로필 관리, **상호 별점 평가** |

---

## 🛠️ 기술 스택

| 구분 | 기술 |
|------|------|
| Language | **Java 21** |
| Framework | **Spring Boot 3.5.9** |
| Database | **PostgreSQL** (Supabase) |
| Auth | **JWT** (HS256) + **RTR** (Refresh Token Rotation) |
| Cloud | **AWS** SQS · SES · S3 |
| SMS | **SOLAPI** |
| CI/CD | **GitHub Actions** + **Docker** |

---

## 📁 프로젝트 구조

**멀티 모듈** Gradle 프로젝트. Layered Architecture + **Hexagonal(Port-Adapter)** 패턴.

```
dewple-backend/
├── apps/
│   ├── app-api-auth/          # 인증 & REST API 서버
│   └── app-worker/            # 비동기 워커 (스케줄러, SQS)
│
├── modules/
│   ├── common/                # BaseEntity, 공통 enum, 예외, QueryDSL 설정
│   ├── user/                  # 사용자 도메인
│   ├── club/                  # 동아리 도메인
│   ├── activity/              # 모임 도메인
│   ├── organization/          # 연합회 도메인
│   └── recruitment/           # 모집 도메인
│
└── docs/                      # 기능 명세서, 도메인별 기능 목록
```

---

## 🏗️ 아키텍처

### 실행 구조

- **app-api-auth** — 인증(JWT 발급, 소셜 로그인)과 REST API를 하나의 서버에서 처리 (향후 **인증 서버 별도 분리** 예정)
- **app-worker** — 모임 자동취소/종료, 유저 하드삭제 등 비동기 작업 전담

### 도메인 모듈

- 비즈니스 로직은 **도메인 모듈**(user, club, activity, organization, recruitment)에 독립적으로 구성
- 외부 연동(SMS · Email · S3)은 도메인에 **Port** 인터페이스만 정의하고, 앱 모듈에서 **Adapter**로 구현
- 인증은 **JWT(HS256)** 기반, refresh token은 사용 시마다 교체하는 **RTR** 방식
- 동아리(18종)와 연합회(14종) 권한을 **비트마스크**로 관리하며, 승인 시 기본 역할 자동 생성

> ❗️ 현재 개발 중이며, 구조와 기능이 지속적으로 업데이트됩니다.
