# CLAUDE.md — dewple-backend

## 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.9 |
| Build Tool | Gradle (Wrapper) | 8.14.3 |
| Database | PostgreSQL (Supabase) | 13+ |
| ORM | Spring Data JPA + Hibernate | - |
| Security | Spring Security + OAuth2 (Authorization/Resource Server) | 6.x |
| API Docs | springdoc-openapi (Swagger) | 2.8.15 |
| Cloud | Spring Cloud AWS (SQS, SES) | 3.4.2 |
| SMS | SOLAPI | 4.2.7 |
| Query | QueryDSL | 5.1.0 |
| Testing | JUnit 5 + Mockito + AssertJ | 5.11.4 |

## 프로젝트 구조

멀티 모듈 Gradle 프로젝트. Layered Architecture + Hexagonal(Port-Adapter) 패턴 혼용.

```
dewple-backend/
├── apps/                              # 실행 가능한 Spring Boot 애플리케이션
│   ├── app-api-auth/                  # 인증 & REST API 서버 (port 8080)
│   └── app-worker/                    # 비동기 워커 (SQS, WebSocket, SES)
│
└── modules/                           # 공유 도메인 모듈 (java-library)
    ├── common/                        # BaseEntity, 공통 enum, 예외, 유틸
    ├── user/                          # 사용자 도메인
    ├── club/                          # 동아리 도메인
    ├── activity/                      # 활동 도메인
    ├── organization/                  # 조직 도메인
    └── recruitment/                   # 모집 도메인
```

### app-api-auth 내부 구조

`auth`(인증)와 `api`(비즈니스)를 패키지 수준에서 분리한다.

- **auth**: 신원 확인, 토큰 발급/검증/폐기 (login, logout, signup, token refresh, verifications)
- **api**: 비즈니스 리소스 CRUD (users, clubs, activities 등)

```
app-api-auth/src/main/java/com/dewple/app_api_auth/
├── auth/                      # 인증 도메인
│   ├── controller/            #   AuthController (/auth/**)
│   └── dto/
├── api/                       # 비즈니스 API 도메인
│   ├── user/                  #   UserController (/users/**)
│   │   ├── controller/
│   │   └── dto/
│   ├── club/
│   ├── activity/
│   └── federation/
├── infra/                     # Port 구현체 (어댑터)
│   ├── sms/                   #   SmsVerificationAdapter (SOLAPI)
│   ├── email/                 #   EmailVerificationAdapter (AWS SES)
│   └── security/              #   PasswordEncoderAdapter
└── global/                    # 횡단 관심사
    ├── config/                #   SecurityConfig, JwtConfig, SwaggerConfig 등
    ├── exception/             #   GlobalExceptionHandler, WebErrorCode
    ├── response/              #   ApiResponse
    └── security/              #   JwtTokenProvider
```

### 모듈 내부 구조 (도메인 모듈 공통)

```
modules/{domain}/
├── entity/          # JPA 엔티티 (BaseEntity 상속)
├── service/         # 비즈니스 로직
├── repository/      # Spring Data JPA + QueryDSL 리포지토리
├── port/            # 외부 연동 인터페이스 (Port)
└── exception/       # 도메인별 ErrorCode enum
```

## 빌드 & 실행

```bash
# 전체 빌드
./gradlew clean build

# 테스트
./gradlew test

# API 서버 실행 (로컬)
./gradlew :apps:app-api-auth:bootRun

# 워커 실행
./gradlew :apps:app-worker:bootRun
```

## 코딩 컨벤션

### 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스 | PascalCase | `VerificationService`, `AuthController` |
| 메서드/필드 | camelCase | `sendPhoneVerificationCode()`, `verificationCode` |
| 상수 | UPPER_SNAKE_CASE | `REQUEST_COOLDOWN_SECONDS`, `CODE_LENGTH` |
| 패키지 | lowercase | `com.dewple.user.service` |
| DTO | PascalCase + 접미사 | `SendVerificationCodeRequest`, `ConfirmVerificationCodeResponse` |
| Enum 값 | UPPER_SNAKE_CASE | `ACTIVE`, `INACTIVE`, `PHONE` |
| Boolean 필드 | is/has 접두사 | `isVerified`, `isExpired` |

### 엔티티 패턴

