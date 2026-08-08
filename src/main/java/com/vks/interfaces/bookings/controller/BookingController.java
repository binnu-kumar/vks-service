package com.vks.interfaces.bookings.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.bookings.model.BookingRequest;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_CUSTOMER)
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping(ApiEndpoints.BOOKINGS)
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookingService.createBooking(request));
    }

    @GetMapping(ApiEndpoints.MY_BOOKINGS)
    public ResponseEntity<List<BookingResponse>> listMyBookings() {
        return ResponseEntity.ok(bookingService.listMyBookings());
    }

    @PatchMapping(ApiEndpoints.BOOKINGS_CANCEL)
    public ResponseEntity<BookingResponse> cancelBooking(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingId));
    }
}