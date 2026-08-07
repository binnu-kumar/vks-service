package com.vks.interfaces.bookings.service;

import com.vks.interfaces.bookings.model.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(UUID eventId, UUID slotId);

    List<BookingResponse> listMyBookings();

    BookingResponse cancelBooking(UUID bookingId);
}