- 모든 엔티티는 `BaseEntity` 상속 (`createdAt`, `updatedAt`, `status` 자동 관리)
- `@Getter` + `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 조합
- 생성자에 `@Builder` 적용
- JSONB 컬럼: `@JdbcTypeCode(SqlTypes.JSON)` + `@Column(columnDefinition = "jsonb")`

### 서비스 패턴

- `@Slf4j` + `@Service` + `@RequiredArgsConstructor`
- 쓰기 작업: `@Transactional`, 읽기 작업: `@Transactional(readOnly = true)`

### DTO 패턴

- Java `record` 사용
- Jakarta Validation 어노테이션 (`@NotBlank`, `@NotNull`, `@Pattern`)
- OpenAPI `@Schema` 어노테이션으로 문서화

### API 응답

- `ApiResponse<T>` 래퍼 사용 (code, status, message, result)
- 성공: `ApiResponse.ok(result)` (code: 1000)
- 에러: `ApiResponse.error(errorCode)`

### 예외 처리

- `ErrorCode` 인터페이스 → 계층별 enum 구현
  - 웹 에러코드: `WebErrorCode` (apps 모듈, 1000~3999) — 성공/요청오류/서버오류
  - 도메인 에러코드: `UserErrorCode` 등 (각 도메인 모듈, 4000+)
- 에러 발생 시 `throw new BusinessException(ErrorCode)` 으로 던지기
- `@RestControllerAdvice` `GlobalExceptionHandler`에서 `BusinessException`을 잡아 `ApiResponse.error()` 변환
- 에러코드 범위
  - 1000: 성공
  - 2000~2999: 클라이언트 오류
  - 3000~3999: 서버 오류
  - 4000~4099: 본인인증 (verification)
  - 4100~4199: 회원가입 (signup)
  - 4200~4299: 로그인 (login)
  - 4300~4399: 토큰 (refresh token)

### 테스트 패턴

- 컨트롤러: `@WebMvcTest` + `MockMvc` + `@MockitoBean`
- 서비스: `@ExtendWith(MockitoExtension.class)` + `@Mock` + `@InjectMocks`
- `@Nested` + `@DisplayName`으로 계층적 테스트 구성
- Given-When-Then 패턴
- 한글 `@DisplayName` 사용 (예: `"성공: 인증 코드 발송"`)

### Repository 패턴

- 단순 조회: Spring Data JPA 메서드 쿼리 사용
- 복잡한 조회: JPQL 대신 QueryDSL 사용 (`RepositoryCustom` + `RepositoryImpl`)
- `JPAQueryFactory` 빈은 `modules/common` 설정(`QueryDslConfig`)에서 등록

### Port-Adapter 패턴

- Port: `modules/{domain}/port/`에 인터페이스 정의 (예: `SmsVerificationPort`, `EmailVerificationPort`, `PasswordEncoderPort`)
- Adapter: `apps/app-api-auth/infra/`에 구현체 (예: `SmsVerificationAdapter`, `EmailVerificationAdapter`)
- 외부 서비스 설정 누락 시 graceful degradation — 로그 경고 후 발송 스킵

### 인증/보안 아키텍처

- JWT: HMAC-SHA256 (HS256), access token + refresh token
- 토큰 전달: 응답 헤더 (`Authorization`, `Authorization-Refresh`)
- RTR (Refresh Token Rotation): refresh 사용 시마다 DB에서 교체
- `RefreshToken` 엔티티로 DB 관리 (멀티 디바이스 동시 로그인 지원)
- `SecurityConfig`에서 인증 불필요 경로를 `permitAll`로 명시 등록
- **API 인증 규칙**: `/auth/**` 로 시작하는 엔드포인트(로그아웃 제외)와 일부 공개 엔드포인트(`/users/check-userid`)를 제외한 모든 API는 요청 헤더에 `Authorization: Bearer {accessToken}`이 필수
- **Swagger 인증 표시**: 인증이 필요한 엔드포인트에는 `@SecurityRequirement(name = BEARER_AUTH)` 어노테이션을 반드시 추가

### 코드 스타일

- 들여쓰기: 4 spaces
- 중괄호: K&R 스타일 (같은 줄에 열기)
- 어노테이션: 한 줄에 하나씩, 선언부 위에 배치

## 설정 파일

- `application.yaml`: 프로필 그룹 정의 (local, dev)
- `application-local.yaml`: 로컬 환경 설정 (`.gitignore` 대상)
- `application-dev.yaml`: 개발 환경 설정 (`.gitignore` 대상)
- 프로필 단위: `Port`, `RDB`, `JPA`, `Aws`, `Secret`, `SMS`, `SES`

## Git 규칙

- 파일을 생성하거나 수정한 후, `.gitignore` 규칙에 해당하지 않는 파일은 즉시 `git add <파일경로>`로 스테이징할 것
