package com.vks.interfaces.bookings.service;

import com.vks.interfaces.bookings.model.BookingRequest;
import com.vks.interfaces.bookings.model.BookingResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(BookingRequest request);

    List<BookingResponse> listMyBookings();

    BookingResponse getBooking(UUID bookingId);

    BookingResponse cancelBooking(UUID bookingId);

    BookingResponse confirmBooking(UUID bookingId);
}