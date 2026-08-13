package com.vks.interfaces.bookings.model;

import jakarta.validation.constraints.Min;
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

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}