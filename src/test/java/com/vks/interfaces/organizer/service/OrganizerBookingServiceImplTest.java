package com.vks.interfaces.organizer.service;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.repository.BookingRepository;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.repository.SlotRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import com.vks.security.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerBookingServiceImplTest {

    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(
            "1", "admin", "tenant-123", UserRole.TENANT_ADMIN, UserRole.TENANT_ADMIN.defaultScopes());

    @Mock private BookingRepository bookingRepository;
    @Mock private EventRepository eventRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private SecurityContextService securityContextService;

    @InjectMocks private OrganizerBookingServiceImpl organizerService;

    @Test
    void getAvailability_returnsBookedAndRemainingCapacity() {
        UUID eventId = UUID.randomUUID();
        EventEntity event = new EventEntity();
        event.setEventId(eventId);
        event.setTenantId(ADMIN.tenantId());
        SlotEntity slot = new SlotEntity();
        slot.setSlotId(UUID.randomUUID());
        slot.setEvent(event);
        slot.setSlotDate(LocalDate.of(2026, 10, 10));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setPrice(new BigDecimal("499.00"));
        slot.setCapacity(10);
        BookingEntity booking = new BookingEntity();
        booking.setQuantity(3);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);

        when(securityContextService.currentUser()).thenReturn(ADMIN);
        when(eventRepository.findByEventIdAndTenantId(eventId, ADMIN.tenantId())).thenReturn(Optional.of(event));
        when(slotRepository.findByEventEventIdAndEventTenantId(eventId, ADMIN.tenantId())).thenReturn(List.of(slot));
        when(bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(
                slot.getSlotId(), ADMIN.tenantId(), List.of(BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED)))
                .thenReturn(List.of(booking));

        var availability = organizerService.getAvailability(eventId);

        assertEquals(1, availability.size());
        assertEquals(3, availability.getFirst().booked());
        assertEquals(7, availability.getFirst().available());
    }
}
