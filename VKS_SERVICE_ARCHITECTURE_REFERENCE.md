# VKS Service Architecture Reference

This document maps the current `vks-service` codebase to the target architecture defined in the local `Documents-DB` repository.

Use this as a working reference for:
1. what this service should own
2. what still needs to be implemented or refactored
3. what APIs are needed
4. how the end-to-end flows should work

This is not a final production architecture spec. It is a practical implementation guide for bringing `vks-service` closer to the intended design.

---

## 1. Source Architecture Reference

This reference is aligned to the local `Documents-DB` starter kit, especially:
1. `implementation-starter-kit/01-architecture-overview.md`
2. `implementation-starter-kit/02-service-boundaries.md`
3. `implementation-starter-kit/backend/springboot-structure.md`
4. `implementation-starter-kit/backend/security-auth.md`
5. `implementation-starter-kit/apis/openapi-phase1.yaml`

---

## 2. Current State of vks-service

Today `vks-service` contains these main capabilities in a single Spring Boot application:
1. signup
2. tenant-admin signup
2. login and refresh token
3. forgot-password and reset-password flows
4. event catalog CRUD and search
5. slot CRUD with capacity
6. booking create, list-my-bookings, cancel with payment-pending hold semantics

### Current strengths

1. The project already has a clean controller/service/repository split.
2. JWT-based authentication is in place with `tenant_id`, `role`, and `scope` claims.
3. Customer and tenant-admin routes are now separated.
4. Core event, slot, and booking vertical slices exist.
5. Service-layer and web/security tests now exist.

### Current architectural limitations

1. It is still one consolidated service, while the target architecture is service-oriented.
2. Slot hold logic is still DB-backed and local to the booking flow, not a dedicated inventory-lock service.
3. There is no payment flow yet.
4. Notification is still synchronous email sending inside service logic rather than async event-driven processing.
5. There is no async event publishing/consuming flow.

---

## 3. Target Architecture for vks-service

Based on the `Documents-DB` architecture, the ideal production design is:

1. UI layer
2. Gateway/BFF layer
3. Domain services layer
4. Data and messaging layer

For now, `vks-service` should be treated as an implementation step toward the domain services layer.

### Domain services expected in target architecture

1. Identity Service
2. Customer Service
3. Event Catalog Service
4. Slot Inventory Service
5. Booking Service
6. Payment Service
7. Notification Service

### Practical interpretation for this repo

Right now this repo can evolve in 2 possible ways:

1. Transitional approach:
Keep `vks-service` as one codebase for now, but implement clear internal module boundaries that match the target service ownership.

2. Final approach:
Split this codebase into multiple services later once API contracts and internal rules stabilize.

Recommended path for now:
Use the transitional approach first. It is faster and safer.

---

## 3A. How vks-service Should Work With BFF

The target architecture expects the frontend to call BFF APIs, not domain services directly.

That means the ideal runtime path is:

1. Angular UI
2. API Gateway
3. BFF
4. `vks-service` or split domain services

### 3A.1 High-level request path

For now, if `vks-service` stays as one backend application, it should behave like the domain-service layer behind the BFF.

#### Admin flow

```text
Angular Tenant Admin UI
  -> Gateway
  -> Admin BFF
  -> vks-service
     -> auth/event/slot/booking/payment modules
```

#### Customer flow

```text
Angular Customer UI
  -> Gateway
  -> Customer BFF
  -> vks-service
     -> auth/event/slot/booking/payment modules
```

### 3A.2 What the BFF should do

The BFF is not the source of truth for bookings, events, slots, or payments.

The BFF should own:
1. response shaping for UI screens
2. aggregation across multiple domain modules
3. frontend-friendly endpoint design
4. hiding internal service boundaries from UI
5. optional orchestration of multi-step reads

The BFF should not own:
1. event persistence
2. slot persistence
3. booking lifecycle truth
4. payment truth
5. auth token issuance logic

### 3A.3 What vks-service should do behind the BFF

`vks-service` should behave as the business system of record for the domain logic it owns.

That means:
1. validate business rules
2. persist domain state
3. enforce tenant and role rules
4. return stable machine-friendly responses
5. emit domain events when needed

