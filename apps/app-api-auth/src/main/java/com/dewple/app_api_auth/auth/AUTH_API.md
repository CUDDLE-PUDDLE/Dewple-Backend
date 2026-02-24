# Auth API - 인증

Base URL: `/auth`

---

## 1. 전화번호 인증 코드 발송

SMS로 영숫자 대문자 6자리 인증 코드를 발송합니다.

### Request

```
POST /auth/verifications/phone
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `phoneNumber` | String | O | 전화번호 (하이픈 포함, `01x-xxxx-xxxx`) | `"010-1234-5678"` |
| `purpose` | String | O | 인증 목적 (`SIGN_UP`, `CHANGE_PHONE`, `GUEST_APPLICATION`) | `"SIGN_UP"` |

```json
{
  "phoneNumber": "010-1234-5678",
  "purpose": "SIGN_UP"
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
    "verificationId": "550e8400-e29b-41d4-a716-446655440000",
    "expiredAt": "2026-02-12T17:05:00+09:00",
    "resendAfterSeconds": 60
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `verificationId` | String (UUID) | 인증 요청 ID (코드 확인 시 사용) |
| `expiredAt` | String (ISO 8601) | 인증 코드 만료 시각 |
| `resendAfterSeconds` | int | 재발송 가능까지 남은 시간 (초) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 4008 | 429 | 인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. | 60초 이내 재요청 |
| 4009 | 500 | SMS 발송에 실패했습니다. | SMS 발송 실패 |

---

## 2. 이메일 인증 코드 발송

이메일로 영숫자 대문자 6자리 인증 코드를 발송합니다.

### Request

```
POST /auth/verifications/email
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `email` | String | O | 이메일 주소 | `"user@example.com"` |
| `purpose` | String | O | 인증 목적 (`SIGN_UP`, `CHANGE_PHONE`, `GUEST_APPLICATION`) | `"SIGN_UP"` |

```json
{
  "email": "user@example.com",
  "purpose": "SIGN_UP"
}
```

### Response (성공)

전화번호 인증 코드 발송과 동일한 응답 형식입니다.

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 4008 | 429 | 인증 요청이 너무 많습니다. 잠시 후 다시 시도해주세요. | 60초 이내 재요청 |
| 4010 | 500 | 이메일 발송에 실패했습니다. | 이메일 발송 실패 |

---

## 3. 인증 코드 확인

발송된 인증 코드를 확인하고 본인인증 토큰을 발급합니다. 채널(phone/email)에 무관하게 `verificationId`로 식별합니다.

### Request

```
POST /auth/verifications/{verificationId}/confirm
Content-Type: application/json
```

**Path Parameter**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `verificationId` | String (UUID) | O | 인증 코드 발송 시 받은 ID |

**Body**

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `code` | String | O | 인증 코드 (영숫자 대문자 6자리, `[A-Z0-9]{6}`) | `"A1B2C3"` |

```json
{
  "code": "A1B2C3"
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
    "verificationToken": "vp_550e8400-e29b-41d4-a716-446655440000"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `verificationToken` | String | 본인인증 완료 토큰 (회원가입 시 사용, 유효시간 10분) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 4001 | 400 | 인증 코드가 만료되었습니다. | 코드 유효시간 초과 |
| 4002 | 400 | 인증 코드가 일치하지 않습니다. | 잘못된 코드 입력 |
| 4003 | 404 | 인증 요청을 찾을 수 없습니다. | 존재하지 않는 verificationId |

---

## 4. 회원가입 (1단계)

필수 정보(이름, 아이디, 비밀번호)를 입력하여 계정을 생성하고 JWT를 헤더로 발급합니다.

### Request

```
POST /auth/signup
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `verificationToken` | String | O | 전화번호 본인인증 완료 토큰 | `"vp_xxxxxxxx-xxxx-..."` |
| `name` | String | O | 이름 (2~20자) | `"홍길동"` |
| `userId` | String | O | 아이디 (영문, 숫자, 밑줄 4~20자) | `"dewple123"` |
| `password` | String | O | 비밀번호 (영문+숫자+특수문자 8~20자) | `"Password1!"` |
| `passwordConfirm` | String | O | 비밀번호 확인 | `"Password1!"` |

```json
{
  "verificationToken": "vp_550e8400-e29b-41d4-a716-446655440000",
  "name": "홍길동",
  "userId": "dewple123",
  "password": "Password1!",
  "passwordConfirm": "Password1!"
}
```

### Response (성공)

```
HTTP 200 OK
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Authorization-Refresh: Bearer eyJhbGciOiJIUzI1NiIs...
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다."
}
```

**Response Headers**

| 헤더 | 설명 |
|------|------|
| `Authorization` | `Bearer {accessToken}` — JWT 액세스 토큰 (1시간) |
| `Authorization-Refresh` | `Bearer {refreshToken}` — JWT 리프레시 토큰 (7일) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 4005 | 400 | 유효하지 않은 본인인증 토큰입니다. | 존재하지 않는 본인인증 토큰 |
| 4006 | 400 | 본인인증 토큰이 만료되었습니다. | 본인인증 토큰 유효시간(10분) 초과 |
| 4101 | 409 | 이미 가입된 전화번호입니다. | 전화번호 중복 |
| 4102 | 409 | 이미 사용 중인 아이디입니다. | 아이디 중복 |
| 4106 | 400 | 비밀번호 확인이 일치하지 않습니다. | password ≠ passwordConfirm |

---

## 5. 로그인

아이디와 비밀번호로 로그인하고 JWT를 헤더로 발급합니다.

### Request

```
POST /auth/login
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `userId` | String | O | 아이디 | `"dewple123"` |
| `password` | String | O | 비밀번호 | `"Password1!"` |

```json
{
  "userId": "dewple123",
  "password": "Password1!"
}
```

### Response (성공)

```
HTTP 200 OK
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Authorization-Refresh: Bearer eyJhbGciOiJIUzI1NiIs...
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다."
}
```

**Response Headers**

| 헤더 | 설명 |
|------|------|
| `Authorization` | `Bearer {accessToken}` — JWT 액세스 토큰 (1시간) |
| `Authorization-Refresh` | `Bearer {refreshToken}` — JWT 리프레시 토큰 (7일) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 아이디 |
| 4202 | 401 | 비밀번호가 일치하지 않습니다. | 비밀번호 불일치 |
| 4203 | 403 | 비활성화된 계정입니다. | 비활성화된 계정 |

---

## 6. 로그아웃

해당 사용자의 모든 리프레시 토큰을 삭제합니다.

### Request

```
POST /auth/logout
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

---

## 7. 토큰 재발급 (RTR)

리프레시 토큰으로 새로운 액세스 토큰과 리프레시 토큰을 발급합니다. 사용된 리프레시 토큰은 즉시 폐기됩니다 (Refresh Token Rotation).

### Request

```
POST /auth/token/refresh
Content-Type: application/json
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `refreshToken` | String | O | 리프레시 토큰 (Bearer 접두사 없이) | `"eyJhbGciOiJIUzI1NiIs..."` |

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

### Response (성공)

```
HTTP 200 OK
Authorization: Bearer eyJhbGciOiJIUzI1NiIs...
Authorization-Refresh: Bearer eyJhbGciOiJIUzI1NiIs...
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다."
}
```

**Response Headers**

| 헤더 | 설명 |
|------|------|
| `Authorization` | `Bearer {accessToken}` — 새 JWT 액세스 토큰 (1시간) |
| `Authorization-Refresh` | `Bearer {refreshToken}` — 새 JWT 리프레시 토큰 (7일) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 4301 | 401 | 유효하지 않은 리프레시 토큰입니다. | 잘못된 토큰 타입, 이미 사용된 토큰 |
| 4302 | 401 | 리프레시 토큰이 만료되었습니다. | 리프레시 토큰 유효기간 초과 |

---

## 8. 아이디 중복 확인

사용 가능한 아이디인지 확인합니다.

### Request

```
GET /auth/check-userid?userId={userId}
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

## 9. 프로필 설정 (2단계)

선택 정보를 입력하여 프로필을 설정합니다. 건너뛰기 가능합니다.

### Request

```
PATCH /auth/signup/profile
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
| `isGraduated` | boolean | X | 졸업 여부 | `false` |
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
