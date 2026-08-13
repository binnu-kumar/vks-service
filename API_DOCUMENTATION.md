# VKS Service — API Documentation

**Base URL:** `http://localhost:8099/vks-service`  
**Content-Type:** `application/json`

---

## Service Registry

| Service | Port | Context Path | Config Client |
|---------|------|--------------|---------------|
| vks-cloud-config-server | 8080 | `/` | — (is the server) |
| admin-bff | 8088 | `/` | ✅ |
| customer-bff | 8089 | `/` | ✅ |
| vks-service | 8099 | `/vks-service` | ✅ |
| notification-service | 8087 | `/notification-service` | ✅ |
| payment-service | 8086 | `/payment-service` | ✅ |

> All services fetch their configuration from `vks-cloud-config-server` at startup.  
> Config files: `D:/Saas/vks/vks-config-repo/configuration/`  
> Override config server URL: `CONFIG_SERVER_URI=http://<host>:8080`

---

## Local Dev — Docker Compose

```bash
# Start PostgreSQL + Kafka + Zookeeper + Kafka UI
docker-compose up -d

# Kafka UI available at http://localhost:9090
```

**Services started by docker-compose:**

| Container | Port | Purpose |
|-----------|------|---------|
| vks-postgres | 5432 | PostgreSQL database |
| vks-zookeeper | 2181 | Kafka coordination |
| vks-kafka | 9092 | Kafka broker |
| vks-kafka-ui | 9090 | Kafka topic browser |

**Startup order for Spring services:**
```
1. docker-compose up -d          (infra)
2. vks-cloud-config-server       (must start first)
3. vks-service
4. notification-service
5. payment-service
6. admin-bff
7. customer-bff
```

---

## Table of Contents

