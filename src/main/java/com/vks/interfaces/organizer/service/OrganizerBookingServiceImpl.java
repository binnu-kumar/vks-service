package com.vks.interfaces.organizer.service;

import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.repository.BookingRepository;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.organizer.model.SlotAvailabilityResponse;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.repository.SlotRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizerBookingServiceImpl implements OrganizerBookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES = List.of(
            BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED);

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SlotRepository slotRepository;
    private final SecurityContextService securityContextService;

    @Override
    public List<BookingResponse> listBookings(UUID eventId, UUID slotId, BookingStatus status) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        return bookingRepository.findForTenantAdmin(currentUser.tenantId(), eventId, slotId, status)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<BookingResponse> listAttendees(UUID eventId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        validateEvent(eventId, currentUser.tenantId());
        return bookingRepository.findForTenantAdmin(
                        currentUser.tenantId(), eventId, null, BookingStatus.CONFIRMED)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<SlotAvailabilityResponse> getAvailability(UUID eventId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        validateEvent(eventId, currentUser.tenantId());
        return slotRepository.findByEventEventIdAndEventTenantId(eventId, currentUser.tenantId())
                .stream().map(this::toAvailability).toList();
    }

    private SlotAvailabilityResponse toAvailability(SlotEntity slot) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        int booked = bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(
                        slot.getSlotId(), currentUser.tenantId(), ACTIVE_STATUSES)
                .stream()
                .filter(booking -> booking.getExpiresAt() == null || booking.getExpiresAt().isAfter(LocalDateTime.now()))
                .mapToInt(booking -> booking.getQuantity() == null ? 1 : booking.getQuantity())
                .sum();
        return new SlotAvailabilityResponse(
                slot.getSlotId(), slot.getEvent().getEventId(), slot.getSlotDate(), slot.getStartTime(),
                slot.getEndTime(), slot.getPrice(), slot.getCapacity(), booked, Math.max(0, slot.getCapacity() - booked));
    }

    private EventEntity validateEvent(UUID eventId, String tenantId) {
        return eventRepository.findByEventIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    private BookingResponse toResponse(BookingEntity booking) {
        SlotEntity slot = booking.getSlot();
        return new BookingResponse(
                booking.getBookingId(), slot.getEvent().getEventId(), slot.getSlotId(), booking.getBookedBy(),
                booking.getQuantity(), slot.getSlotDate(), slot.getStartTime(), slot.getEndTime(),
                booking.getPriceAtBooking(), booking.getStatus(), booking.getExpiresAt(),
                booking.getCreatedAt(), booking.getUpdatedAt());
    }
}
