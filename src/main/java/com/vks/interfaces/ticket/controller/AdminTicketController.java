package com.vks.interfaces.ticket.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.ticket.model.TicketResponse;
import com.vks.interfaces.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.BASE_TENANT_ADMIN)
@RequiredArgsConstructor
public class AdminTicketController {

    private final TicketService ticketService;

    @PatchMapping(ApiEndpoints.ADMIN_TICKET_CHECK_IN)
    public ResponseEntity<TicketResponse> checkIn(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(ticketService.checkIn(ticketNumber));
    }
}
