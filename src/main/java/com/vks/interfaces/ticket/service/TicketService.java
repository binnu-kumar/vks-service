package com.vks.interfaces.ticket.service;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.ticket.model.TicketResponse;

import java.util.List;
import java.util.UUID;

public interface TicketService {

    List<TicketResponse> createTickets(BookingEntity booking);

    List<TicketResponse> listMyTickets();

    List<TicketResponse> getBookingTickets(UUID bookingId);

    TicketResponse checkIn(String ticketNumber);

    void cancelTickets(UUID bookingId, String tenantId);
}
