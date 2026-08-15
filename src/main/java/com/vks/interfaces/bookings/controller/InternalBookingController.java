package com.vks.interfaces.bookings.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_INTERNAL)
@RequiredArgsConstructor
public class InternalBookingController {

    private final BookingService bookingService;

    @PatchMapping(ApiEndpoints.BOOKING_CONFIRM_INTERNAL)
    public ResponseEntity<BookingResponse> confirmBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.confirmBooking(bookingId));
    }
}