### 3A.4 Transitional model for this repo

Because `vks-service` is still one application, the practical near-term model is:

1. Admin BFF calls `vks-service` for auth, event, and slot admin flows.
2. Customer BFF calls `vks-service` for customer event views, slot availability, bookings, and later payments.
3. `vks-service` keeps internal module boundaries even before physical service split.

This lets you keep coding in one codebase while preserving the future architecture.

---

## 4. Recommended Internal Boundaries in vks-service

Even if this stays one deployable service for now, the code should behave as if these logical services already exist.

### 4.1 Identity module

Owns:
1. login
2. refresh token
3. JWT creation
4. password reset
5. forgot password

Should own next:
1. logout or token revocation strategy
2. audit of sensitive auth actions

Should not own:
1. event business rules
2. booking lifecycle
3. payment lifecycle

### 4.2 Event Catalog module

Owns:
1. event metadata
2. event schedule
3. event list/search
4. event admin CRUD

Should not own:
1. booking status
2. seat locking
3. payment state

### 4.3 Slot Inventory module

Owns:
1. slot definition
2. slot availability state
3. slot lock or reservation hold
4. lock expiry

Current repo only has:
1. slot CRUD with capacity
2. booking hold expiry handled during booking creation

Still needed:
1. lock table or reservation table
2. central lock expiry handling such as scheduled cleanup or Redis TTL
3. stronger race-condition protection under concurrent load
4. quantity semantics if multi-seat booking is needed

### 4.4 Booking module

Owns:
1. booking creation
2. booking state transitions
3. booking history
4. cancellation rules

Current repo only has:
1. booking creation in `PAYMENT_PENDING`
2. idempotency key support
3. list my bookings
4. cancel booking
5. hold expiry timestamp on booking

Still needed:
1. booking-confirmed after payment confirmation
2. stronger automatic expiry handling beyond opportunistic cleanup
3. payment-linked transitions
4. richer failure and refund behavior

### 4.5 Payment module

Owns:
1. payment order creation
2. payment confirmation callback/webhook
3. retry/refund state

Current repo has:
1. OTP email sending
2. booking pending email
3. booking cancellation email
4. tenant-admin onboarding email

### 4.6 Notification module

Owns:
1. email templates
2. SMS/WhatsApp hooks if needed
3. send booking confirmation
4. send payment confirmation
5. retry notification failures

Current repo has:
1. OTP email sending only

---

## 5. Security Model Required by Architecture

The target architecture expects JWT claims to include:
1. `sub`
2. `tenant_id`
3. `role`
4. `scope`

### What current code does

Current JWTs only carry:
1. `sub`
2. `username`
3. `tenant_id`
4. `role`
5. `scope`
6. `type`

### What needs to change

#### 5.1 JWT claims

Status:
Implemented.

Access token should include at least:

```json
{
  "sub": "user-id-or-login-id",
  "tenant_id": "tenant-123",
  "role": "TENANT_ADMIN",
  "scope": ["events:write", "slots:write", "bookings:read"]
}
```

Refresh token should be separate and minimal.

#### 5.2 Security enforcement

The system should enforce:
1. unauthenticated routes only for signup/login/forgot-password/reset-password-with-token
2. tenant admin routes only for `TENANT_ADMIN`
3. customer routes only for `CUSTOMER`
4. tenant_id filtering on every tenant-scoped query

Status:
Implemented for current auth, event, slot, and booking routes.

#### 5.3 Repository and service rules

1. Never trust tenant_id from request body.
2. Read tenant_id from JWT/auth context.
3. Apply tenant filters in queries or service layer.
4. Audit admin-sensitive actions.

Status:
Tenant filtering is implemented on current event, slot, and booking paths. Audit logging is still minimal.

---

## 6. Recommended API Surface

The `Documents-DB` phase-1 OpenAPI suggests a split between auth, tenant admin, customer, booking, and payment flows.

Below is the recommended API shape for `vks-service` if kept as one repo.

### 6.1 Auth APIs

