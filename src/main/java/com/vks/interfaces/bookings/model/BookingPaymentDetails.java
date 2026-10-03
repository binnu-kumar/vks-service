package com.vks.interfaces.bookings.model;

import com.vks.interfaces.bookings.entity.BookingStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingPaymentDetails(
        UUID bookingId,
        BigDecimal amount,
        BookingStatus status
) {}
