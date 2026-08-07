package com.vks.interfaces.bookings.service;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.repository.BookingRepository;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.repository.SlotRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SlotRepository slotRepository;

    @Override
    @Transactional
    public BookingResponse createBooking(UUID eventId, UUID slotId) {
        String bookedBy = getAuthenticatedUsername();
        log.info("Creating booking for event id: {}, slot id: {}, user: {}", eventId, slotId, bookedBy);

        validateEvent(eventId);
        SlotEntity slot = slotRepository.findBySlotIdAndEventEventId(slotId, eventId)
                .orElseThrow(() -> new RuntimeException(
                        "Slot not found with id: " + slotId + " for event id: " + eventId));

        if (bookingRepository.existsBySlotSlotIdAndBookedByAndStatus(slotId, bookedBy, BookingStatus.CONFIRMED)) {
            throw new RuntimeException("Booking already exists for this slot and user");
        }

        BookingEntity booking = new BookingEntity();
        booking.setSlot(slot);
        booking.setBookedBy(bookedBy);
        booking.setPriceAtBooking(slot.getPrice());
        booking.setStatus(BookingStatus.CONFIRMED);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    public List<BookingResponse> listMyBookings() {
        String bookedBy = getAuthenticatedUsername();
        log.info("Listing bookings for user: {}", bookedBy);
        return bookingRepository.findByBookedByOrderByCreatedAtDesc(bookedBy)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        String bookedBy = getAuthenticatedUsername();
        log.info("Cancelling booking id: {} for user: {}", bookingId, bookedBy);

        BookingEntity booking = bookingRepository.findByBookingIdAndBookedBy(bookingId, bookedBy)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    private void validateEvent(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found with id: " + eventId));
    }

    private String getAuthenticatedUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Authenticated user not found in security context");
        }
        return authentication.getName();
    }

    private BookingResponse toResponse(BookingEntity booking) {
        SlotEntity slot = booking.getSlot();
        return new BookingResponse(
                booking.getBookingId(),
                slot.getEvent().getEventId(),
                slot.getSlotId(),
                booking.getBookedBy(),
                slot.getSlotDate(),
                slot.getStartTime(),
                slot.getEndTime(),
                booking.getPriceAtBooking(),
                booking.getStatus(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }
}