Keep:
1. `POST /api/v1/auth/signup`
2. `POST /api/v1/auth/tenant-admin/signup`
3. `POST /api/v1/auth/login`
4. `POST /api/v1/auth/refresh`
5. `POST /api/v1/auth/forgot-password`
6. `POST /api/v1/auth/verify-otp`
7. `POST /api/v1/auth/reset-password-with-token`
8. `POST /api/v1/auth/reset-password`

Add later:
1. `POST /api/v1/auth/logout`
2. `POST /api/v1/auth/revoke`

### 6.2 Tenant Admin APIs

These are admin-managed event and slot operations.

Recommended routes:
1. `GET /api/v1/tenant-admin/events`
2. `POST /api/v1/tenant-admin/events`
3. `PATCH /api/v1/tenant-admin/events/{eventId}`
4. `DELETE /api/v1/tenant-admin/events/{eventId}`
5. `POST /api/v1/tenant-admin/events/search`
6. `GET /api/v1/tenant-admin/events/{eventId}/slots`
7. `POST /api/v1/tenant-admin/events/{eventId}/slots`
8. `PATCH /api/v1/tenant-admin/events/{eventId}/slots/{slotId}`
9. `DELETE /api/v1/tenant-admin/events/{eventId}/slots/{slotId}`

Status:
Implemented.

#### How these work with Admin BFF

The Admin BFF should expose admin-screen-friendly endpoints to Angular, for example:
1. `GET /bff/admin/events`
2. `POST /bff/admin/events`
3. `PATCH /bff/admin/events/{eventId}`
4. `GET /bff/admin/events/{eventId}/slots`

The Admin BFF then calls `vks-service` tenant-admin routes internally.

Admin BFF responsibilities here:
1. attach forwarded auth context from gateway
2. optionally transform UI query params into service filters
3. combine event and slot data if the screen needs one payload

`vks-service` responsibilities here:
1. validate role is `TENANT_ADMIN`
2. enforce tenant ownership
3. save and return domain state

### 6.3 Customer APIs

Recommended customer-facing routes:
1. `GET /api/v1/customer/profile`
2. `GET /api/v1/customer/events`
3. `GET /api/v1/customer/events/{eventId}`
4. `GET /api/v1/customer/events/{eventId}/slots`
5. `POST /api/v1/customer/bookings`
6. `GET /api/v1/customer/bookings`
7. `GET /api/v1/customer/bookings/{bookingId}`
8. `PATCH /api/v1/customer/bookings/{bookingId}/cancel`

Status:
Customer bookings are now exposed under customer-scoped routes.

#### How these work with Customer BFF

The Customer BFF should expose customer-friendly endpoints such as:
1. `GET /bff/customer/events`
2. `GET /bff/customer/events/{eventId}`
3. `GET /bff/customer/events/{eventId}/slots`
4. `POST /bff/customer/bookings`
5. `GET /bff/customer/bookings`

The Customer BFF then calls `vks-service` customer/domain routes internally.

Customer BFF responsibilities here:
1. return frontend-ready event cards and booking summaries
2. combine booking plus payment status when needed
3. hide internal route fragmentation from the UI

`vks-service` responsibilities here:
1. validate customer permissions
2. enforce tenant scoping
3. create and manage booking state correctly
4. return source-of-truth booking data

### 6.4 Payment APIs

Recommended:
1. `POST /api/v1/payments/orders`
2. `POST /api/v1/payments/confirm`
3. `POST /api/v1/payments/webhook`
4. `GET /api/v1/payments/{paymentOrderId}`

Current mismatch:
No payment APIs exist yet.

#### How payment should work with BFF

Recommended flow:
1. Customer UI calls Customer BFF to start payment.
2. Customer BFF calls `vks-service` booking module to validate booking state.
3. Customer BFF calls payment module in `vks-service` or later dedicated payment service.
4. Payment order details are returned to UI.
5. Payment callback or webhook updates payment state server-to-server.

Important rule:
The BFF may orchestrate the flow, but it should not become the payment source of truth.

### 6.5 Notification APIs

These may remain internal in many systems, but if exposed:
1. `POST /api/v1/internal/notifications/send`
2. `POST /api/v1/internal/notifications/retry`

