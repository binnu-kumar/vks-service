package com.vks.interfaces.ticket.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.ticket.model.TicketResponse;
import com.vks.interfaces.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ApiEndpoints.BASE_CUSTOMER)
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping(ApiEndpoints.CUSTOMER_TICKETS)
    public ResponseEntity<List<TicketResponse>> listMyTickets() {
        return ResponseEntity.ok(ticketService.listMyTickets());
    }

    @GetMapping(ApiEndpoints.CUSTOMER_BOOKING_TICKETS)
    public ResponseEntity<List<TicketResponse>> getBookingTickets(@PathVariable UUID bookingId) {
        return ResponseEntity.ok(ticketService.getBookingTickets(bookingId));
    }
}
