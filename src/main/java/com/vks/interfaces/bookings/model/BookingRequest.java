package com.vks.interfaces.bookings.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class BookingRequest {

    @NotNull(message = "Event id is required")
    private UUID eventId;

    @NotNull(message = "Slot id is required")
    private UUID slotId;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}