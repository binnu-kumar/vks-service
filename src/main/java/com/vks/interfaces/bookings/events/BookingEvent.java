package com.vks.interfaces.bookings.events;

import com.vks.interfaces.bookings.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookingEvent {

    private String eventType;
    private UUID eventId;
    private Instant occurredAt;
    private String tenantId;
    private UUID bookingId;
    private String customerId;
    private UUID slotId;
    private Integer quantity;
    private BigDecimal amount;
    private BookingStatus status;
}