Current mismatch:
Notification now exists for OTP, booking pending, booking cancelled, and admin onboarding, but it is still synchronous and local to the service layer.

---

## 7. Recommended Booking Flow

This is the most important business flow to align correctly.

### 7.1 Current booking flow

Current implementation:
1. validate event exists
2. validate slot belongs to event
3. expire stale holds for the slot during create
4. prevent duplicate active booking by same user for same slot
5. check active bookings against slot capacity
6. create booking in `PAYMENT_PENDING` state
7. attach hold expiry timestamp

This is a stronger transitional design than the initial prototype, but it is still not the final payment-integrated architecture.

### 7.2 Target booking flow

Recommended future flow:

#### Step 1. Customer selects event and slot

1. customer browses events
2. customer views slots
3. system checks slot is available

In BFF model:
1. UI calls Customer BFF
2. Customer BFF fetches event/slot data from `vks-service`
3. Customer BFF shapes one UI response

#### Step 2. Create booking draft

`POST /api/v1/customer/bookings`

Request should include:
1. eventId
2. slotId
3. quantity if capacity-based
4. idempotency key

Service behavior:
1. validate tenant and user
2. validate event and slot
3. create slot lock or reservation hold
4. create booking in `DRAFT` or `PAYMENT_PENDING`
5. snapshot price into booking

In BFF model:
1. UI sends one booking command to Customer BFF
2. Customer BFF forwards the command to `vks-service`
3. `vks-service` creates the booking draft
4. Customer BFF returns the booking summary to UI

#### Step 3. Create payment order

`POST /api/v1/payments/orders`

Service behavior:
1. validate booking is payable
2. create payment order
3. return payment gateway details

In BFF model:
1. UI calls Customer BFF
2. Customer BFF orchestrates booking lookup plus payment order creation
3. BFF returns a UI-friendly payment payload

#### Step 4. Payment confirmation

`POST /api/v1/payments/confirm` or webhook-driven confirm

Service behavior:
1. verify payment callback
2. update payment state
3. update booking state to `CONFIRMED`
4. release lock into final inventory consumption
5. emit async event for notifications

In BFF model:
1. customer browser should not be the only confirmation source
2. server-side confirmation or webhook must update the final state
3. BFF can poll or fetch final booking status for the confirmation page

#### Step 5. Notification send

1. send booking confirmation
2. send payment confirmation
3. store retry state if send fails

### 7.3 Suggested booking states

Recommended enum:
1. `DRAFT`
2. `PAYMENT_PENDING`
3. `CONFIRMED`
4. `CANCELLED`
5. `FAILED`
6. `EXPIRED`
7. `REFUNDED`

Current code only has:
1. `CONFIRMED`
2. `CANCELLED`

---

## 8. Recommended Slot Inventory Flow

The target architecture separates slot inventory from booking confirmation.

### What is needed

1. slot capacity or inventory count
2. temporary lock on booking start
3. lock expiry job or TTL
4. final consumption after payment confirm

Status:
Slot capacity and temporary booking hold expiry are implemented. Dedicated lock storage and scheduled expiry are still pending.

### Minimal design if not using Redis yet

You can start with a database-backed lock table:

1. `slot_locks`
2. `lock_id`
3. `slot_id`
4. `customer_id`
5. `status`
6. `expires_at`

Then later move hot lock management to Redis if needed.

### If a slot has unlimited bookings

Then explicitly document that business rule and skip lock logic.

### If a slot has fixed capacity

Then you must implement:
1. capacity on slot
2. confirmed count or reserved count
3. lock expiry handling
4. race-condition protection

Status:
Capacity and reserved-count style checks are implemented in the booking flow. Stronger concurrency protection is still needed for production-grade inventory control.

---

## 9. Payment Flow Required by Architecture

Payment is absent today but must be planned before booking is considered complete.

### Required entities

1. payment order
2. payment attempt
3. payment status
4. reconciliation record

### Minimum states

1. `CREATED`
2. `PENDING`
3. `SUCCESS`
4. `FAILED`
5. `REFUNDED`

### Minimum APIs

1. create payment order
2. confirm payment
3. webhook processing
4. fetch payment status

### Required linkage

1. payment belongs to booking
2. booking transitions depend on payment outcome

