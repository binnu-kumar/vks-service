package com.vks.interfaces.bookings.service;

import com.vks.common.exception.ConflictException;
import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.events.BookingEvent;
import com.vks.interfaces.bookings.events.BookingEventPublisher;
import com.vks.interfaces.bookings.model.BookingRequest;
import com.vks.interfaces.bookings.model.BookingResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    private static final AuthenticatedUser CURRENT_USER = new AuthenticatedUser(
            "42", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes());

    @Mock private BookingRepository bookingRepository;
    @Mock private EventRepository eventRepository;
    @Mock private SlotRepository slotRepository;
    @Mock private BookingEventPublisher bookingEventPublisher;
    @Mock private SecurityContextService securityContextService;

    @InjectMocks private BookingServiceImpl bookingService;

    @Test
    void createBooking_savesPendingBookingWithSnapshotPriceAndExpiry() {
        ReflectionTestUtils.setField(bookingService, "holdDurationMinutes", 15L);
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingRequest request = bookingRequest(eventId, slotId, 1);
        BookingEntity savedBooking = createBooking(slot, BookingStatus.PAYMENT_PENDING, 1);
        savedBooking.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(eventRepository.findByEventIdAndTenantId(eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventIdAndEventTenantId(slotId, eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(slot));
        when(bookingRepository.findByTenantIdAndBookedByAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.existsBySlotSlotIdAndBookedByAndTenantIdAndStatusIn(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(any(), any(), any())).thenReturn(List.of());
        when(bookingRepository.save(any())).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(request);

        ArgumentCaptor<BookingEntity> captor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(captor.capture());
        BookingEntity persisted = captor.getValue();

        assertEquals(slot, persisted.getSlot());
        assertEquals(CURRENT_USER.username(), persisted.getBookedBy());
        assertEquals(CURRENT_USER.tenantId(), persisted.getTenantId());
        assertEquals(slot.getPrice(), persisted.getPriceAtBooking());
        assertEquals(BookingStatus.PAYMENT_PENDING, persisted.getStatus());
        assertNotNull(persisted.getExpiresAt());
        assertEquals(1, persisted.getQuantity());
        assertEquals(savedBooking.getBookingId(), response.getBookingId());
        verify(bookingEventPublisher).publish(any(BookingEvent.class));
    }

    @Test
    void createBooking_throwsWhenSlotCapacityReached() {
        ReflectionTestUtils.setField(bookingService, "holdDurationMinutes", 15L);
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        slot.setCapacity(1);
        BookingRequest request = bookingRequest(eventId, slotId, 1);
        BookingEntity existing = createBooking(slot, BookingStatus.PAYMENT_PENDING, 1);
        existing.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(eventRepository.findByEventIdAndTenantId(eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventIdAndEventTenantId(slotId, eventId, CURRENT_USER.tenantId())).thenReturn(Optional.of(slot));
        when(bookingRepository.findByTenantIdAndBookedByAndIdempotencyKey(any(), any(), any())).thenReturn(Optional.empty());
        when(bookingRepository.existsBySlotSlotIdAndBookedByAndTenantIdAndStatusIn(any(), any(), any(), any())).thenReturn(false);
        when(bookingRepository.findBySlotSlotIdAndTenantIdAndStatusIn(any(), any(), any())).thenReturn(List.of(existing));

        ConflictException ex = assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
        assertEquals("Slot capacity has been reached", ex.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_throwsOnDuplicateIdempotencyKey() {
        ReflectionTestUtils.setField(bookingService, "holdDurationMinutes", 15L);
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        BookingRequest request = bookingRequest(eventId, slotId, 1);

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(bookingRepository.findByTenantIdAndBookedByAndIdempotencyKey(any(), any(), any()))
                .thenReturn(Optional.of(new BookingEntity()));

        assertThrows(ConflictException.class, () -> bookingService.createBooking(request));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void listMyBookings_returnsMappedBookings() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity booking = createBooking(slot, BookingStatus.PAYMENT_PENDING, 2);

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(bookingRepository.findByBookedByAndTenantIdOrderByCreatedAtDesc(CURRENT_USER.userId(), CURRENT_USER.tenantId()))
                .thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.listMyBookings();

        assertEquals(1, responses.size());
        assertEquals(booking.getBookingId(), responses.getFirst().getBookingId());
        assertEquals(2, responses.getFirst().getQuantity());
    }

    @Test
    void cancelBooking_marksAsCancelledAndPublishesEvent() {
        UUID bookingId = UUID.randomUUID();
        EventEntity event = createEvent(UUID.randomUUID());
        SlotEntity slot = createSlot(event, UUID.randomUUID());
        BookingEntity booking = createBooking(slot, BookingStatus.PAYMENT_PENDING, 1);
        booking.setBookingId(bookingId);

        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(bookingRepository.findByBookingIdAndBookedByAndTenantId(bookingId, CURRENT_USER.userId(), CURRENT_USER.tenantId()))
                .thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(bookingEventPublisher).publish(any(BookingEvent.class));
    }

    @Test
    void confirmBooking_transitionsToConfirmedAndPublishesEvent() {
        UUID bookingId = UUID.randomUUID();
        EventEntity event = createEvent(UUID.randomUUID());
        SlotEntity slot = createSlot(event, UUID.randomUUID());
        BookingEntity booking = createBooking(slot, BookingStatus.PAYMENT_PENDING, 1);
        booking.setBookingId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse response = bookingService.confirmBooking(bookingId);

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        assertNull(response.getExpiresAt());
        verify(bookingEventPublisher).publish(any(BookingEvent.class));
    }

    @Test
    void confirmBooking_alreadyConfirmed_returnsIdempotently() {
        UUID bookingId = UUID.randomUUID();
        EventEntity event = createEvent(UUID.randomUUID());
        SlotEntity slot = createSlot(event, UUID.randomUUID());
        BookingEntity booking = createBooking(slot, BookingStatus.CONFIRMED, 1);
        booking.setBookingId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.confirmBooking(bookingId);

        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void confirmBooking_fromCancelledStatus_throwsConflict() {
        UUID bookingId = UUID.randomUUID();
        EventEntity event = createEvent(UUID.randomUUID());
        SlotEntity slot = createSlot(event, UUID.randomUUID());
        BookingEntity booking = createBooking(slot, BookingStatus.CANCELLED, 1);
        booking.setBookingId(bookingId);

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(ConflictException.class, () -> bookingService.confirmBooking(bookingId));
    }

    // --- helpers ---

    private BookingRequest bookingRequest(UUID eventId, UUID slotId, int quantity) {
        BookingRequest r = new BookingRequest();
        r.setEventId(eventId);
        r.setSlotId(slotId);
        r.setQuantity(quantity);
        r.setIdempotencyKey("key-1");
        return r;
    }

    private EventEntity createEvent(UUID eventId) {
        EventEntity e = new EventEntity();
        e.setEventId(eventId);
        e.setEventName("Tech Summit");
        e.setTenantId(CURRENT_USER.tenantId());
        return e;
    }

    private SlotEntity createSlot(EventEntity event, UUID slotId) {
        SlotEntity s = new SlotEntity();
        s.setSlotId(slotId);
        s.setEvent(event);
        s.setSlotDate(LocalDate.of(2025, 9, 1));
        s.setStartTime(LocalTime.of(9, 0));
        s.setEndTime(LocalTime.of(10, 30));
        s.setPrice(new BigDecimal("499.00"));
        s.setCapacity(5);
        return s;
    }

    private BookingEntity createBooking(SlotEntity slot, BookingStatus status, int quantity) {
        BookingEntity b = new BookingEntity();
        b.setBookingId(UUID.randomUUID());
        b.setSlot(slot);
        b.setBookedBy(CURRENT_USER.userId());
        b.setTenantId(CURRENT_USER.tenantId());
        b.setIdempotencyKey("key-1");
        b.setPriceAtBooking(slot.getPrice());
        b.setQuantity(quantity);
        b.setStatus(status);
        b.setCreatedAt(LocalDateTime.of(2025, 7, 15, 16, 0));
        b.setUpdatedAt(LocalDateTime.of(2025, 7, 15, 16, 0));
        return b;
    }
}
