package com.vks.interfaces.organizer.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record SlotAvailabilityResponse(
        UUID slotId,
        UUID eventId,
        LocalDate slotDate,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal price,
        int capacity,
        int booked,
        int available
) {}