---

## 10. Notification Flow Required by Architecture

Current system sends OTP email only.

Current implementation now also sends:
1. booking pending email
2. booking cancellation email
3. tenant-admin onboarding email

Needed later:
1. booking confirmation email
2. payment success email
3. cancellation email
4. failure retry strategy

Better design:
1. booking confirmed event emitted
2. notification consumer handles actual send
3. failed sends go to retry or DLQ

---

## 11. Database and Data Design Gaps

### Existing data areas

1. signup users
2. otp
3. events
4. slots
5. bookings

### Important missing fields and concepts

#### Users

Need:
1. role
2. tenant_id
3. status
4. audit timestamps if needed

Status:
Role and tenant_id are implemented. Status and audit fields are still pending.

#### Events

Need:
1. tenant_id
2. status
3. optional publish flag

#### Slots

Need:
1. tenant_id or guaranteed parent tenant path
2. capacity if limited inventory
3. status

Status:
Capacity is implemented. Tenant scoping is enforced via parent event ownership. Slot status is still pending.

#### Bookings

Need:
1. tenant_id
2. booking state lifecycle
3. idempotency key
4. quantity if multi-seat
5. payment linkage
6. cancellation reason if needed

Status:
Tenant_id, booking states, and idempotency key are implemented. Quantity, payment linkage, and cancellation reason are still pending.

#### Payments

Need:
1. payment order table
2. transaction or reconciliation table

#### Inventory locks

Need:
1. slot lock table or Redis lock strategy

---

## 12. Technical Gaps to Close

These are the concrete engineering items still needed.

### 12.1 Security

1. add logout/revocation strategy if needed
2. strengthen audit coverage for admin-sensitive operations

### 12.2 API contract

1. align route naming to architecture contract
2. separate tenant-admin and customer API surfaces
3. standardize response shapes
4. version route contracts intentionally

### 12.3 Error handling

1. return 422 where appropriate for richer domain validation
2. expand typed exception coverage where additional modules are added

### 12.4 Booking reliability

1. move hold expiry from opportunistic cleanup to scheduled or distributed expiry
2. add stronger slot lock strategy for concurrency
3. integrate payment confirmation transitions
4. add refund and failure flows

### 12.5 Async architecture

1. publish booking confirmed event
2. publish payment success/failure event
3. add Kafka consumers where needed
4. add retry or DLQ plan

### 12.6 Observability

1. correlation id propagation
2. structured logs with booking/payment ids
3. better audit logging for sensitive actions
4. metrics for booking success/failure

---

## 13. Recommended Implementation Order

This is the most practical coding sequence.

### Phase 1. Security foundation

1. enrich JWT with `tenant_id`, `role`, `scope`
2. update JWT filter to parse claims into authorities
3. enforce tenant-admin vs customer route separation

Status:
Implemented.

### Phase 2. Event and slot scoping

1. add tenant_id to event and slot entities
2. enforce tenant-aware reads/writes
3. rename routes to explicit tenant-admin paths if desired

Status:
Implemented for current event, slot, and booking flows.

### Phase 3. Booking hardening

1. add booking draft or payment-pending state
2. add idempotency key
3. move booking API toward customer-scoped routes
4. add booking state transition rules

Status:
Partially implemented. Payment-pending, idempotency, and customer route scope are done. Payment-driven confirmation flow is still pending.

### Phase 4. Slot inventory

1. define slot capacity semantics
2. add lock or reservation strategy
3. add expiry behavior

Status:
Partially implemented. Capacity and hold expiry behavior are in place. Dedicated lock storage and stronger concurrency control are still pending.

### Phase 5. Payment module

1. create payment entities and APIs
2. wire booking to payment lifecycle
3. add callback/webhook handling

### Phase 6. Notification and async flow

1. emit booking/payment events
2. process notifications asynchronously
3. add retry/DLQ behavior

### Phase 7. Polish and production readiness

1. global exception handling
2. contract tests
3. repository integration tests
4. monitoring and audit improvements

---

## 14. Suggested API Direction for This Repo

If you continue coding directly in `vks-service`, this is the recommended endpoint direction.

