package com.vks.interfaces.ticket.controller;

import com.vks.interfaces.ticket.entity.TicketStatus;
import com.vks.interfaces.ticket.model.TicketResponse;
import com.vks.interfaces.ticket.service.TicketService;
import com.vks.security.AuthenticatedUser;
import com.vks.security.JwtUtil;
import com.vks.security.SecurityConfig;
import com.vks.security.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(SecurityConfig.class)
class TicketControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TicketService ticketService;
    @MockitoBean private JwtUtil jwtUtil;

    @Test
    void customerCanListIssuedTickets() throws Exception {
        UUID bookingId = UUID.randomUUID();
        when(jwtUtil.validateToken("customer-token")).thenReturn(true);
        when(jwtUtil.isAccessToken("customer-token")).thenReturn(true);
        when(jwtUtil.extractAuthenticatedUser("customer-token")).thenReturn(new AuthenticatedUser(
                "42", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes()));
        when(ticketService.getBookingTickets(bookingId)).thenReturn(List.of(new TicketResponse(
                UUID.randomUUID(), "TKT-123", bookingId, UUID.randomUUID(), UUID.randomUUID(),
                "vks://ticket/TKT-123", "base64-qr", TicketStatus.ACTIVE, null, null)));

        mockMvc.perform(get("/api/v1/customer/bookings/{bookingId}/tickets", bookingId)
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticketNumber").value("TKT-123"))
                .andExpect(jsonPath("$[0].qrCodeBase64").value("base64-qr"));
    }
}
