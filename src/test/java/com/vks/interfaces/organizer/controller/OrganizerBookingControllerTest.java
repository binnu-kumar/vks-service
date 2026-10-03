package com.vks.interfaces.organizer.controller;

import com.vks.interfaces.organizer.model.SlotAvailabilityResponse;
import com.vks.interfaces.organizer.service.OrganizerBookingService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrganizerBookingController.class)
@Import(SecurityConfig.class)
class OrganizerBookingControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OrganizerBookingService organizerBookingService;
    @MockitoBean private JwtUtil jwtUtil;

    @Test
    void availabilityEndpointReturnsRemainingCapacityForAdmin() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(jwtUtil.validateToken("admin-token")).thenReturn(true);
        when(jwtUtil.isAccessToken("admin-token")).thenReturn(true);
        when(jwtUtil.extractAuthenticatedUser("admin-token")).thenReturn(new AuthenticatedUser(
                "1", "admin", "tenant-123", UserRole.TENANT_ADMIN, UserRole.TENANT_ADMIN.defaultScopes()));
        when(organizerBookingService.getAvailability(eventId)).thenReturn(List.of(new SlotAvailabilityResponse(
                UUID.randomUUID(), eventId, LocalDate.of(2026, 10, 10), LocalTime.of(9, 0),
                LocalTime.of(10, 0), new BigDecimal("499.00"), 10, 3, 7)));

        mockMvc.perform(get("/api/v1/tenant-admin/events/{eventId}/availability", eventId)
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].booked").value(3))
                .andExpect(jsonPath("$[0].available").value(7));
    }
}
