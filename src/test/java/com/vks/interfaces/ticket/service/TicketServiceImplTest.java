package com.vks.interfaces.ticket.service;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.ticket.entity.TicketEntity;
import com.vks.interfaces.ticket.entity.TicketStatus;
import com.vks.interfaces.ticket.repository.TicketRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final AuthenticatedUser CUSTOMER = new AuthenticatedUser(
            "42", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes());

    @Mock private TicketRepository ticketRepository;
    @Mock private SecurityContextService securityContextService;
    @Mock private QrCodeService qrCodeService;

    @InjectMocks private TicketServiceImpl ticketService;

    @Test
    void createTickets_createsOneTicketPerBookingQuantity() {
        BookingEntity booking = booking(2);
        when(ticketRepository.findByBookingIdAndTenantIdOrderByCreatedAtAsc(
                booking.getBookingId(), booking.getTenantId())).thenReturn(List.of());
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(qrCodeService.generateBase64(org.mockito.ArgumentMatchers.anyString())).thenReturn("base64-qr");

        var tickets = ticketService.createTickets(booking);

        assertEquals(2, tickets.size());
        assertEquals("base64-qr", tickets.getFirst().getQrCodeBase64());
        assertEquals(TicketStatus.ACTIVE, tickets.getFirst().getStatus());
        verify(ticketRepository).saveAll(anyList());
    }

    @Test
    void checkIn_changesActiveTicketToCheckedIn() {
        TicketEntity ticket = new TicketEntity();
        ticket.setTicketId(UUID.randomUUID());
        ticket.setTicketNumber("TKT-123");
        ticket.setBookingId(UUID.randomUUID());
        ticket.setEventId(UUID.randomUUID());
        ticket.setSlotId(UUID.randomUUID());
        ticket.setTenantId(CUSTOMER.tenantId());
        ticket.setCustomerId(CUSTOMER.userId());
        ticket.setQrPayload("vks://ticket/TKT-123");
        ticket.setStatus(TicketStatus.ACTIVE);

        when(securityContextService.currentUser()).thenReturn(new AuthenticatedUser(
                "admin-1", "admin", CUSTOMER.tenantId(), UserRole.TENANT_ADMIN, UserRole.TENANT_ADMIN.defaultScopes()));
        when(ticketRepository.findByTicketNumberAndTenantId("TKT-123", CUSTOMER.tenantId()))
                .thenReturn(Optional.of(ticket));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(qrCodeService.generateBase64("vks://ticket/TKT-123")).thenReturn("base64-qr");

        var response = ticketService.checkIn("TKT-123");

        assertEquals(TicketStatus.CHECKED_IN, response.getStatus());
        verify(ticketRepository).save(ticket);
    }

    private BookingEntity booking(int quantity) {
        EventEntity event = new EventEntity();
        event.setEventId(UUID.randomUUID());
        event.setTenantId(CUSTOMER.tenantId());
        SlotEntity slot = new SlotEntity();
        slot.setSlotId(UUID.randomUUID());
        slot.setEvent(event);
        slot.setSlotDate(LocalDate.of(2026, 10, 10));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 0));
        slot.setPrice(new BigDecimal("499.00"));
        BookingEntity booking = new BookingEntity();
        booking.setBookingId(UUID.randomUUID());
        booking.setSlot(slot);
        booking.setBookedBy(CUSTOMER.userId());
        booking.setTenantId(CUSTOMER.tenantId());
        booking.setQuantity(quantity);
        booking.setPriceAtBooking(slot.getPrice());
        booking.setStatus(BookingStatus.CONFIRMED);
        return booking;
    }
}
