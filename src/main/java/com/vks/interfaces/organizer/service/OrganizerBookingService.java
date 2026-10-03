package com.vks.interfaces.organizer.service;

import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.organizer.model.SlotAvailabilityResponse;

import java.util.List;
import java.util.UUID;

public interface OrganizerBookingService {

    List<BookingResponse> listBookings(UUID eventId, UUID slotId, BookingStatus status);

    List<BookingResponse> listAttendees(UUID eventId);

    List<SlotAvailabilityResponse> getAvailability(UUID eventId);
}
