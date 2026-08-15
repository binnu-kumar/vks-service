# Backend API Contract for QA and UAT

This is a test-focused contract view of all backend services as one system.

Scope:
1. request path and method
2. auth and role requirements
3. upstream/downstream ownership
4. success status codes
5. common error status codes
6. request and response payload references

---

## 1. Environment and Routing

| Service | Base URL (local) | Context Path |
|---|---|---|
| vks-cloud-config-server | http://localhost:8080 | / |
| admin-bff | http://localhost:8088 | / |
| customer-bff | http://localhost:8089 | / |
| vks-service | http://localhost:8099 | /vks-service |
| payment-service | http://localhost:8086 | /payment-service |
| notification-service | http://localhost:8087 | /notification-service |

Config dependency:
- all services are config clients using CONFIG_SERVER_URI (default http://localhost:8080)

---

## 2. Auth and Role Matrix

| Service | Route Pattern | Auth | Role Rule |
|---|---|---|---|
| admin-bff | /bff/admin/** | required | TENANT_ADMIN |
| customer-bff | /bff/customer/** | required | CUSTOMER or TENANT_ADMIN |
| vks-service | /api/v1/auth/** | public | none |
| vks-service | /api/v1/customer/** | required | CUSTOMER |
| vks-service | /api/v1/tenant-admin/** | required | TENANT_ADMIN |
| vks-service | /api/v1/internal/** | public | internal use by payment-service |
| payment-service | /payments/webhook | public | none |
| payment-service | /payments/** (except webhook) | required | JWT authenticated |

JWT claim expectations:
- sub
- role
- tenant_id

---

## 3. admin-bff API Contract

Base path: /bff/admin

| Method | Path | Downstream | Auth | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|---|
| GET | /bff/admin/dashboard | vks-service GET /api/v1/tenant-admin/events | Bearer | 200 | 400, 401, 403, 500 | none | passthrough list<EventResponse> |
| GET | /bff/admin/events | vks-service GET /api/v1/tenant-admin/events | Bearer | 200 | 400, 401, 403, 500 | none | passthrough list<EventResponse> |
| POST | /bff/admin/events/search | vks-service POST /api/v1/tenant-admin/events/search | Bearer | 200 | 400, 401, 403, 500 | EventSearchRequest | passthrough list<EventResponse> |
| GET | /bff/admin/events/{eventId} | vks-service GET /api/v1/tenant-admin/events/{eventId} | Bearer | 200 | 400, 401, 403, 404, 500 | none | passthrough EventResponse |
| POST | /bff/admin/events | vks-service POST /api/v1/tenant-admin/events | Bearer | 201 | 400, 401, 403, 500 | EventRequest | passthrough EventResponse |
| PATCH | /bff/admin/events/{eventId} | vks-service PATCH /api/v1/tenant-admin/events/{eventId} | Bearer | 200 | 400, 401, 403, 404, 500 | EventRequest | passthrough EventResponse |
| DELETE | /bff/admin/events/{eventId} | vks-service DELETE /api/v1/tenant-admin/events/{eventId} | Bearer | 204 | 400, 401, 403, 404, 500 | none | none |
| GET | /bff/admin/events/{eventId}/slots | vks-service GET /api/v1/tenant-admin/events/{eventId}/slots | Bearer | 200 | 400, 401, 403, 404, 500 | none | passthrough list<SlotResponse> |
| POST | /bff/admin/events/{eventId}/slots | vks-service POST /api/v1/tenant-admin/events/{eventId}/slots | Bearer | 201 | 400, 401, 403, 404, 500 | SlotRequest | passthrough SlotResponse |
| PATCH | /bff/admin/events/{eventId}/slots/{slotId} | vks-service PATCH /api/v1/tenant-admin/events/{eventId}/slots/{slotId} | Bearer | 200 | 400, 401, 403, 404, 500 | SlotRequest | passthrough SlotResponse |
| DELETE | /bff/admin/events/{eventId}/slots/{slotId} | vks-service DELETE /api/v1/tenant-admin/events/{eventId}/slots/{slotId} | Bearer | 204 | 400, 401, 403, 404, 500 | none | none |

Known caveat:
- admin-bff proxies GET tenant-admin event read routes; verify those routes exist and are exposed in deployed vks-service build used by QA.

---

## 4. customer-bff API Contract

Base path: /bff/customer

| Method | Path | Downstream | Auth | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|---|
| GET | /bff/customer/profile | vks-service GET /api/v1/customer/profile | Bearer | 200 | 401, 403, 500 | none | CustomerProfileResponse |
| GET | /bff/customer/events | vks-service GET /api/v1/customer/events | Bearer | 200 | 401, 403, 500 | none | list<EventResponse> |
| GET | /bff/customer/events/{eventId} | vks-service GET /api/v1/customer/events/{eventId} | Bearer | 200 | 401, 403, 404, 500 | none | EventResponse |
| GET | /bff/customer/events/{eventId}/slots | vks-service GET /api/v1/customer/events/{eventId}/slots | Bearer | 200 | 401, 403, 404, 500 | none | list<SlotResponse> |
| POST | /bff/customer/bookings | vks-service POST /api/v1/customer/bookings | Bearer | 201 | 400, 401, 403, 404, 409, 500 | BookingRequest | BookingResponse |
| GET | /bff/customer/bookings | vks-service GET /api/v1/customer/bookings | Bearer | 200 | 401, 403, 500 | none | list<BookingResponse> |
| GET | /bff/customer/bookings/{bookingId} | vks-service GET /api/v1/customer/bookings/{bookingId} | Bearer | 200 | 401, 403, 404, 500 | none | BookingResponse |
| PATCH | /bff/customer/bookings/{bookingId}/cancel | vks-service PATCH /api/v1/customer/bookings/{bookingId}/cancel | Bearer | 200 | 401, 403, 404, 409, 500 | none | BookingResponse |
| GET | /bff/customer/confirmation/{bookingId} | vks-service GET /api/v1/customer/bookings/{bookingId} | Bearer | 200 | 401, 403, 404, 500 | none | BookingResponse |
| POST | /bff/customer/payment/orders | payment-service POST /payments/orders | Bearer | 201 | 400, 401, 403, 409, 500 | PaymentOrderRequest | PaymentResponse |
| POST | /bff/customer/payment/confirm | payment-service POST /payments/confirm | Bearer | 200 | 400, 401, 403, 404, 409, 500 | PaymentConfirmRequest | PaymentResponse |
| GET | /bff/customer/payment/{paymentOrderId} | payment-service GET /payments/{paymentOrderId} | Bearer | 200 | 401, 403, 404, 500 | none | PaymentResponse |

---

## 5. vks-service API Contract

Base URL path: /vks-service

### 5.1 Auth Endpoints

| Method | Path | Auth | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|
| POST | /api/v1/auth/signup | public | 200 | 400, 500 | SignupRequest | SignupResponse |
| POST | /api/v1/auth/tenant-admin/signup | public | 200 | 400, 500 | AdminSignupRequest | SignupResponse |
| POST | /api/v1/auth/login | public | 200 | 400, 500 | LoginRequest | LoginResponse |
| POST | /api/v1/auth/refresh | public | 200 | 400, 500 | RefreshTokenRequest | LoginResponse |
| POST | /api/v1/auth/forgot-password | public | 200 | 400, 500 | ForgotPasswordRequest | ForgotPasswordResponse |
| POST | /api/v1/auth/verify-otp | public | 200 | 400, 500 | VerifyOtpRequest | VerifyOtpResponse |
| POST | /api/v1/auth/reset-password-with-token | public | 200 | 400, 500 | ResetPasswordWithTokenRequest | ResetPasswordResponse |
| POST | /api/v1/auth/reset-password | authenticated | 200 | 400, 401, 403, 500 | ResetPasswordRequest | ResetPasswordResponse |

### 5.2 Customer Endpoints

| Method | Path | Role | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|
| GET | /api/v1/customer/profile | CUSTOMER | 200 | 401, 403, 404, 500 | none | CustomerProfileResponse |
| GET | /api/v1/customer/events | CUSTOMER | 200 | 401, 403, 500 | none | list<EventResponse> |
| GET | /api/v1/customer/events/{eventId} | CUSTOMER | 200 | 401, 403, 404, 500 | none | EventResponse |
| GET | /api/v1/customer/events/{eventId}/slots | CUSTOMER | 200 | 401, 403, 404, 500 | none | list<SlotResponse> |
| POST | /api/v1/customer/bookings | CUSTOMER | 201 | 400, 401, 403, 404, 409, 500 | BookingRequest | BookingResponse |
| GET | /api/v1/customer/bookings | CUSTOMER | 200 | 401, 403, 500 | none | list<BookingResponse> |
| GET | /api/v1/customer/bookings/{bookingId} | CUSTOMER | 200 | 401, 403, 404, 500 | none | BookingResponse |
| PATCH | /api/v1/customer/bookings/{bookingId}/cancel | CUSTOMER | 200 | 401, 403, 404, 409, 500 | none | BookingResponse |

### 5.3 Tenant Admin Endpoints

| Method | Path | Role | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|
| POST | /api/v1/tenant-admin/events/search | TENANT_ADMIN | 200 | 400, 401, 403, 500 | EventSearchRequest | list<EventResponse> |
| POST | /api/v1/tenant-admin/events | TENANT_ADMIN | 201 | 400, 401, 403, 500 | EventRequest | EventResponse |
| PATCH | /api/v1/tenant-admin/events/{eventId} | TENANT_ADMIN | 200 | 400, 401, 403, 404, 500 | EventRequest | EventResponse |
| DELETE | /api/v1/tenant-admin/events/{eventId} | TENANT_ADMIN | 204 | 401, 403, 404, 500 | none | none |
| GET | /api/v1/tenant-admin/events/{eventId}/slots | TENANT_ADMIN | 200 | 401, 403, 404, 500 | none | list<SlotResponse> |
| POST | /api/v1/tenant-admin/events/{eventId}/slots | TENANT_ADMIN | 201 | 400, 401, 403, 404, 500 | SlotRequest | SlotResponse |
| PATCH | /api/v1/tenant-admin/events/{eventId}/slots/{slotId} | TENANT_ADMIN | 200 | 400, 401, 403, 404, 500 | SlotRequest | SlotResponse |
| DELETE | /api/v1/tenant-admin/events/{eventId}/slots/{slotId} | TENANT_ADMIN | 204 | 401, 403, 404, 500 | none | none |

### 5.4 Internal Endpoint

| Method | Path | Caller | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|
| PATCH | /api/v1/internal/bookings/{bookingId}/confirm | payment-service | 200 | 404, 409, 500 | none | BookingResponse |

---

## 6. payment-service API Contract

Base URL path: /payment-service

| Method | Path | Auth | Success | Common Errors | Request Body | Response Body |
|---|---|---|---|---|---|---|
| POST | /payments/orders | required | 201 | 400, 401, 403, 409, 500 | PaymentOrderRequest | PaymentResponse |
| POST | /payments/confirm | required | 200 | 400, 401, 403, 404, 409, 500 | PaymentConfirmRequest | PaymentResponse |
| POST | /payments/webhook | public | 200 | 400, 404, 409, 500 | PaymentConfirmRequest | PaymentResponse |
| GET | /payments/{paymentOrderId} | required | 200 | 401, 403, 404, 500 | none | PaymentResponse |

Important behavior for testing:
1. /payments/confirm calls vks-service internal booking confirm endpoint.
2. payment success can be persisted even if internal booking confirm call fails (error logged).
3. /payments/orders is idempotency-key guarded per tenant.

---

## 7. notification-service Contract

REST endpoints:
- GET /internal/ping
- actuator endpoints as configured

Kafka consumer contract:
- topic: booking.events
- consumer group: notification-dispatch-consumer (default)
- retry: 3 attempts with exponential backoff
- DLT suffix: .dlq

Expected booking event payload fields:
- eventType
- eventId
- occurredAt
- tenantId
- bookingId
- customerId
- slotId
- quantity
- amount
- status

---

## 8. 

## 9. Canonical Payload Schemas

### 9.1 EventRequest
```json
{
  "eventName": "Tech Summit",
  "description": "Annual conference",
  "location": "Bengaluru",
  "startDate": "2026-09-01T10:00:00",
  "endDate": "2026-09-01T18:00:00"
}
```

### 9.2 SlotRequest
```json
{
  "slotDate": "2026-09-01",
  "startTime": "10:00:00",
  "endTime": "11:00:00",
  "price": 499.00,
  "capacity": 100
}
```

### 9.3 BookingRequest
```json
{
  "eventId": "f4b0ea7b-9a3f-41db-aa8d-8be6769f140b",
  "slotId": "a6d6fca7-c5ab-41b6-ba58-62fc89e503d5",
  "quantity": 2,
  "idempotencyKey": "booking-req-20260815-001"
}
```

### 9.4 PaymentOrderRequest
```json
{
  "bookingId": "b2db5b07-58fb-4171-9b21-cd2a7fc3f74c",
  "amount": 998.00,
  "idempotencyKey": "payment-req-20260815-001"
}
```

### 9.5 PaymentConfirmRequest
```json
{
  "paymentOrderId": "order_A1B2C3D4E5F6G7H8",
  "gatewayPaymentId": "pay_Q1W2E3R4T5",
  "signature": "signed_payload"
}
```

---

## 10. End-to-End Validation Scenarios for QA

1. Admin event lifecycle:
- create event
- update event
- create slot
- update slot
- delete slot
- delete event

2. Customer booking lifecycle:
- list events and slots
- create booking with idempotency key
- verify PAYMENT_PENDING
- cancel booking and verify CANCELLED

3. Payment-confirm path:
- create payment order
- confirm payment
- verify payment SUCCESS
- verify booking transitions to CONFIRMED via internal API call

4. Kafka path:
- BOOKING_CREATED and BOOKING_CONFIRMED messages published to booking.events
- notification-service consumes booking events
- forced consumer failure routes retries and then DLT

5. Security path:
- role mismatch returns 403
- missing JWT returns 401
- malformed body returns 400
