# Activity API - 모임

Base URL: `/activities`

모든 엔드포인트는 JWT 인증이 필요합니다.

---

## 1. 모임 생성

새로운 모임을 생성합니다. 개인 모임 또는 동아리 모임을 생성할 수 있습니다.

- **개인 모임**: `clubId`를 전달하지 않으면 개인 모임으로 생성됩니다.
- **동아리 모임**: `clubId`를 전달하면 해당 동아리의 모임으로 생성됩니다. 동아리 멤버이면서 `MANAGE_ACTIVITY` 권한이 필요합니다.

### Request

```
POST /activities
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 필드 | 타입 | 필수 | 설명 | 예시 |
|------|------|------|------|------|
| `clubId` | Long | X | 동아리 ID (개인 모임이면 생략) | `1` |
| `openType` | String | O | 공개 타입 (`PUBLIC`, `PRIVATE`) | `"PUBLIC"` |
| `name` | String | O | 모임 이름 (100자 이내) | `"봄맞이 독서 모임"` |
| `description` | String | O | 모임 설명 | `"함께 책을 읽고 토론하는 모임입니다."` |
| `capacity` | Integer | X | 최대 모집 인원 (null이면 제한 없음, 1 이상) | `20` |
| `isAttendanceCheck` | Boolean | X | 출석 체크 여부 (기본값: `false`) | `false` |
| `isSearchable` | Boolean | X | 검색 노출 여부 (기본값: `true`, PRIVATE 모임에서만 의미) | `true` |
| `startAt` | String (ISO 8601) | O | 시작 시간 (현재 시간 이후) | `"2026-04-01T10:00:00+09:00"` |
| `endAt` | String (ISO 8601) | O | 종료 시간 (시작 시간 이후) | `"2026-04-01T12:00:00+09:00"` |
| `categoryId` | Long | X | 카테고리 ID | `1` |
| `regionId` | Long | X | 지역 ID | `1` |
| `activityType` | String | O | 활동 방식 (`ONLINE`, `OFFLINE`, `BOTH`) | `"BOTH"` |
| `isVerificationRequired` | Boolean | X | 본인인증 필수 여부 (기본값: `false`) | `false` |
| `minAge` | Integer | X | 최소 연령 | `20` |
| `maxAge` | Integer | X | 최대 연령 | `30` |
| `gender` | String | X | 성별 제한 (`MALE`, `FEMALE`, `ANY`, 기본값: `ANY`) | `"ANY"` |

```json
{
  "clubId": 1,
  "openType": "PUBLIC",
  "name": "봄맞이 독서 모임",
  "description": "함께 책을 읽고 토론하는 모임입니다.",
  "capacity": 20,
  "isAttendanceCheck": false,
  "isSearchable": true,
  "startAt": "2026-04-01T10:00:00+09:00",
  "endAt": "2026-04-01T12:00:00+09:00",
  "categoryId": 1,
  "regionId": 1,
  "activityType": "BOTH",
  "isVerificationRequired": false,
  "minAge": 20,
  "maxAge": 30,
  "gender": "ANY"
}
```

### Response (성공)

```
HTTP 201 Created
```

```json
{
  "code": 1000,
  "status": 200,
  "message": "요청에 성공하였습니다.",
  "result": {
    "activityId": 100,
    "clubId": 1,
    "clubName": "테스트 동아리",
    "openType": "PUBLIC",
    "name": "봄맞이 독서 모임",
    "description": "함께 책을 읽고 토론하는 모임입니다.",
    "capacity": 20,
    "isAttendanceCheck": false,
    "isSearchable": true,
    "startAt": "2026-04-01T10:00:00+09:00",
    "endAt": "2026-04-01T12:00:00+09:00",
    "createdAt": "2026-02-28T15:30:00+09:00",
    "categoryId": 1,
    "categoryName": "독서",
    "regionId": 1,
    "regionName": "서울",
    "activityType": "BOTH",
    "isVerificationRequired": false,
    "minAge": 20,
    "maxAge": 30,
    "gender": "ANY"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `activityId` | Long | 생성된 모임 ID |
| `clubId` | Long | 동아리 ID (개인 모임이면 null) |
| `clubName` | String | 동아리 이름 (개인 모임이면 null) |
| `openType` | String | 공개 타입 |
| `name` | String | 모임 이름 |
| `description` | String | 모임 설명 |
| `capacity` | Integer | 최대 모집 인원 (null이면 제한 없음) |
| `isAttendanceCheck` | Boolean | 출석 체크 여부 |
| `isSearchable` | Boolean | 검색 노출 여부 |
| `startAt` | String (ISO 8601) | 시작 시간 |
| `endAt` | String (ISO 8601) | 종료 시간 |
| `createdAt` | String (ISO 8601) | 생성 시간 |
| `categoryId` | Long | 카테고리 ID (null일 수 있음) |
| `categoryName` | String | 카테고리 이름 (null일 수 있음) |
| `regionId` | Long | 지역 ID (null일 수 있음) |
| `regionName` | String | 지역 이름 (null일 수 있음) |
| `activityType` | String | 활동 방식 (`ONLINE`, `OFFLINE`, `BOTH`) |
| `isVerificationRequired` | Boolean | 본인인증 필수 여부 |
| `minAge` | Integer | 최소 연령 (null일 수 있음) |
| `maxAge` | Integer | 최대 연령 (null일 수 있음) |
| `gender` | String | 성별 제한 (`MALE`, `FEMALE`, `ANY`) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |
| 5001 | 400 | 종료 시간은 시작 시간 이후여야 합니다. | endAt ≤ startAt |
| 5002 | 400 | 시작 시간은 현재 시간 이후여야 합니다. | startAt이 과거 시간 |
| 6000 | 404 | 동아리를 찾을 수 없습니다. | 존재하지 않는 동아리 ID |
| 6001 | 403 | 해당 동아리의 멤버가 아닙니다. | 동아리 멤버가 아닌 사용자 |
| 6002 | 403 | 해당 권한이 없습니다. | MANAGE_ACTIVITY 권한 없음 |
| 5005 | 404 | 카테고리를 찾을 수 없습니다. | 존재하지 않는 카테고리 ID |
| 5006 | 404 | 지역을 찾을 수 없습니다. | 존재하지 않는 지역 ID |

---

## 2. 모임 삭제

모임을 삭제(비활성화)합니다.

- **개인 모임**: 생성자 본인만 삭제할 수 있습니다.
- **동아리 모임**: 생성자 본인 또는 `MANAGE_ACTIVITY` 권한 보유자가 삭제할 수 있습니다.

### Request

```
DELETE /activities/{activityId}
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 삭제할 모임 ID |

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
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID |
| 5003 | 403 | 이미 삭제된 모임입니다. | 이미 비활성화된 모임 |
| 5004 | 403 | 모임 삭제 권한이 없습니다. | 삭제 권한이 없는 사용자 |

---

## 3. 모임 상세 조회

모임의 상세 정보를 조회합니다. 모임 기본 정보와 참가자 목록을 반환합니다.

### Request

```
GET /activities/{activityId}
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 조회할 모임 ID |

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
    "activityId": 100,
    "name": "봄맞이 독서 모임",
    "description": "함께 책을 읽고 토론하는 모임입니다.",
    "clubId": 1,
    "clubName": "테스트 동아리",
    "openType": "PUBLIC",
    "capacity": 20,
    "startAt": "2026-04-01T10:00:00+09:00",
    "endAt": "2026-04-01T12:00:00+09:00",
    "participants": [
      {
        "id": 2,
        "profileImg": "https://example.com/img.jpg",
        "name": "참가자1"
      },
      {
        "id": 3,
        "profileImg": null,
        "name": "참가자2"
      }
    ]
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `activityId` | Long | 모임 ID |
| `name` | String | 모임 이름 |
| `description` | String | 모임 설명 |
| `clubId` | Long | 동아리 ID (개인 모임이면 null) |
| `clubName` | String | 동아리 이름 (개인 모임이면 null) |
| `openType` | String | 공개 타입 (`PUBLIC`, `PRIVATE`) |
| `capacity` | Integer | 최대 모집 인원 (null이면 제한 없음) |
| `startAt` | String (ISO 8601) | 시작 시간 |
| `endAt` | String (ISO 8601) | 종료 시간 |
| `participants` | Array | 참가자 목록 |
| `participants[].id` | Long | 참가자 ID |
| `participants[].profileImg` | String | 프로필 이미지 URL (null일 수 있음) |
| `participants[].name` | String | 참가자 이름 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |

---

## 4. 모임 목록 조회

섹션별 모임 목록을 조회합니다.

- **PERSONAL**: 사설모임 (동아리 소속이 아닌 개인 모임)
- **LIKED_CLUBS**: 관심동아리 모임 (좋아요한 동아리의 모임)
- **MY_CLUBS**: 내 동아리 모임 (소속 동아리의 모임)

### Request

```
GET /activities?section={PERSONAL|LIKED_CLUBS|MY_CLUBS}&page=0&size=10
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `section` | Query | String | O | 섹션 (`PERSONAL`, `LIKED_CLUBS`, `MY_CLUBS`) |
| `page` | Query | Integer | X | 페이지 번호 (기본값: 0) |
| `size` | Query | Integer | X | 페이지 크기 (기본값: 10) |

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
    "content": [
      {
        "activityId": 1,
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "activityType": "PERSONAL",
        "clubName": null,
        "name": "봄맞이 독서 모임",
        "categoryName": "독서",
        "regionName": "서울",
        "participantCount": 5,
        "capacity": 20,
        "likeCount": 10,
        "viewCount": 100,
        "commentCount": 3,
        "isLiked": false
      }
    ],
    "page": 0,
    "size": 10,
    "hasNext": true
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | Array | 모임 목록 |
| `content[].activityId` | Long | 모임 ID |
| `content[].thumbnailUrl` | String | 썸네일 URL (null일 수 있음) |
| `content[].activityType` | String | 모임 타입 (`PERSONAL`, `CLUB`) |
| `content[].clubName` | String | 동아리 이름 (사설모임이면 null) |
| `content[].name` | String | 모임 이름 |
| `content[].categoryName` | String | 카테고리 이름 (null일 수 있음) |
| `content[].regionName` | String | 지역 이름 (null일 수 있음) |
| `content[].participantCount` | int | 참가 인원 |
| `content[].capacity` | Integer | 정원 (null이면 무제한) |
| `content[].likeCount` | int | 좋아요 수 |
| `content[].viewCount` | int | 조회수 |
| `content[].commentCount` | int | 댓글 수 |
| `content[].isLiked` | boolean | 현재 사용자의 좋아요 여부 |
| `page` | int | 현재 페이지 번호 |
| `size` | int | 페이지 크기 |
| `hasNext` | boolean | 다음 페이지 존재 여부 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 4201 | 404 | 사용자를 찾을 수 없습니다. | 존재하지 않는 사용자 |

---

## 5. 모임 지원자 리스트 조회

모임의 지원자(참가자) 목록을 페이지네이션으로 조회합니다.

- **개인 모임**: 생성자 본인만 조회할 수 있습니다.
- **동아리 모임**: 생성자 본인 또는 `MANAGE_ACTIVITY` 권한 보유자가 조회할 수 있습니다.

### Request

```
GET /activities/{activityId}/participants?page=0&size=10
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 조회할 모임 ID |
| `page` | Query | Integer | X | 페이지 번호 (기본값: 0) |
| `size` | Query | Integer | X | 페이지 크기 (기본값: 10) |

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
    "content": [
      {
        "participantId": 1,
        "userId": 2,
        "profileImg": "https://example.com/img.jpg",
        "name": "홍길동",
        "participantStatus": "PENDING",
        "appliedAt": "2026-03-01T10:00:00+09:00"
      }
    ],
    "page": 0,
    "size": 10,
    "hasNext": false
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | Array | 지원자 목록 |
| `content[].participantId` | Long | 참가자 레코드 ID |
| `content[].userId` | Long | 사용자 ID |
| `content[].profileImg` | String | 프로필 이미지 URL (null일 수 있음) |
| `content[].name` | String | 이름 |
| `content[].participantStatus` | String | 지원 상태 (`PENDING`, `APPROVED`, `REJECTED`) |
| `content[].appliedAt` | String (ISO 8601) | 지원 일시 |
| `page` | int | 현재 페이지 번호 |
| `size` | int | 페이지 크기 |
| `hasNext` | boolean | 다음 페이지 존재 여부 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |
| 5007 | 403 | 지원자 조회 권한이 없습니다. | 조회 권한이 없는 사용자 |

---

## 6. 지원자 상태 변경 (확정/불가 처리)

지원자의 상태를 확정(APPROVED) 또는 불가(REJECTED)로 변경합니다.

- **개인 모임**: 생성자 본인만 변경할 수 있습니다.
- **동아리 모임**: 생성자 본인 또는 `MANAGE_ACTIVITY` 권한 보유자가 변경할 수 있습니다.

### Request

```
PATCH /activities/{activityId}/participants/{participantId}/status
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 모임 ID |
| `participantId` | Path | Long | O | 지원자(참가자) ID |
| `participantStatus` | Body | String | O | 변경할 상태 (`APPROVED`, `REJECTED`) |

```json
{
  "participantStatus": "APPROVED"
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
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |
| 5008 | 404 | 해당 지원자를 찾을 수 없습니다. | 존재하지 않는 지원자 ID 또는 다른 모임의 지원자 |
| 5009 | 403 | 지원자 관리 권한이 없습니다. | 관리 권한이 없는 사용자 |
| 5010 | 400 | 유효하지 않은 지원 상태입니다. | PENDING으로 변경 시도 등 |

---

## 7. 모임 참여 응답 (참여/불참 선택)

운영진이 참여 확정(APPROVED)한 지원자가 참여(CONFIRMED) 또는 불참(DECLINED)을 선택합니다.

- **본인만 호출 가능**: APPROVED 상태인 지원자 본인만 요청할 수 있습니다.
- **상태 흐름**: `PENDING → APPROVED(운영진) → CONFIRMED/DECLINED(지원자 본인)`

### Request

```
PATCH /activities/{activityId}/participation
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 모임 ID |
| `participantStatus` | Body | String | O | 참여 응답 (`CONFIRMED`, `DECLINED`) |

```json
{
  "participantStatus": "CONFIRMED"
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
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |
| 5008 | 404 | 해당 지원자를 찾을 수 없습니다. | 해당 모임에 지원하지 않은 유저 |
| 5011 | 400 | 참여 확정 상태가 아닌 지원자입니다. | APPROVED 상태가 아닌 지원자의 응답 시도 |
| 5012 | 400 | 참여 응답은 CONFIRMED 또는 DECLINED만 가능합니다. | 잘못된 상태값 입력 |

---

## 8. 관심 모임 추가

모임을 관심 모임으로 등록합니다. 이미 등록된 관심 모임을 다시 추가하면 에러가 반환됩니다.

### Request

```
POST /activities/{activityId}/interest
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 관심 등록할 모임 ID |

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
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |
| 5013 | 409 | 이미 관심 모임으로 등록되어 있습니다. | 이미 관심 모임으로 등록된 상태 |

---

## 9. 관심 모임 제거

모임을 관심 모임에서 제거합니다. 관심 등록되지 않은 모임을 제거하면 에러가 반환됩니다.

### Request

```
DELETE /activities/{activityId}/interest
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 관심 제거할 모임 ID |

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
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |
| 5014 | 404 | 관심 모임으로 등록되지 않은 모임입니다. | 관심 등록되지 않은 모임을 제거 시도 |

---

## 10. 관심 모임 목록 조회

관심 모임으로 등록한 모임 목록을 페이지네이션으로 조회합니다. 모임 목록 조회와 동일한 응답 포맷입니다.

### Request

```
GET /activities/interests?page=0&size=10
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `page` | Query | Integer | X | 페이지 번호 (기본값: 0) |
| `size` | Query | Integer | X | 페이지 크기 (기본값: 10) |

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
    "content": [
      {
        "activityId": 1,
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "activityType": "PERSONAL",
        "clubName": null,
        "name": "봄맞이 독서 모임",
        "categoryName": "독서",
        "regionName": "서울",
        "participantCount": 5,
        "capacity": 20,
        "likeCount": 10,
        "viewCount": 100,
        "commentCount": 3,
        "isLiked": true
      }
    ],
    "page": 0,
    "size": 10,
    "hasNext": false
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `content` | Array | 관심 모임 목록 |
| `content[].activityId` | Long | 모임 ID |
| `content[].thumbnailUrl` | String | 썸네일 URL (null일 수 있음) |
| `content[].activityType` | String | 모임 타입 (`PERSONAL`, `CLUB`) |
| `content[].clubName` | String | 동아리 이름 (사설모임이면 null) |
| `content[].name` | String | 모임 이름 |
| `content[].categoryName` | String | 카테고리 이름 (null일 수 있음) |
| `content[].regionName` | String | 지역 이름 (null일 수 있음) |
| `content[].participantCount` | int | 참가 인원 |
| `content[].capacity` | Integer | 정원 (null이면 무제한) |
| `content[].likeCount` | int | 좋아요 수 |
| `content[].viewCount` | int | 조회수 |
| `content[].commentCount` | int | 댓글 수 |
| `content[].isLiked` | boolean | 현재 사용자의 좋아요 여부 (항상 true) |
| `page` | int | 현재 페이지 번호 |
| `size` | int | 페이지 크기 |
| `hasNext` | boolean | 다음 페이지 존재 여부 |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |

---

## 11. 초대 코드 조회

비공개 개인 모임의 초대 코드를 조회합니다. 모임 생성자만 조회할 수 있습니다.

- **비공개 개인 모임 전용**: PUBLIC 모임이나 동아리 모임은 초대 코드가 없습니다.
- **생성자 전용**: 모임을 생성한 본인만 초대 코드를 조회할 수 있습니다.

### Request

```
GET /activities/{activityId}/invite-code
Authorization: Bearer {accessToken}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|----------|------|------|------|------|
| `activityId` | Path | Long | O | 모임 ID |

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
    "inviteCode": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `inviteCode` | String | 초대 코드 (UUID 형식) |

### 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 401 | 401 | Unauthorized | JWT 토큰 누락 또는 만료 |
| 5000 | 404 | 모임을 찾을 수 없습니다. | 존재하지 않는 모임 ID 또는 삭제된 모임 |
| 5016 | 400 | 비공개 모임만 초대 코드를 사용할 수 있습니다. | PUBLIC 모임에서 초대 코드 조회 시도 |
| 5017 | 403 | 초대 코드 조회 권한이 없습니다. | 생성자가 아닌 사용자의 조회 시도 |
| 5021 | 400 | 개인 모임만 초대 코드를 사용할 수 있습니다. | 동아리 모임에서 초대 코드 조회 시도 |

---

## 12. 초대 코드로 모임 참여

초대 코드를 사용하여 비공개 모임에 참여 신청합니다. PENDING 상태로 등록되며, 모임 생성자가 이후 APPROVED/REJECTED 처리합니다.

### Request

```
POST /activities/join
Content-Type: application/json
Authorization: Bearer {accessToken}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `inviteCode` | String | O | 초대 코드 |

```json
{
  "inviteCode": "550e8400-e29b-41d4-a716-446655440000"
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
| 5000 | 404 | 모임을 찾을 수 없습니다. | 삭제된 모임 |
| 5015 | 404 | 유효하지 않은 초대 코드입니다. | 존재하지 않는 초대 코드 |
| 5018 | 409 | 이미 참가 신청한 모임입니다. | 이미 참가 신청한 상태 |
| 5019 | 409 | 모임 정원이 가득 찼습니다. | 정원 초과 |
| 5020 | 400 | 본인이 생성한 모임에는 참가할 수 없습니다. | 생성자 본인의 참여 시도 |

---

## 공통 에러

| 코드 | HTTP | 메시지 | 설명 |
|------|------|--------|------|
| 2000 | 400 | 유효하지 않은 요청입니다. | 요청 Validation 실패 |
| 3000 | 500 | 서버에서 오류가 발생하였습니다. | 서버 내부 오류 |
