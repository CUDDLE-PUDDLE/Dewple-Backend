# User API - 사용자

Base URL: `/users`

모든 엔드포인트는 JWT 인증이 필요합니다 (아이디 중복 확인 제외).

---

## 1. 내 프로필 조회

로그인한 사용자가 본인의 프로필 정보를 조회합니다.

### Request

```
GET /users/me
Authorization: Bearer {accessToken}
```

Body 없음

### Response (성공)

```
HTTP 200 OK
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다.",
  "result": {
    "name": "홍길동",
    "profileImg": "https://example.com/img.jpg",
    "nickname": "듀플러",
    "email": "user@example.com",
    "phone": "01012345678",
    "birthdate": "2000-01-01",
    "gender": "MALE",
    "university": "SEOUL_NATIONAL",
    "isGraduated": false,
    "workplace": "듀플 주식회사",
    "selfIntroduction": "안녕하세요!",
    "mbti": "INTJ",
    "interests": ["개발", "디자인"]
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | String | 이름 |
| `profileImg` | String | 프로필 이미지 URL (nullable) |
| `nickname` | String | 닉네임 (nullable) |
| `email` | String | 이메일 (nullable) |
| `phone` | String | 전화번호 |
| `birthdate` | String (ISO 8601) | 생년월일 (nullable) |
| `gender` | String | 성별 `MALE`, `FEMALE` (nullable) |
| `university` | String | 대학교 enum (nullable) |
| `isGraduated` | Boolean | 졸업 여부 (nullable) |
| `workplace` | String | 직장 (nullable) |
| `selfIntroduction` | String | 자기소개 (nullable) |
| `mbti` | String | MBTI (nullable) |
| `interests` | String[] | 관심 분야 목록 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |

---

## 2. 내 프로필 수정

로그인한 사용자가 본인의 프로필을 수정합니다. 전달된 필드만 변경됩니다 (partial update).

### Request

```
PATCH /users/me
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `nickname` | String | X | 닉네임 (2~20자) | `"듀플러"` |
| `email` | String | X | 이메일 | `"user@example.com"` |
| `birthdate` | String (ISO 8601) | X | 생년월일 | `"2000-01-01"` |
| `gender` | String | X | 성별 (`MALE`, `FEMALE`) | `"MALE"` |
| `university` | String | X | 대학교 (enum) | `"SEOUL_NATIONAL"` |
| `isGraduated` | Boolean | X | 졸업 여부 | `false` |
| `workplace` | String | X | 직장 (100자 이내) | `"듀플 주식회사"` |
| `profileImg` | String | X | 프로필 이미지 URL | `"https://example.com/img.jpg"` |
| `selfIntroduction` | String | X | 자기소개 (500자 이내) | `"안녕하세요!"` |
| `mbti` | String | X | MBTI | `"INTJ"` |
| `categoryIds` | Long[] | X | 관심 분야 카테고리 ID 목록 | `[1, 2]` |

```json
{
  "nickname": "새닉네임",
  "email": "new@example.com",
  "mbti": "ENFP",
  "categoryIds": [1, 3]
}
```

### Response (성공)

```
HTTP 200 OK
```

내 프로필 조회와 동일한 응답 형식입니다 (`GetMyProfileResponse`).

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4103 | 409 | 이미 사용 중인 닉네임입니다. | 닉네임 중복 |
| 4104 | 409 | 이미 사용 중인 이메일입니다. | 이메일 중복 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |
| 4400 | 404 | 존재하지 않는 카테고리입니다. | 유효하지 않은 카테고리 ID |

---

## 3. 회원 프로필 조회

특정 회원의 공개 프로필을 조회합니다.

### Request

```
GET /users/{id}
Authorization: Bearer {accessToken}
```

**Path Parameter**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `id` | Long | O | 조회할 회원의 ID (PK) |

### Response (성공)