### Keep current auth base

1. `/api/v1/auth/*`

### Split business APIs into route scopes

#### Tenant admin

1. `/api/v1/tenant-admin/events`
2. `/api/v1/tenant-admin/events/{eventId}`
3. `/api/v1/tenant-admin/events/search`
4. `/api/v1/tenant-admin/events/{eventId}/slots`
5. `/api/v1/tenant-admin/events/{eventId}/slots/{slotId}`

#### Customer

1. `/api/v1/customer/events`
2. `/api/v1/customer/events/{eventId}`
3. `/api/v1/customer/events/{eventId}/slots`
4. `/api/v1/customer/bookings`
5. `/api/v1/customer/bookings/{bookingId}`
6. `/api/v1/customer/bookings/{bookingId}/cancel`

#### Payment

1. `/api/v1/payments/orders`
2. `/api/v1/payments/confirm`
3. `/api/v1/payments/webhook`

### BFF-facing route idea

The external frontend contract should preferably be BFF-scoped rather than direct domain-service-scoped.

Example external routes:

#### Admin BFF external routes

1. `/bff/admin/events`
2. `/bff/admin/events/{eventId}`
3. `/bff/admin/events/{eventId}/slots`
4. `/bff/admin/dashboard`

#### Customer BFF external routes

1. `/bff/customer/events`
2. `/bff/customer/events/{eventId}`
3. `/bff/customer/slots/{slotId}`
4. `/bff/customer/bookings`
5. `/bff/customer/bookings/{bookingId}`
6. `/bff/customer/payment/checkout`
7. `/bff/customer/confirmation/{bookingId}`

Internally, those BFF routes can fan into `vks-service` routes until the system is split into separate services.

---

## 14A. Example Screen-to-BFF-to-Service Mapping

This section shows how actual UI screens should call the system.

### Tenant admin event management screen

1. UI calls `GET /bff/admin/events`
2. Admin BFF calls `GET /api/v1/tenant-admin/events`
3. `vks-service` returns tenant-scoped events
4. Admin BFF may enrich or paginate for UI

### Tenant admin slot management screen

1. UI calls `GET /bff/admin/events/{eventId}/slots`
2. Admin BFF calls `GET /api/v1/tenant-admin/events/{eventId}/slots`
3. `vks-service` returns slots

### Customer event discovery screen

1. UI calls `GET /bff/customer/events`
2. Customer BFF calls `GET /api/v1/customer/events`
3. `vks-service` returns customer-visible events
4. BFF shapes cards, labels, or grouped responses if needed

### Customer booking creation screen

1. UI calls `POST /bff/customer/bookings`
2. Customer BFF calls `POST /api/v1/customer/bookings`
3. `vks-service` creates booking draft or payment-pending booking
4. Customer BFF returns booking summary

### Customer payment screen

1. UI calls `POST /bff/customer/payment/checkout`
2. Customer BFF may first validate booking state
3. Customer BFF calls `POST /api/v1/payments/orders`
4. payment order details are returned to UI

### Confirmation screen

1. UI calls `GET /bff/customer/confirmation/{bookingId}`
2. Customer BFF fetches booking plus payment state
3. BFF returns a single confirmation payload for the UI

---

---

## 15. What You Can Code Immediately

These are the safest next items to implement right away.

1. JWT claims with `tenant_id` and `role`
2. role-based route separation
3. tenant_id field in event, slot, booking entities
4. global exception handler
5. booking state enum expansion
6. customer-facing booking API routes
7. payment order entity and API skeleton

---

## 16. Summary

`vks-service` is already a useful base, but it is still a consolidated application with prototype-level booking behavior.

To align it with the `Documents-DB` architecture, the biggest required changes are:
1. proper tenant and role-aware auth
2. tenant-admin vs customer route separation
3. booking state-machine instead of direct confirm-on-create
4. slot inventory lock design
5. payment lifecycle integration
6. async notification flow
7. BFF-first frontend integration instead of direct UI-to-domain-service coupling

If you use this document while coding, the practical order is:
1. secure and scope the platform correctly
2. harden booking behavior
3. add payment and notification flow
4. only then split into separate deployable services if needed