1. [Signup](#1-signup)
2. [Tenant Admin Signup](#1a-tenant-admin-signup)
3. [Login](#2-login)
4. [Refresh Token](#3-refresh-token)
5. [Reset Password (Logged In)](#4-reset-password-logged-in)
6. [Forgot Password — Send OTP](#5-forgot-password--send-otp)
7. [Verify OTP](#6-verify-otp)
8. [Reset Password with Token](#7-reset-password-with-token)
9. [Customer Profile](#8-customer-profile)
10. [List All Events (Customer)](#9-list-all-events-customer)
11. [Get Event by ID (Customer)](#10-get-event-by-id-customer)
12. [List Event Slots (Customer)](#11-list-event-slots-customer)
13. [List All Events (Admin)](#12-list-all-events-admin)
14. [Get Event by ID (Admin)](#13-get-event-by-id-admin)
15. [Create Event](#14-create-event)
16. [Update Event](#15-update-event)
17. [Delete Event](#16-delete-event)
18. [Search Events](#17-search-events)
19. [List Event Slots (Admin)](#18-list-event-slots-admin)
20. [Create Slot](#19-create-slot)
21. [Update Slot](#20-update-slot)
22. [Delete Slot](#21-delete-slot)
23. [Create Booking](#22-create-booking)
24. [List My Bookings](#23-list-my-bookings)
25. [Get Booking by ID](#24-get-booking-by-id)
26. [Cancel Booking](#25-cancel-booking)
27. [Create Payment Order](#26-create-payment-order)
28. [Confirm Payment](#27-confirm-payment)
29. [Payment Webhook](#28-payment-webhook)
30. [Get Payment by Order ID](#29-get-payment-by-order-id)

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

**Request Body:**
```json
{
  "firstname": "John",
  "lastname": "Doe",
  "tenantId": "tenant-123",
  "mobileno": "9876543210",
  "emailid": "john@gmail.com",
  "password": "Pass@1234",
  "confirmPassword": "Pass@1234"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| firstname | String | Yes | Not blank |
| lastname | String | Yes | Not blank |
| tenantId | String | Yes | Not blank |
| mobileno | String | Yes | Exactly 10 digits |
| emailid | String | No | Valid email format |
| password | String | Yes | Min 8 characters |
| confirmPassword | String | Yes | Must match password |

**Success `200`:** `{ "success": true, "message": "User registered successfully" }`

---

### 1A. Tenant Admin Signup

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/tenant-admin/signup` |
| **Auth** | Not required |

**Request Body:** Same as Signup + `"onboardingSecret": "admin-bootstrap-secret"`

**Success `200`:** `{ "success": true, "message": "Tenant admin registered successfully" }`

---

### 2. Login

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/login` |
| **Auth** | Not required |
| **Rate Limit** | 5 req/min per IP (enable via `RATE_LIMITING_ENABLED=true`) |

**Request Body:**
```json
{ "username": "9876543210", "password": "Pass@1234" }
```

**Success `200`:**
```json
{
  "success": true,
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tenantId": "tenant-123",
  "role": "CUSTOMER",
  "scopes": ["events:read", "slots:read", "bookings:read", "bookings:write"]
}
```

**Rate limit `429`:** `{ "error": "Too many login attempts. Please try again later." }`

---

### 3. Refresh Token

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/refresh` |
| **Auth** | Not required |

**Request Body:** `{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }`

**Success `200`:** Same shape as login response with new tokens.

---

### 4. Reset Password (Logged In)

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/reset-password` |
| **Auth** | Required |

**Request Body:** `{ "newPassword": "NewPass@5678", "confirmPassword": "NewPass@5678" }`

---

### 5. Forgot Password — Send OTP

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/forgot-password` |
| **Auth** | Not required |

**Request Body:** `{ "username": "9876543210" }`

---

### 6. Verify OTP

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/verify-otp` |
| **Auth** | Not required |

**Request Body:** `{ "username": "9876543210", "otp": "482910" }`

**Success `200`:** `{ "success": true, "resetToken": "eyJhbGciOiJIUzI1NiJ9..." }`

---

### 7. Reset Password with Token

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/auth/reset-password-with-token` |
| **Auth** | Not required |

**Request Body:** `{ "resetToken": "...", "newPassword": "NewPass@5678", "confirmPassword": "NewPass@5678" }`

---

## CUSTOMER APIs

**Base Path:** `/api/v1/customer`  
> Requires role `CUSTOMER` and `Authorization: Bearer <token>`

---

### 8. Customer Profile

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/customer/profile` |

**Success `200`:**
```json
{
  "userId": "42",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@gmail.com",
  "mobileNo": "9876543210",
  "tenantId": "tenant-123",
  "role": "CUSTOMER"
}
```

---

### 9. List All Events (Customer)

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/customer/events` |

Returns all events for the authenticated tenant ordered by start date ascending.

---

### 10. Get Event by ID (Customer)

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/customer/events/{eventId}` |

---

### 11. List Event Slots (Customer)

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/api/v1/customer/events/{eventId}/slots` |

**Success `200`:**
```json
[{
  "slotId": "...", "eventId": "...", "slotDate": "2025-09-01",
  "startTime": "09:00:00", "endTime": "10:30:00",
  "price": 499.00, "capacity": 5
}]
```

---

## TENANT ADMIN APIs

**Base Path:** `/api/v1/tenant-admin`  
> Requires role `TENANT_ADMIN` and `Authorization: Bearer <token>`

---

### 12–13. List / Get Events (Admin)

| Method | URL |
|--------|-----|
| GET | `/api/v1/tenant-admin/events` |
| GET | `/api/v1/tenant-admin/events/{eventId}` |

---

### 14. Create Event

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/tenant-admin/events` |

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

**Success `201`:** Full event object.

---

### 15–16. Update / Delete Event

| Method | URL |
|--------|-----|
| PATCH | `/api/v1/tenant-admin/events/{eventId}` |
| DELETE | `/api/v1/tenant-admin/events/{eventId}` |

---

### 17. Search Events

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/tenant-admin/events/search` |

**Request Body** — all optional:
```json
{ "eventName": "summit", "location": "bangalore", "startDateFrom": "2025-09-01T00:00:00" }
```

---

### 18–21. Slot APIs (Admin)

| Method | URL |
|--------|-----|
| GET | `/api/v1/tenant-admin/events/{eventId}/slots` |
| POST | `/api/v1/tenant-admin/events/{eventId}/slots` |
| PATCH | `/api/v1/tenant-admin/events/{eventId}/slots/{slotId}` |
| DELETE | `/api/v1/tenant-admin/events/{eventId}/slots/{slotId}` |

**Create/Update Slot Request Body:**
```json
{ "slotDate": "2025-09-01", "startTime": "09:00:00", "endTime": "10:30:00", "price": 499.00, "capacity": 5 }
```

---

## BOOKING APIs

**Base Path:** `/api/v1/customer/bookings`  
> Requires role `CUSTOMER` and `Authorization: Bearer <token>`

---

### 22. Create Booking

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/api/v1/customer/bookings` |

**Request Body:**
```json
{
  "eventId": "550e8400-e29b-41d4-a716-446655440001",
  "slotId": "550e8400-e29b-41d4-a716-446655440000",
  "quantity": 2,
  "idempotencyKey": "booking-req-001"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| eventId | UUID | Yes | Must exist in caller tenant |
| slotId | UUID | Yes | Must belong to the event |
| quantity | Integer | Yes | Min 1 |
| idempotencyKey | String | Yes | Unique per booking command |

**Success `201`:**
```json
{
  "bookingId": "...", "eventId": "...", "slotId": "...",
  "bookedBy": "42", "quantity": 2,
  "slotDate": "2025-09-01", "startTime": "09:00:00", "endTime": "10:30:00",
  "priceAtBooking": 499.00, "status": "PAYMENT_PENDING",
  "expiresAt": "2025-07-15T16:15:00"
}
```

**Booking Status Values:**

| Status | Meaning |
|--------|---------|
| DRAFT | Initial draft |
| PAYMENT_PENDING | Reserved, awaiting payment |
| CONFIRMED | Payment confirmed |
| CANCELLED | Cancelled by user |
| FAILED | Payment failed |
| EXPIRED | Hold expired |
| REFUNDED | Payment refunded |

---

### 23–25. List / Get / Cancel Booking

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/v1/customer/bookings` | List all bookings for authenticated user |
| GET | `/api/v1/customer/bookings/{bookingId}` | Get single booking |
| PATCH | `/api/v1/customer/bookings/{bookingId}/cancel` | Cancel booking |

---

## PAYMENT APIs

**Base URL:** `http://localhost:8086/payment-service`  
> `POST /payments/orders`, `POST /payments/confirm`, `GET /payments/{id}` require `Authorization: Bearer <token>`  
> `POST /payments/webhook` is public (server-to-server)

---

### 26. Create Payment Order

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/payments/orders` |
| **Auth** | Required |

#### How It Works
1. Validates JWT — extracts `tenantId` from token.
2. Checks idempotency key — rejects duplicate payment commands.
3. Generates a unique `paymentOrderId` (dummy: `order_<16-char-hex>`).
4. Creates payment record in `CREATED` status.
5. Returns payment order details for the UI to initiate payment.

**Request Body:**
```json
{
  "bookingId": "550e8400-e29b-41d4-a716-446655440010",
  "amount": 499.00,
  "idempotencyKey": "pay-req-001"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| bookingId | UUID | Yes | Must reference a booking |
| amount | BigDecimal | Yes | > 0 |
| idempotencyKey | String | Yes | Unique per payment command |

**Success `201`:**
```json
{
  "id": "...",
  "tenantId": "tenant-123",
  "bookingId": "550e8400-e29b-41d4-a716-446655440010",
  "paymentOrderId": "order_ABC123DEF456GH78",
  "gatewayPaymentId": null,
  "status": "CREATED",
  "amount": 499.00,
  "createdAt": "2025-07-15T16:00:00",
  "updatedAt": "2025-07-15T16:00:00"
}
```

**Failure `409`:** `{ "message": "Payment order with this idempotency key already exists" }`

---

### 27. Confirm Payment

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/payments/confirm` |
| **Auth** | Required |

#### How It Works
1. Looks up payment by `paymentOrderId`.
2. Validates `signature` is non-blank (dummy check — production would verify with gateway secret).
3. Sets `gatewayPaymentId`, transitions payment to `SUCCESS`.
4. Calls `PATCH /api/v1/internal/bookings/{bookingId}/confirm` on vks-service to transition booking `PAYMENT_PENDING → CONFIRMED`.
5. Publishes `PAYMENT_SUCCEEDED` event to Kafka `payment.events` topic.
6. `notification-service` picks up `BOOKING_CONFIRMED` event and sends confirmation email.

**Request Body:**
```json
{
  "paymentOrderId": "order_ABC123DEF456GH78",
  "gatewayPaymentId": "pay_XYZ987",
  "signature": "dummy-signature"
}
```

**Success `200`:**
```json
{
  "id": "...",
  "paymentOrderId": "order_ABC123DEF456GH78",
  "gatewayPaymentId": "pay_XYZ987",
  "status": "SUCCESS",
  "amount": 499.00
}
```

**Failure `409`:** `{ "message": "Invalid payment signature" }`

---

### 28. Payment Webhook

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `/payments/webhook` |
| **Auth** | Not required (server-to-server) |

#### How It Works
Same as confirm but called by the payment gateway server-to-server. No JWT required. Idempotent — if already `SUCCESS`, returns current state.

**Request Body:** Same as Confirm Payment.

**Success `200`:** Same as Confirm Payment response.

---

### 29. Get Payment by Order ID

| | |
|---|---|
| **Method** | `GET` |
| **URL** | `/payments/{paymentOrderId}` |
| **Auth** | Required |

Returns current payment status for the given order ID, scoped to the authenticated tenant.

**Success `200`:** Full payment response object.

**Failure `404`:** `{ "message": "Payment order not found: ..." }`

---

## Internal APIs (vks-service)

> Called by payment-service only. No JWT required. Not exposed to UI.

| Method | URL | Description |
|--------|-----|-------------|
| PATCH | `/api/v1/internal/bookings/{bookingId}/confirm` | Transitions booking `PAYMENT_PENDING → CONFIRMED`, publishes `BOOKING_CONFIRMED` event |

---

## BFF APIs

### Admin BFF — `http://localhost:8088`

| Method | URL | Proxies To (vks-service) |
|--------|-----|--------------------------|
| GET | `/bff/admin/dashboard` | `GET /api/v1/tenant-admin/events` |
| GET | `/bff/admin/events` | `GET /api/v1/tenant-admin/events` |
| POST | `/bff/admin/events` | `POST /api/v1/tenant-admin/events` |
| POST | `/bff/admin/events/search` | `POST /api/v1/tenant-admin/events/search` |
| GET | `/bff/admin/events/{eventId}` | `GET /api/v1/tenant-admin/events/{eventId}` |
| PATCH | `/bff/admin/events/{eventId}` | `PATCH /api/v1/tenant-admin/events/{eventId}` |
| DELETE | `/bff/admin/events/{eventId}` | `DELETE /api/v1/tenant-admin/events/{eventId}` |
| GET | `/bff/admin/events/{eventId}/slots` | `GET /api/v1/tenant-admin/events/{eventId}/slots` |
| POST | `/bff/admin/events/{eventId}/slots` | `POST /api/v1/tenant-admin/events/{eventId}/slots` |
| PATCH | `/bff/admin/events/{eventId}/slots/{slotId}` | `PATCH /api/v1/tenant-admin/events/{eventId}/slots/{slotId}` |
| DELETE | `/bff/admin/events/{eventId}/slots/{slotId}` | `DELETE /api/v1/tenant-admin/events/{eventId}/slots/{slotId}` |

### Customer BFF — `http://localhost:8089`

| Method | URL | Proxies To |
|--------|-----|------------|
| GET | `/bff/customer/profile` | vks-service `GET /api/v1/customer/profile` |
| GET | `/bff/customer/events` | vks-service `GET /api/v1/customer/events` |
| GET | `/bff/customer/events/{eventId}` | vks-service `GET /api/v1/customer/events/{eventId}` |
| GET | `/bff/customer/events/{eventId}/slots` | vks-service `GET /api/v1/customer/events/{eventId}/slots` |
| POST | `/bff/customer/bookings` | vks-service `POST /api/v1/customer/bookings` |
| GET | `/bff/customer/bookings` | vks-service `GET /api/v1/customer/bookings` |
| GET | `/bff/customer/bookings/{bookingId}` | vks-service `GET /api/v1/customer/bookings/{bookingId}` |
| PATCH | `/bff/customer/bookings/{bookingId}/cancel` | vks-service `PATCH /api/v1/customer/bookings/{bookingId}/cancel` |
| GET | `/bff/customer/confirmation/{bookingId}` | vks-service `GET /api/v1/customer/bookings/{bookingId}` |
| POST | `/bff/customer/payment/orders` | payment-service `POST /payments/orders` |
| POST | `/bff/customer/payment/confirm` | payment-service `POST /payments/confirm` |
| GET | `/bff/customer/payment/{paymentOrderId}` | payment-service `GET /payments/{paymentOrderId}` |

> All BFF calls forward `Authorization` and generate `X-Correlation-ID`.

---

## End-to-End Payment Flow

```
1. Customer creates booking
   POST /bff/customer/bookings
   → vks-service creates booking in PAYMENT_PENDING
   → Kafka: BOOKING_CREATED → notification-service sends pending email

2. Customer initiates payment
   POST /bff/customer/payment/orders
   → payment-service creates payment order in CREATED
   → Returns paymentOrderId to UI

3. Customer completes payment (dummy: any non-blank signature)
   POST /bff/customer/payment/confirm
   → payment-service validates signature
   → payment transitions to SUCCESS
   → payment-service calls vks-service PATCH /api/v1/internal/bookings/{id}/confirm
   → vks-service transitions booking PAYMENT_PENDING → CONFIRMED
   → Kafka: BOOKING_CONFIRMED → notification-service sends confirmation email
   → Kafka: PAYMENT_SUCCEEDED → payment.events topic

4. Customer views confirmation
   GET /bff/customer/confirmation/{bookingId}
   → Returns confirmed booking details
```

---

## Notification Service — `http://localhost:8087/notification-service`

### Async Notification Flow

```
vks-service → publishes BookingEvent to booking.events (key: bookingId)
notification-service → listens on booking.events (group: notification-dispatch-consumer)
  PAYMENT_PENDING  → booking-pending email
  CONFIRMED        → booking-confirmed email
  CANCELLED        → booking-cancelled email
  on failure       → retry 3x (2s → 4s backoff) → notification.dlq
```

### Kafka Topics

| Topic | Producer | Consumer |
|-------|----------|----------|
| `booking.events` | vks-service | notification-service |
| `payment.events` | payment-service | — (future analytics) |
| `notification.dlq` | RetryableTopic | — |

---

## Cloud Config Server — `http://localhost:8080`

### Config Files

```
D:/Saas/vks/vks-config-repo/configuration/
  ├── vks-service.yml
  ├── admin-bff.yml
  ├── customer-bff.yml
  ├── notification-service.yml
  └── payment-service.yml
```

---

## Scheduled Jobs

| Job | Service | Schedule | Behaviour |
|-----|---------|----------|-----------|
| BookingExpiryScheduler | vks-service | Every 60s | Marks `DRAFT`/`PAYMENT_PENDING` bookings past `expiresAt` as `EXPIRED` |

---

## Test Coverage

| Service | Test Classes |
|---------|-------------|
| vks-service | `BookingServiceImplTest`, `EventServiceImplTest`, `SlotServiceImplTest`, `LoginServiceImplTest`, `SignupServiceImplTest`, `ForgotPasswordServiceImplTest`, `ResetPasswordServiceImplTest`, `JwtUtilTest`, `AuthControllerSecurityTest`, `ProtectedApiControllerTest` |
| payment-service | `PaymentServiceTest`, `PaymentControllerTest` |

---

## Security

| Aspect | Detail |
|--------|--------|
| Password Hashing | Argon2 — iterations: 2, memory: 65536 KB, parallelism: 1 |
| Token Type | JWT signed with HMAC-SHA256 |
| Access Token Expiry | 15 minutes |
| Refresh Token Expiry | 7 days |
| OTP Length | 6 digits, single-use, 5-minute expiry |
| JWT Claims | `sub`, `username`, `tenant_id`, `role`, `scope`, `type` |
| Rate Limiting | 5 login req/min per IP (`RATE_LIMITING_ENABLED=true`) |
| Correlation ID | `X-Correlation-ID` in MDC logs, forwarded by BFFs and payment-service |
| Tenant Isolation | `tenant_id` enforced on every read/write from JWT claim |
| Role Separation | `TENANT_ADMIN` → `/api/v1/tenant-admin/**`, `CUSTOMER` → `/api/v1/customer/**` |
| Internal Endpoints | `/api/v1/internal/**` — no JWT, service-to-service only |

---

## Error Reference

| HTTP Status | Meaning |
|-------------|---------|
| 200 | Success |
| 201 | Created |
| 204 | No content |
| 400 | Validation or business logic failure |
| 401 | Missing or invalid Bearer token |
| 403 | Insufficient role |
| 404 | Not found |
| 409 | Conflict — duplicate, capacity reached, invalid state transition |
| 429 | Rate limit exceeded |
| 500 | Internal server error |

**Error Response Shape:**
```json
{
  "timestamp": "2025-07-15T16:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Booking not found with id: ...",
  "path": "/api/v1/customer/bookings/..."
}
```
