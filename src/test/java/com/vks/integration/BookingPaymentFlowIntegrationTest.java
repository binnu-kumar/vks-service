package com.vks.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Enable when booking and payment test environment with dependent services is available")
class BookingPaymentFlowIntegrationTest {

    @Test
    void bookingToPaymentToNotificationFlow_shouldPassEndToEnd() {
        // Skeleton:
        // 1. Authenticate as customer and create booking with idempotency key.
        // 2. Create payment order against booking.
        // 3. Confirm payment and verify booking status transition.
        // 4. Verify notification event is emitted/consumed.
    }
}
