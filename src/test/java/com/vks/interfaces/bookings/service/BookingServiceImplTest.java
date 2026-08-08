package com.vks.interfaces.bookings.service;

import com.vks.common.EmailService;
import com.vks.common.exception.ConflictException;
import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingRequest;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.repository.BookingRepository;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.repository.SignupRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.repository.SlotRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import com.vks.security.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final AuthenticatedUser CURRENT_USER = new AuthenticatedUser(
            "42", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes());

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private SignupRepository signupRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @Test
    void createBookingSavesPendingBookingWithSnapshotPriceAndExpiry() {
        ReflectionTestUtils.setField(bookingService, "holdDurationMinutes", 15L);
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingRequest request = bookingRequest(eventId, slotId);
        BookingEntity savedBooking = createBooking(slot, BookingStatus.PAYMENT_PENDING);
        savedBooking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(eventRepository.findByEventIdAndTenantId(eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventIdAndEventTenantId(slotId, eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(slot));
        when(bookingRepository.findByTenantIdAndBookedByAndIdempotencyKey(CURRENT_USER.tenantId(), CURRENT_USER.userId(), request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(bookingRepository.existsBySlotSlotIdAndBookedByAndTenantIdAndStatusIn(slotId, CURRENT_USER.userId(), CURRENT_USER.tenantId(),
                List.of(BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED))).thenReturn(false);
        when(bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(slotId, CURRENT_USER.tenantId(),
                List.of(BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED))).thenReturn(List.of());
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(savedBooking);

        SignupEntity user = new SignupEntity();
        user.setEmailid("john@example.com");
        when(signupRepository.findById(42L)).thenReturn(Optional.of(user));

        BookingResponse response = bookingService.createBooking(request);

        ArgumentCaptor<BookingEntity> bookingCaptor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        BookingEntity persisted = bookingCaptor.getValue();

        assertEquals(slot, persisted.getSlot());
        assertEquals(CURRENT_USER.userId(), persisted.getBookedBy());
        assertEquals(CURRENT_USER.tenantId(), persisted.getTenantId());
        assertEquals(slot.getPrice(), persisted.getPriceAtBooking());
        assertEquals(BookingStatus.PAYMENT_PENDING, persisted.getStatus());
        assertNotNull(persisted.getExpiresAt());
        assertEquals(savedBooking.getBookingId(), response.getBookingId());
        assertEquals(BookingStatus.PAYMENT_PENDING, response.getStatus());
        verify(emailService).sendBookingPendingEmail("john@example.com", savedBooking.getBookingId().toString(), event.getEventName());
    }

    @Test
    void createBookingThrowsWhenSlotCapacityReached() {
        ReflectionTestUtils.setField(bookingService, "holdDurationMinutes", 15L);
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        slot.setCapacity(1);
        BookingRequest request = bookingRequest(eventId, slotId);
        BookingEntity existing = createBooking(slot, BookingStatus.PAYMENT_PENDING);
        existing.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(eventRepository.findByEventIdAndTenantId(eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventIdAndEventTenantId(slotId, eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(slot));
        when(bookingRepository.findByTenantIdAndBookedByAndIdempotencyKey(CURRENT_USER.tenantId(), CURRENT_USER.userId(), request.getIdempotencyKey()))
                .thenReturn(Optional.empty());
        when(bookingRepository.existsBySlotSlotIdAndBookedByAndTenantIdAndStatusIn(slotId, CURRENT_USER.userId(), CURRENT_USER.tenantId(),
                List.of(BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED))).thenReturn(false);
        when(bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(slotId, CURRENT_USER.tenantId(),
                List.of(BookingStatus.DRAFT, BookingStatus.PAYMENT_PENDING, BookingStatus.CONFIRMED))).thenReturn(List.of(existing));

        ConflictException exception = assertThrows(ConflictException.class, () -> bookingService.createBooking(request));

        assertEquals("Slot capacity has been reached", exception.getMessage());
        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void listMyBookingsReturnsMappedBookingsForAuthenticatedUser() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity booking = createBooking(slot, BookingStatus.PAYMENT_PENDING);

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(bookingRepository.findByBookedByAndTenantIdOrderByCreatedAtDesc(CURRENT_USER.userId(), CURRENT_USER.tenantId()))
                .thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.listMyBookings();

        assertEquals(1, responses.size());
        BookingResponse response = responses.getFirst();
        assertEquals(booking.getBookingId(), response.getBookingId());
        assertEquals(CURRENT_USER.userId(), response.getBookedBy());
        assertEquals(eventId, response.getEventId());
        assertEquals(slotId, response.getSlotId());
        assertEquals(slot.getPrice(), response.getPriceAtBooking());
    }

    @Test
    void cancelBookingMarksBookingAsCancelledAndSendsNotification() {
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity booking = createBooking(slot, BookingStatus.PAYMENT_PENDING);
        booking.setBookingId(bookingId);

        SignupEntity user = new SignupEntity();
        user.setEmailid("john@example.com");

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(bookingRepository.findByBookingIdAndBookedByAndTenantId(bookingId, CURRENT_USER.userId(), CURRENT_USER.tenantId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(signupRepository.findById(42L)).thenReturn(Optional.of(user));

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(emailService).sendBookingCancelledEmail("john@example.com", bookingId.toString(), event.getEventName());
    }

    private BookingRequest bookingRequest(UUID eventId, UUID slotId) {
        BookingRequest request = new BookingRequest();
        request.setEventId(eventId);
        request.setSlotId(slotId);
        request.setIdempotencyKey("key-1");
        return request;
    }

    private EventEntity createEvent(UUID eventId) {
        EventEntity event = new EventEntity();
        event.setEventId(eventId);
        event.setEventName("Tech Summit");
        event.setTenantId(CURRENT_USER.tenantId());
        return event;
    }

    private SlotEntity createSlot(EventEntity event, UUID slotId) {
        SlotEntity slot = new SlotEntity();
        slot.setSlotId(slotId);
        slot.setEvent(event);
        slot.setSlotDate(LocalDate.of(2025, 9, 1));
        slot.setStartTime(LocalTime.of(9, 0));
        slot.setEndTime(LocalTime.of(10, 30));
        slot.setPrice(new BigDecimal("499.00"));
        slot.setCapacity(5);
        return slot;
    }

    private BookingEntity createBooking(SlotEntity slot, BookingStatus status) {
        BookingEntity booking = new BookingEntity();
        booking.setBookingId(UUID.randomUUID());
        booking.setSlot(slot);
        booking.setBookedBy(CURRENT_USER.userId());
        booking.setTenantId(CURRENT_USER.tenantId());
        booking.setIdempotencyKey("key-1");
        booking.setPriceAtBooking(slot.getPrice());
        booking.setStatus(status);
        booking.setCreatedAt(LocalDateTime.of(2025, 7, 15, 16, 0));
        booking.setUpdatedAt(LocalDateTime.of(2025, 7, 15, 16, 0));
        return booking;
    }
}