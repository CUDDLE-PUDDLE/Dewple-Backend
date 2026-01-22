# Dewple Backend

Spring Boot 기반 멀티 모듈 백엔드 프로젝트

## 프로젝트 구조
```
dewple-backend/
├── apps/
│   ├── app-api-auth/      # 인증/API 서버
│   └── app-worker/        # 워커 애플리케이션
└── modules/
    ├── common/            # 공통 모듈
    ├── security-common/   # 보안 공통
    ├── notification-client/ # 알림 클라이언트
    └── auth-server/       # 인증 서버
```

## 기술 스택

- Java 21
- Spring Boot 3.5.9
- Spring Security
- Spring Data JPA
- PostgreSQL
- AWS SQS
- Flyway
