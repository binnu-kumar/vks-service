# VKS Service — API Documentation

**Base URL:** `http://localhost:8099/vks-service`  
**Content-Type:** `application/json`

---

## Table of Contents

1. [Signup](#1-signup)
2. [Login](#2-login)
3. [Refresh Token](#3-refresh-token)
4. [Reset Password (Logged In)](#4-reset-password-logged-in)
5. [Forgot Password — Send OTP](#5-forgot-password--send-otp)
6. [Verify OTP](#6-verify-otp)
7. [Reset Password with Token](#7-reset-password-with-token)
8. [List All Events](#8-list-all-events)
9. [Get Event by ID](#9-get-event-by-id)
10. [Create Event](#10-create-event)
11. [Update Event](#11-update-event)
12. [Delete Event](#12-delete-event)
13. [Search Events](#13-search-events)
14. [List Event Slots](#14-list-event-slots)
15. [Create Slot](#15-create-slot)
16. [Update Slot](#16-update-slot)
17. [Delete Slot](#17-delete-slot)

---

## AUTH APIs

**Base Path:** `/api/v1/auth`

---

### 1. Signup

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/signup` |
| **Auth** | Not required |

#### How It Works
1. Validates that `password` and `confirmPassword` match — returns error if not.
2. Checks if `mobileno` already exists in the database — returns error if duplicate.
3. Hashes the password using **Argon2** algorithm (iterations: 2, memory: 65536KB, parallelism: 1).
4. Saves the user record to the `signup` table with the hashed password.
5. Returns success response. The `id` is auto-generated (1, 2, 3...) by the database.

> Password is never stored in plain text. Argon2 is a memory-hard hashing algorithm designed to be resistant to brute-force attacks.

**Request Body:**
```json
{
  "firstname": "John",
  "lastname": "Doe",
  "mobileno": "9876543210",
  "emailid": "john@gmail.com",
  "password": "Pass@1234",
  "confirmPassword": "Pass@1234"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| firstname | String | Yes | Not blank |
| lastname | String | Yes | Not blank |
| mobileno | String | Yes | Exactly 10 digits |
| emailid | String | No | Valid email format |
| password | String | Yes | Min 8 characters |
| confirmPassword | String | Yes | Must match password |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "User registered successfully"
}
```

**Failure Response `400`:**
```json
{
  "success": false,
  "message": "Mobile number already registered"
}
```
```json
{
  "success": false,
  "message": "Password and confirm password do not match"
}
```

---

### 2. Login

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/login` |
| **Auth** | Not required |

#### How It Works
1. Looks up the user in the database by `mobileno` or `emailid` — returns error if not found.
2. Verifies the submitted password against the stored Argon2 hash — returns error if mismatch.
3. Generates two JWT tokens:
   - **Access token** — subject is the username (mobileno or emailid), valid for **15 minutes**, contains `type: access` claim.
   - **Refresh token** — same subject, valid for **7 days**, contains `type: refresh` claim.
4. Returns both tokens in the response.

> Both tokens are signed with HMAC-SHA256 using the secret key configured in `application.yml`.

**Request Body:**
```json
{
  "username": "9876543210",
  "password": "Pass@1234"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| username | String | Yes | mobileno or emailid |
| password | String | Yes | Not blank |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response Fields:**

| Field | Description |
|-------|-------------|
| token | Access token — valid for 15 minutes, use in Authorization header |
| refreshToken | Refresh token — valid for 7 days, use to get new access token |

**Failure Response `400`:**
```json
{
  "success": false,
  "message": "Invalid username or password",
  "token": null,
  "refreshToken": null
}
```

---

### 3. Refresh Token

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/refresh` |
| **Auth** | Not required |

#### How It Works
1. Validates the submitted `refreshToken` — checks signature and expiry.
2. Checks the `type` claim inside the token — rejects if it is not `refresh` (prevents using an access token here).
3. Extracts the subject (username) from the token.
4. Generates a new access token (15 min) and a new refresh token (7 days) for the same subject.
5. Returns both new tokens.

> Every refresh call issues a new refresh token. The old refresh token is not explicitly invalidated (stateless design) but will expire after 7 days.

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| refreshToken | String | Yes | Valid refresh token from login response |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "Token refreshed",
  "token": "eyJhbGciOiJIUzI1NiJ9...(new)",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...(new)"
}
```

**Failure Response `400`:**
```json
{
  "success": false,
  "message": "Invalid or expired refresh token",
  "token": null,
  "refreshToken": null
}
```

---

### 4. Reset Password (Logged In)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/reset-password` |
| **Auth** | Required — `Authorization: Bearer <token>` |

#### How It Works
1. JWT filter validates the Bearer token in the `Authorization` header — returns `401` if missing or invalid.
2. Validates that `newPassword` and `confirmPassword` match — returns error if not.
3. Looks up the user by `mobileno` or `emailid` — returns error if not found.
4. Hashes the new password using Argon2 (same parameters as signup).
5. Updates the password in the `signup` table and saves.

> This API is for users who are already logged in and want to change their password. For forgotten passwords, use the forgot-password flow instead.

**Request Body:**
```json
{
  "username": "9876543210",
  "newPassword": "NewPass@5678",
  "confirmPassword": "NewPass@5678"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| username | String | Yes | mobileno or emailid |
| newPassword | String | Yes | Min 8 characters |
| confirmPassword | String | Yes | Must match newPassword |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "Password reset successful"
}
```

**Failure Response `400`:**
```json
{
  "success": false,
  "message": "New password and confirm password do not match"
}
```
```json
{
  "success": false,
  "message": "User not found"
}
```

---

### 5. Forgot Password — Send OTP

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/forgot-password` |
| **Auth** | Not required |

#### How It Works
1. Looks up the user by `mobileno` or `emailid` — returns error if not found.
2. Checks if the user has a registered email — returns error if no email on record.
3. Deletes any previously generated unused OTPs for this username from the `otp` table.
4. Generates a new **6-digit OTP** using `SecureRandom` (cryptographically secure).
5. Saves the OTP to the `otp` table with:
   - `username` — the submitted username
   - `otp` — the 6-digit code
   - `expiry` — current time + 5 minutes
   - `used` — false
6. Sends the OTP to the user's registered email via Gmail SMTP.

> Each call to this API invalidates the previous OTP by deleting it before generating a new one.

**Request Body:**
```json
{
  "username": "9876543210"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| username | String | Yes | mobileno or emailid |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "OTP sent to registered email address. Valid for 5 minutes."
}
```

**Failure Response `400`:**
```json
{
  "success": false,
  "message": "User not found"
}
```
```json
{
  "success": false,
  "message": "No email address registered for this account"
}
```

---

### 6. Verify OTP

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/verify-otp` |
| **Auth** | Not required |

#### How It Works
1. Looks up the latest unused OTP for the username from the `otp` table ordered by expiry descending.
2. Returns error if no OTP record found (not generated or already used).
3. Checks if the OTP has expired — deletes the record and returns error if expired.
4. Compares the submitted OTP with the stored OTP — returns error if mismatch.
5. Marks the OTP as `used = true` in the database (single-use enforcement).
6. Generates a JWT **reset token** with the username as subject (valid for 15 minutes).
7. Returns the reset token — use it in the reset-password-with-token API.

> The OTP is single-use. Once verified, it cannot be used again even if it has not expired yet.

**Request Body:**
```json
{
  "username": "9876543210",
  "otp": "482910"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| username | String | Yes | mobileno or emailid |
| otp | String | Yes | 6-digit OTP received on email |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "resetToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Failure Responses `400`:**
```json
{
  "success": false,
  "message": "Invalid OTP",
  "resetToken": null
}
```
```json
{
  "success": false,
  "message": "OTP has expired",
  "resetToken": null
}
```
```json
{
  "success": false,
  "message": "OTP not generated or already used",
  "resetToken": null
}
```

---

### 7. Reset Password with Token

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/reset-password-with-token` |
| **Auth** | Not required |

#### How It Works
1. Validates that `newPassword` and `confirmPassword` match — returns error if not.
2. Validates the `resetToken` — checks JWT signature and expiry — returns error if invalid or expired.
3. Extracts the username (subject) from the reset token.
4. Looks up the user by the extracted username — returns error if not found.
5. Hashes the new password using Argon2.
6. Updates the password in the `signup` table and saves.

> The reset token is the JWT received from verify-otp. It is valid for 15 minutes. No login is required for this API.

**Request Body:**
```json
{
  "resetToken": "eyJhbGciOiJIUzI1NiJ9...",
  "newPassword": "NewPass@5678",
  "confirmPassword": "NewPass@5678"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| resetToken | String | Yes | Token received from verify-otp response |
| newPassword | String | Yes | Min 8 characters |
| confirmPassword | String | Yes | Must match newPassword |

**Success Response `200`:**
```json
{
  "success": true,
  "message": "Password reset successful"
}
```

**Failure Response `400`:**
```json
{
  "success": false,
  "message": "Reset token is invalid or expired"
}
```
```json
{
  "success": false,
  "message": "Passwords do not match"
}
```

---

## EVENT APIs

**Base Path:** `/api/v1`
> All event APIs require `Authorization: Bearer <token>` header

---

### 8. List All Events

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/events` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Fetches all records from the `events` table using `findAll()`.
3. Maps each `EventEntity` to `EventResponse` and returns the list.

> Returns all events regardless of date or status. Use the search API for filtered results.

No request body.

**Success Response `200`:**
```json
[
  {
    "id": 1,
    "eventName": "Annual Tech Summit",
    "description": "A yearly technology conference",
    "location": "Bangalore",
    "startDate": "2025-09-01T09:00:00",
    "endDate": "2025-09-03T18:00:00",
    "createdBy": "9876543210",
    "createdAt": "2025-07-01T10:00:00",
    "updatedAt": "2025-07-01T10:00:00"
  }
]
```

> Returns empty array `[]` if no events exist.

---

### 9. Get Event by ID

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/events/{eventId}` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Looks up the event by `id` in the `events` table.
3. Returns the event if found, throws `RuntimeException` if not found.

No request body. Replace `{eventId}` with the numeric event id.

**Success Response `200`:**
```json
{
  "id": 1,
  "eventName": "Annual Tech Summit",
  "description": "A yearly technology conference",
  "location": "Bangalore",
  "startDate": "2025-09-01T09:00:00",
  "endDate": "2025-09-03T18:00:00",
  "createdBy": "9876543210",
  "createdAt": "2025-07-01T10:00:00",
  "updatedAt": "2025-07-01T10:00:00"
}
```

**Failure Response `500`:**
```json
Event not found with id: 1
```

---

### 10. Create Event

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/events` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Extracts the authenticated username from the `Authentication` object (set by JWT filter) — this becomes `createdBy`.
3. Validates the request body — `eventName` is required.
4. Creates a new `EventEntity`, maps all fields from the request.
5. Sets `createdBy` from the JWT subject (username used to login).
6. `createdAt` and `updatedAt` are automatically set via `@PrePersist`.
7. Saves to the `events` table and returns the saved entity as response with HTTP `201`.

> `createdBy` is never sent in the request body — it is always taken from the Bearer token automatically.

**Request Body:**
```json
{
  "eventName": "Annual Tech Summit",
  "description": "A yearly technology conference",
  "location": "Bangalore",
  "startDate": "2025-09-01T09:00:00",
  "endDate": "2025-09-03T18:00:00"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| eventName | String | Yes | Not blank |
| description | String | No | — |
| location | String | No | — |
| startDate | LocalDateTime | No | Format: `yyyy-MM-ddTHH:mm:ss` |
| endDate | LocalDateTime | No | Format: `yyyy-MM-ddTHH:mm:ss` |

**Success Response `201`:**
```json
{
  "id": 1,
  "eventName": "Annual Tech Summit",
  "description": "A yearly technology conference",
  "location": "Bangalore",
  "startDate": "2025-09-01T09:00:00",
  "endDate": "2025-09-03T18:00:00",
  "createdBy": "9876543210",
  "createdAt": "2025-07-01T10:00:00",
  "updatedAt": "2025-07-01T10:00:00"
}
```

---

### 11. Update Event

| | |
|---|---|
| **Method** | `PATCH` |
| **URL** | `/api/v1/events/{eventId}` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Looks up the existing event by `eventId` — throws `RuntimeException` if not found.
3. Updates all fields on the existing entity from the request body.
4. `updatedAt` is automatically refreshed via `@PreUpdate`.
5. Saves the updated entity and returns it.

> This is a full update — all fields in the request will overwrite the existing values. `createdBy` and `createdAt` are never changed on update.

Replace `{eventId}` with the numeric event id.

**Request Body:**
```json
{
  "eventName": "Annual Tech Summit 2025",
  "description": "Updated description",
  "location": "Mumbai",
  "startDate": "2025-09-01T09:00:00",
  "endDate": "2025-09-03T18:00:00"
}
```

**Success Response `200`:**
```json
{
  "id": 1,
  "eventName": "Annual Tech Summit 2025",
  "description": "Updated description",
  "location": "Mumbai",
  "startDate": "2025-09-01T09:00:00",
  "endDate": "2025-09-03T18:00:00",
  "createdBy": "9876543210",
  "createdAt": "2025-07-01T10:00:00",
  "updatedAt": "2025-07-01T12:00:00"
}
```

**Failure Response `500`:**
```json
Event not found with id: 1
```

---

### 12. Delete Event

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/api/v1/events/{eventId}` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Looks up the existing event by `eventId` — throws `RuntimeException` if not found.
3. Deletes the event from the `events` table.
4. Returns HTTP `204 No Content` on success.

> Deleting an event will cascade to all associated slots.

No request body. Replace `{eventId}` with the numeric event id.

**Success Response `204`:**
```
No content
```

**Failure Response `500`:**
```json
Event not found with id: 1
```

---

### 13. Search Events

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/events/search` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Receives the search filters in the request body — all fields are optional.
3. Builds a JPA `Specification` dynamically using only the fields that are provided (non-null, non-blank):
   - `eventName` — SQL `LIKE %value%` (case-insensitive)
   - `location` — SQL `LIKE %value%` (case-insensitive)
   - `createdBy` — SQL `= value` (exact match)
   - `startDateFrom` — SQL `startDate >= value`
   - `startDateTo` — SQL `startDate <= value`
   - `endDateFrom` — SQL `endDate >= value`
   - `endDateTo` — SQL `endDate <= value`
4. All active filters are combined with SQL `AND`.
5. Executes the query and returns matching events.

> Sending an empty `{}` body returns all events. Each filter you add narrows the results further.

**Request Body** — all fields optional:
```json
{
  "eventName": "summit",
  "location": "bangalore",
  "createdBy": "9876543210",
  "startDateFrom": "2025-09-01T00:00:00",
  "startDateTo": "2025-09-30T23:59:59",
  "endDateFrom": "2025-09-01T00:00:00",
  "endDateTo": "2025-09-30T23:59:59"
}
```

**Request Fields:**

| Field | Type | Required | Behaviour |
|-------|------|----------|-----------|
| eventName | String | No | Partial, case-insensitive match |
| location | String | No | Partial, case-insensitive match |
| createdBy | String | No | Exact match |
| startDateFrom | LocalDateTime | No | Events starting on or after this date |
| startDateTo | LocalDateTime | No | Events starting on or before this date |
| endDateFrom | LocalDateTime | No | Events ending on or after this date |
| endDateTo | LocalDateTime | No | Events ending on or before this date |

**Success Response `200`:**
```json
[
  {
    "id": 1,
    "eventName": "Annual Tech Summit",
    "description": "A yearly technology conference",
    "location": "Bangalore",
    "startDate": "2025-09-01T09:00:00",
    "endDate": "2025-09-03T18:00:00",
    "createdBy": "9876543210",
    "createdAt": "2025-07-01T10:00:00",
    "updatedAt": "2025-07-01T10:00:00"
  }
]
```

> Returns empty array `[]` if no events match the filters.

---

## SLOT APIs

**Base Path:** `/api/v1/events/{eventId}/slots`
> All slot APIs require `Authorization: Bearer <token>` header

---

### 14. List Event Slots

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/events/{eventId}/slots` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Validates that the event exists by `eventId` — throws `RuntimeException` if not found.
3. Fetches all slot records associated with the event using `findByEventEventId(eventId)`.
4. Maps each `SlotEntity` to `SlotResponse` and returns the list.

> Returns all slots for the specified event in chronological order.

No request body. Replace `{eventId}` with the event UUID.

**Success Response `200`:**
```json
[
  {
    "slotId": "550e8400-e29b-41d4-a716-446655440000",
    "eventId": "550e8400-e29b-41d4-a716-446655440001",
    "slotDate": "2025-09-01",
    "startTime": "09:00:00",
    "endTime": "10:30:00",
    "createdAt": "2025-07-15T14:30:00",
    "updatedAt": "2025-07-15T14:30:00"
  }
]
```

> Returns empty array `[]` if no slots exist for the event.

---

### 15. Create Slot

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/events/{eventId}/slots` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Validates that the event exists by `eventId` — throws `RuntimeException` if not found.
3. Validates the request body — `slotDate`, `startTime`, and `endTime` are required.
4. Creates a new `SlotEntity` and maps all fields from the request.
5. Associates the slot with the event.
6. `createdAt` and `updatedAt` are automatically set via `@PrePersist`.
7. Saves to the `slots` table and returns the saved entity as response with HTTP `201`.

> Each slot belongs to exactly one event. Replace `{eventId}` with the event UUID.

**Request Body:**
```json
{
  "slotDate": "2025-09-01",
  "startTime": "09:00:00",
  "endTime": "10:30:00"
}
```

**Request Fields:**

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| slotDate | LocalDate | Yes | Format: `yyyy-MM-dd` |
| startTime | LocalTime | Yes | Format: `HH:mm:ss` |
| endTime | LocalTime | Yes | Format: `HH:mm:ss` |

**Success Response `201`:**
```json
{
  "slotId": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "slotDate": "2025-09-01",
  "startTime": "09:00:00",
  "endTime": "10:30:00",
  "createdAt": "2025-07-15T14:30:00",
  "updatedAt": "2025-07-15T14:30:00"
}
```

**Failure Response `500`:**
```json
Event not found with id: 550e8400-e29b-41d4-a716-446655440001
```

---

### 16. Update Slot

| | |
|---|---|
| **Method** | `PATCH` |
| **URL** | `/api/v1/events/{eventId}/slots/{slotId}` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Validates that the event exists by `eventId` — throws `RuntimeException` if not found.
3. Looks up the existing slot by `slotId` — throws `RuntimeException` if not found.
4. Updates all fields on the existing slot entity from the request body.
5. `updatedAt` is automatically refreshed via `@PreUpdate`.
6. Saves the updated entity and returns it.

> This is a full update — all fields in the request will overwrite the existing values. `createdAt` is never changed on update.

Replace `{eventId}` and `{slotId}` with their respective UUIDs.

**Request Body:**
```json
{
  "slotDate": "2025-09-02",
  "startTime": "14:00:00",
  "endTime": "15:30:00"
}
```

**Success Response `200`:**
```json
{
  "slotId": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "slotDate": "2025-09-02",
  "startTime": "14:00:00",
  "endTime": "15:30:00",
  "createdAt": "2025-07-15T14:30:00",
  "updatedAt": "2025-07-15T15:45:00"
}
```

**Failure Response `500`:**
```json
Slot not found with id: 550e8400-e29b-41d4-a716-446655440000
```

---

### 17. Delete Slot

| | |
|---|---|
| **Method** | `DELETE` |
| **URL** | `/api/v1/events/{eventId}/slots/{slotId}` |
| **Auth** | Required |

#### How It Works
1. JWT filter validates the Bearer token — returns `401` if missing or invalid.
2. Validates that the event exists by `eventId` — throws `RuntimeException` if not found.
3. Looks up the existing slot by `slotId` — throws `RuntimeException` if not found.
4. Deletes the slot from the `slots` table.
5. Returns HTTP `204 No Content` on success.

> Deleting a slot does not affect the associated event.

No request body. Replace `{eventId}` and `{slotId}` with their respective UUIDs.

**Success Response `204`:**
```
No content
```

**Failure Response `500`:**
```json
Slot not found with id: 550e8400-e29b-41d4-a716-446655440000
```

---

## Forgot Password Flow

```
Step 1 — POST /forgot-password
         Send username → OTP generated and sent to registered email

Step 2 — POST /verify-otp
         Send username + OTP → OTP verified, resetToken returned

Step 3 — POST /reset-password-with-token
         Send resetToken + newPassword + confirmPassword → Password updated
```

---

## Security

| Aspect | Detail |
|--------|--------|
| Password Hashing | Argon2 — iterations: 2, memory: 65536 KB, parallelism: 1 |
| Token Type | JWT signed with HMAC-SHA256 |
| Access Token Expiry | 15 minutes |
| Refresh Token Expiry | 7 days |
| OTP Length | 6 digits |
| OTP Expiry | 5 minutes |
| OTP Usage | Single-use — marked used after verification |
| Token Claim | `type: access` or `type: refresh` to prevent token misuse |

---

## Error Reference

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Success |
| 201 | Created successfully |
| 400 | Bad request — validation error or business logic failure |
| 401 | Unauthorized — missing or invalid Bearer token |
| 403 | Forbidden — valid token but access denied |
| 500 | Internal server error |
