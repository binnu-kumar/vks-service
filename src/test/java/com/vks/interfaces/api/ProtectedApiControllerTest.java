package com.vks.interfaces.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.service.BookingService;
import com.vks.interfaces.eventcatalog.model.EventResponse;
import com.vks.interfaces.eventcatalog.service.EventService;
import com.vks.interfaces.slot.model.SlotResponse;
import com.vks.interfaces.slot.service.SlotService;
import com.vks.security.JwtUtil;
import com.vks.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        com.vks.interfaces.eventcatalog.controller.EventController.class,
        com.vks.interfaces.slot.controller.SlotController.class,
        com.vks.interfaces.bookings.controller.BookingController.class
}, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.config.import="
})
@Import(SecurityConfig.class)
class ProtectedApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EventService eventService;

    @MockBean
    private SlotService slotService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void eventsEndpointIsProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listEventsReturnsOkWithToken() throws Exception {
        EventResponse response = new EventResponse(
                UUID.randomUUID(), "Tech Summit", "Conference", "Bangalore",
                LocalDateTime.of(2025, 9, 1, 9, 0), LocalDateTime.of(2025, 9, 1, 18, 0),
                "9876543210", LocalDateTime.of(2025, 7, 1, 10, 0), LocalDateTime.of(2025, 7, 1, 10, 0));
        authenticate("valid-token", "9876543210");
        when(eventService.listEvents()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/events").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventName").value("Tech Summit"));
    }

    @Test
    void createEventReturnsCreatedAndUsesAuthenticatedPrincipal() throws Exception {
        EventResponse response = new EventResponse(
                UUID.randomUUID(), "Tech Summit", "Conference", "Bangalore",
                LocalDateTime.of(2025, 9, 1, 9, 0), LocalDateTime.of(2025, 9, 1, 18, 0),
                "9876543210", LocalDateTime.of(2025, 7, 1, 10, 0), LocalDateTime.of(2025, 7, 1, 10, 0));
        authenticate("valid-token", "9876543210");
        when(eventService.createEvent(any(), eq("9876543210"))).thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"eventName\":\"Tech Summit\",\"description\":\"Conference\",\"location\":\"Bangalore\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("9876543210"));
    }

    @Test
    void createSlotReturnsCreatedWithToken() throws Exception {
        UUID eventId = UUID.randomUUID();
        SlotResponse response = new SlotResponse(
                UUID.randomUUID(), eventId, LocalDate.of(2025, 9, 1), LocalTime.of(9, 0), LocalTime.of(10, 30),
                new BigDecimal("499.00"), LocalDateTime.of(2025, 7, 1, 10, 0), LocalDateTime.of(2025, 7, 1, 10, 0));
        authenticate("valid-token", "9876543210");
        when(slotService.createSlot(eq(eventId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/{eventId}/slots", eventId)
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"slotDate\":\"2025-09-01\",\"startTime\":\"09:00:00\",\"endTime\":\"10:30:00\",\"price\":499.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(499.0));
    }

    @Test
    void createBookingReturnsCreatedWithToken() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        BookingResponse response = new BookingResponse(
                UUID.randomUUID(), eventId, slotId, "9876543210",
                LocalDate.of(2025, 9, 1), LocalTime.of(9, 0), LocalTime.of(10, 30),
                new BigDecimal("499.00"), BookingStatus.CONFIRMED,
                LocalDateTime.of(2025, 7, 1, 10, 0), LocalDateTime.of(2025, 7, 1, 10, 0));
        authenticate("valid-token", "9876543210");
        when(bookingService.createBooking(eventId, slotId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/{eventId}/slots/{slotId}/bookings", eventId, slotId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancelBookingReturnsOkWithToken() throws Exception {
        UUID bookingId = UUID.randomUUID();
        BookingResponse response = new BookingResponse(
                bookingId, UUID.randomUUID(), UUID.randomUUID(), "9876543210",
                LocalDate.of(2025, 9, 1), LocalTime.of(9, 0), LocalTime.of(10, 30),
                new BigDecimal("499.00"), BookingStatus.CANCELLED,
                LocalDateTime.of(2025, 7, 1, 10, 0), LocalDateTime.of(2025, 7, 1, 11, 0));
        authenticate("valid-token", "9876543210");
        when(bookingService.cancelBooking(bookingId)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/bookings/{bookingId}/cancel", bookingId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void deleteSlotReturnsNoContentWithToken() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        authenticate("valid-token", "9876543210");

        mockMvc.perform(delete("/api/v1/events/{eventId}/slots/{slotId}", eventId, slotId)
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNoContent());

        verify(slotService).deleteSlot(eventId, slotId);
    }

    private void authenticate(String token, String subject) {
        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.extractSubject(token)).thenReturn(subject);
    }
}