```
HTTP 200 OK
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다.",
  "result": {
    "name": "홍길동",
    "profileImg": "https://example.com/img.jpg",
    "selfIntroduction": "안녕하세요!",
    "mbti": "INTJ",
    "interests": ["개발", "디자인"]
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `name` | String | 이름 |
| `profileImg` | String | 프로필 이미지 URL (nullable) |
| `selfIntroduction` | String | 자기소개 (nullable) |
| `mbti` | String | MBTI (nullable) |
| `interests` | String[] | 관심 분야 목록 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 회원 |

---

## 4. 전화번호 변경

인증된 새 전화번호로 변경합니다. 사전에 전화번호 인증(`POST /auth/verifications/phone` + 코드 확인)이 완료되어야 합니다.

### Request

```
PATCH /users/me/phone
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `verificationToken` | String | O | 전화번호 인증 완료 토큰 | `"vp_xxxxxxxx-xxxx-..."` |

```json
{
  "verificationToken": "vp_550e8400-e29b-41d4-a716-446655440000"
}
```

### Response (성공)

```
HTTP 200 OK
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다."
}
```

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4005 | 400 | 유효하지 않은 본인인증 토큰입니다. | 잘못된 인증 토큰 |
| 4006 | 400 | 본인인증 토큰이 만료되었습니다. | 인증 토큰 유효시간 초과 |
| 4101 | 409 | 이미 가입된 전화번호입니다. | 다른 계정에서 사용 중인 번호 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |
| 4401 | 400 | 현재 사용 중인 전화번호와 동일합니다. | 기존 번호와 동일 |

---

## 5. 회원 탈퇴

본인의 계정을 탈퇴(비활성화) 처리합니다. Soft Delete 방식으로 status를 `INACTIVE`로 변경합니다.

### Request

```
DELETE /users/me
Authorization: Bearer {accessToken}
```

Body 없음

### Response (성공)

```
HTTP 200 OK
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다."
}
```

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |
| 4203 | 403 | 비활성화된 계정입니다. | 이미 탈퇴 처리된 계정 |

---

## 6. 아이디 중복 확인

사용 가능한 아이디인지 확인합니다. 인증 불필요합니다.

### Request

```
GET /users/check-userid?userId={userId}
```

**Query Parameter**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `userId` | String | O | 확인할 아이디 | `"dewple123"` |

### Response (성공)

```
HTTP 200 OK
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다.",
  "result": {
    "isAvailable": true
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `isAvailable` | boolean | 사용 가능 여부 (`true`: 사용 가능, `false`: 이미 사용 중) |

---

## 7. 회원가입 시 프로필 설정

회원가입 후 선택 정보를 입력하여 프로필을 설정합니다. 건너뛰기 가능합니다.

### Request

```
PATCH /users/me/profile
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `nickname` | String | X | 닉네임 (2~20자) | `"듀플러"` |
| `email` | String | X | 이메일 | `"user@example.com"` |
| `birthdate` | String (ISO 8601) | X | 생년월일 | `"2000-01-01"` |
| `gender` | String | X | 성별 (`MALE`, `FEMALE`) | `"MALE"` |
| `university` | String | X | 대학교 (enum) | `"SEOUL_NATIONAL"` |
| `isGraduated` | Boolean | X | 졸업 여부 | `false` |
| `workplace` | String | X | 직장 (100자 이내) | `"듀플 주식회사"` |

```json
{
  "nickname": "듀플러",
  "email": "user@example.com",
  "birthdate": "2000-01-01",
  "gender": "MALE",
  "university": "SEOUL_NATIONAL",
  "isGraduated": false,
  "workplace": "듀플 주식회사"
}
```

### Response (성공)

```
HTTP 200 OK
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다.",
  "result": {
    "nickname": "듀플러",
    "email": "user@example.com"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `nickname` | String | 설정된 닉네임 |
| `email` | String | 설정된 이메일 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4103 | 409 | 이미 사용 중인 닉네임입니다. | 닉네임 중복 |
| 4104 | 409 | 이미 사용 중인 이메일입니다. | 이메일 중복 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |

---

## 공통 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 2000 | 400 | 유효하지 않은 요청입니다. | 요청 Validation 실패 |
| 3000 | 500 | 서버에서 오류가 발생하였습니다. | 서버 내부 오류 |
