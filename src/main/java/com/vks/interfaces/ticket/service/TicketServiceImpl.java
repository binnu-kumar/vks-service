package com.vks.interfaces.ticket.service;

import com.vks.common.exception.ConflictException;
import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.ticket.entity.TicketEntity;
import com.vks.interfaces.ticket.entity.TicketStatus;
import com.vks.interfaces.ticket.model.TicketResponse;
import com.vks.interfaces.ticket.repository.TicketRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final SecurityContextService securityContextService;
    private final QrCodeService qrCodeService;

    @Override
    @Transactional
    public List<TicketResponse> createTickets(BookingEntity booking) {
        List<TicketEntity> existing = ticketRepository.findByBookingIdAndTenantIdOrderByCreatedAtAsc(
                booking.getBookingId(), booking.getTenantId());
        if (!existing.isEmpty()) {
            return existing.stream().map(this::toResponse).toList();
        }

        List<TicketEntity> tickets = new ArrayList<>();
        for (int index = 0; index < booking.getQuantity(); index++) {
            TicketEntity ticket = new TicketEntity();
            String ticketNumber = "TKT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
            ticket.setTicketNumber(ticketNumber);
            ticket.setBookingId(booking.getBookingId());
            ticket.setEventId(booking.getSlot().getEvent().getEventId());
            ticket.setSlotId(booking.getSlot().getSlotId());
            ticket.setTenantId(booking.getTenantId());
            ticket.setCustomerId(booking.getBookedBy());
            ticket.setQrPayload("vks://ticket/" + ticketNumber);
            ticket.setStatus(TicketStatus.ACTIVE);
            tickets.add(ticket);
        }
        return ticketRepository.saveAll(tickets).stream().map(this::toResponse).toList();
    }

    @Override
    public List<TicketResponse> listMyTickets() {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        return ticketRepository.findByCustomerIdAndTenantIdOrderByCreatedAtDesc(
                        currentUser.userId(), currentUser.tenantId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<TicketResponse> getBookingTickets(UUID bookingId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        return ticketRepository.findByBookingIdAndTenantIdOrderByCreatedAtAsc(bookingId, currentUser.tenantId())
                .stream()
                .filter(ticket -> ticket.getCustomerId().equals(currentUser.userId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse checkIn(String ticketNumber) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        TicketEntity ticket = ticketRepository.findByTicketNumberAndTenantId(ticketNumber, currentUser.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found: " + ticketNumber));
        if (ticket.getStatus() == TicketStatus.CHECKED_IN) {
            return toResponse(ticket);
        }
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Ticket cannot be checked in from status: " + ticket.getStatus());
        }
        ticket.setStatus(TicketStatus.CHECKED_IN);
        return toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional
    public void cancelTickets(UUID bookingId, String tenantId) {
        List<TicketEntity> tickets = ticketRepository.findByBookingIdAndTenantIdOrderByCreatedAtAsc(bookingId, tenantId);
        tickets.forEach(ticket -> {
            if (ticket.getStatus() == TicketStatus.ACTIVE) {
                ticket.setStatus(TicketStatus.CANCELLED);
            }
        });
        ticketRepository.saveAll(tickets);
    }

    private TicketResponse toResponse(TicketEntity ticket) {
        return new TicketResponse(
                ticket.getTicketId(),
                ticket.getTicketNumber(),
                ticket.getBookingId(),
                ticket.getEventId(),
                ticket.getSlotId(),
                ticket.getQrPayload(),
                qrCodeService.generateBase64(ticket.getQrPayload()),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
