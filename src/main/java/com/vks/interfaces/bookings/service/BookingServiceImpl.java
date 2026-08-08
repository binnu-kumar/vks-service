package com.vks.interfaces.bookings.service;

import com.vks.common.EmailService;
import com.vks.common.exception.ConflictException;
import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingRequest;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.repository.BookingRepository;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.repository.SignupRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.repository.SlotRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.DRAFT,
            BookingStatus.PAYMENT_PENDING,
            BookingStatus.CONFIRMED
    );

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final SlotRepository slotRepository;
    private final SignupRepository signupRepository;
    private final EmailService emailService;
    private final SecurityContextService securityContextService;

    @Value("${booking.hold-duration-minutes:15}")
    private long holdDurationMinutes;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Creating booking for event id: {}, slot id: {}, user: {}", request.getEventId(), request.getSlotId(), currentUser.userId());

        bookingRepository.findByTenantIdAndBookedByAndIdempotencyKey(
                currentUser.tenantId(), currentUser.userId(), request.getIdempotencyKey())
                .ifPresent(existing -> {
                    throw new ConflictException("Booking command with this idempotency key was already processed");
                });

        validateEvent(request.getEventId(), currentUser.tenantId());
        SlotEntity slot = slotRepository.findBySlotIdAndEventEventIdAndEventTenantId(
                        request.getSlotId(), request.getEventId(), currentUser.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Slot not found with id: " + request.getSlotId() + " for event id: " + request.getEventId()));

        expireStaleBookings(slot.getSlotId(), currentUser.tenantId());

        if (bookingRepository.existsBySlotSlotIdAndBookedByAndTenantIdAndStatusIn(
                request.getSlotId(), currentUser.userId(), currentUser.tenantId(), ACTIVE_BOOKING_STATUSES)) {
            throw new ConflictException("Active booking already exists for this slot and user");
        }

        long activeBookingCount = bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(
                request.getSlotId(), currentUser.tenantId(), ACTIVE_BOOKING_STATUSES)
            .stream()
            .filter(existing -> existing.getExpiresAt() == null || existing.getExpiresAt().isAfter(LocalDateTime.now()))
            .count();

        if (activeBookingCount >= slot.getCapacity()) {
            throw new ConflictException("Slot capacity has been reached");
        }

        BookingEntity booking = new BookingEntity();
        booking.setSlot(slot);
        booking.setBookedBy(currentUser.username());
        booking.setTenantId(currentUser.tenantId());
        booking.setIdempotencyKey(request.getIdempotencyKey());
        booking.setPriceAtBooking(slot.getPrice());
        booking.setStatus(BookingStatus.PAYMENT_PENDING);
        booking.setExpiresAt(LocalDateTime.now().plusMinutes(holdDurationMinutes));

        BookingEntity savedBooking = bookingRepository.save(booking);
        notifyBookingPending(savedBooking);
        return toResponse(savedBooking);
    }

    @Override
    public List<BookingResponse> listMyBookings() {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Listing bookings for user: {}", currentUser.userId());
        return bookingRepository.findByBookedByAndTenantIdOrderByCreatedAtDesc(currentUser.userId(), currentUser.tenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(UUID bookingId) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Cancelling booking id: {} for user: {}", bookingId, currentUser.userId());

        BookingEntity booking = bookingRepository.findByBookingIdAndBookedByAndTenantId(
                bookingId, currentUser.userId(), currentUser.tenantId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return toResponse(booking);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setExpiresAt(null);
        BookingEntity savedBooking = bookingRepository.save(booking);
        notifyBookingCancelled(savedBooking);
        return toResponse(savedBooking);
    }

    private void validateEvent(UUID eventId, String tenantId) {
        eventRepository.findByEventIdAndTenantId(eventId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
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
                booking.getExpiresAt(),
                booking.getCreatedAt(),
                booking.getUpdatedAt()
        );
    }

    private void expireStaleBookings(UUID slotId, String tenantId) {
        List<BookingEntity> activeBookings = bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(
                slotId, tenantId, ACTIVE_BOOKING_STATUSES);

        for (BookingEntity activeBooking : activeBookings) {
            if (activeBooking.getExpiresAt() != null && activeBooking.getExpiresAt().isBefore(LocalDateTime.now())) {
                activeBooking.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(activeBooking);
            }
        }
    }

    private void notifyBookingPending(BookingEntity booking) {
        SignupEntity user = signupRepository.findById(Long.valueOf(booking.getBookedBy())).orElse(null);
        if (user != null && user.getEmailid() != null) {
            emailService.sendBookingPendingEmail(
                    user.getEmailid(),
                    booking.getBookingId().toString(),
                    booking.getSlot().getEvent().getEventName()
            );
        }
    }

    private void notifyBookingCancelled(BookingEntity booking) {
        SignupEntity user = signupRepository.findById(Long.valueOf(booking.getBookedBy())).orElse(null);
        if (user != null && user.getEmailid() != null) {
            emailService.sendBookingCancelledEmail(
                    user.getEmailid(),
                    booking.getBookingId().toString(),
                    booking.getSlot().getEvent().getEventName()
            );
        }
    }
}