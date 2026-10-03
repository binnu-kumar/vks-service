package com.vks.interfaces.organizer.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.organizer.model.SlotAvailabilityResponse;
import com.vks.interfaces.organizer.service.OrganizerBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_TENANT_ADMIN)
@RequiredArgsConstructor
public class OrganizerBookingController {

    private final OrganizerBookingService organizerBookingService;

    @GetMapping(ApiEndpoints.ADMIN_BOOKINGS)
    public ResponseEntity<List<BookingResponse>> listBookings(
            @RequestParam(required = false) UUID eventId,
            @RequestParam(required = false) UUID slotId,
            @RequestParam(required = false) BookingStatus status) {
        return ResponseEntity.ok(organizerBookingService.listBookings(eventId, slotId, status));
    }

    @GetMapping(ApiEndpoints.ADMIN_ATTENDEES)
    public ResponseEntity<List<BookingResponse>> listAttendees(@PathVariable UUID eventId) {
        return ResponseEntity.ok(organizerBookingService.listAttendees(eventId));
    }

    @GetMapping(ApiEndpoints.ADMIN_AVAILABILITY)
    public ResponseEntity<List<SlotAvailabilityResponse>> getAvailability(@PathVariable UUID eventId) {
        return ResponseEntity.ok(organizerBookingService.getAvailability(eventId));
    }
}
