package com.vks.interfaces.bookings.service;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import com.vks.interfaces.bookings.model.BookingResponse;
import com.vks.interfaces.bookings.repository.BookingRepository;
import com.vks.interfaces.eventcatalog.entity.EventEntity;
import com.vks.interfaces.eventcatalog.repository.EventRepository;
import com.vks.interfaces.slot.entity.SlotEntity;
import com.vks.interfaces.slot.repository.SlotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

    private static final String USERNAME = "9876543210";

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SlotRepository slotRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    @BeforeEach
    void setUpAuthentication() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(USERNAME, null));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createBookingSavesConfirmedBookingWithSnapshotPrice() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity savedBooking = createBooking(slot, BookingStatus.CONFIRMED);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventId(slotId, eventId)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsBySlotSlotIdAndBookedByAndStatus(slotId, USERNAME, BookingStatus.CONFIRMED))
                .thenReturn(false);
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(savedBooking);

        BookingResponse response = bookingService.createBooking(eventId, slotId);

        ArgumentCaptor<BookingEntity> bookingCaptor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(bookingCaptor.capture());
        BookingEntity persisted = bookingCaptor.getValue();

        assertEquals(slot, persisted.getSlot());
        assertEquals(USERNAME, persisted.getBookedBy());
        assertEquals(slot.getPrice(), persisted.getPriceAtBooking());
        assertEquals(BookingStatus.CONFIRMED, persisted.getStatus());

        assertEquals(savedBooking.getBookingId(), response.getBookingId());
        assertEquals(eventId, response.getEventId());
        assertEquals(slotId, response.getSlotId());
        assertEquals(slot.getPrice(), response.getPriceAtBooking());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void createBookingThrowsWhenConfirmedBookingAlreadyExists() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(slotRepository.findBySlotIdAndEventEventId(slotId, eventId)).thenReturn(Optional.of(slot));
        when(bookingRepository.existsBySlotSlotIdAndBookedByAndStatus(slotId, USERNAME, BookingStatus.CONFIRMED))
                .thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> bookingService.createBooking(eventId, slotId));

        assertEquals("Booking already exists for this slot and user", exception.getMessage());
        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    @Test
    void listMyBookingsReturnsMappedBookingsForAuthenticatedUser() {
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity booking = createBooking(slot, BookingStatus.CONFIRMED);

        when(bookingRepository.findByBookedByOrderByCreatedAtDesc(USERNAME)).thenReturn(List.of(booking));

        List<BookingResponse> responses = bookingService.listMyBookings();

        assertEquals(1, responses.size());
        BookingResponse response = responses.getFirst();
        assertEquals(booking.getBookingId(), response.getBookingId());
        assertEquals(USERNAME, response.getBookedBy());
        assertEquals(eventId, response.getEventId());
        assertEquals(slotId, response.getSlotId());
        assertEquals(slot.getPrice(), response.getPriceAtBooking());
        assertEquals(BookingStatus.CONFIRMED, response.getStatus());
    }

    @Test
    void cancelBookingMarksConfirmedBookingAsCancelled() {
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity booking = createBooking(slot, BookingStatus.CONFIRMED);
        booking.setBookingId(bookingId);

        when(bookingRepository.findByBookingIdAndBookedBy(bookingId, USERNAME)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        verify(bookingRepository).save(booking);
    }

    @Test
    void cancelBookingReturnsExistingBookingWhenAlreadyCancelled() {
        UUID bookingId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        EventEntity event = createEvent(eventId);
        SlotEntity slot = createSlot(event, slotId);
        BookingEntity booking = createBooking(slot, BookingStatus.CANCELLED);
        booking.setBookingId(bookingId);

        when(bookingRepository.findByBookingIdAndBookedBy(bookingId, USERNAME)).thenReturn(Optional.of(booking));

        BookingResponse response = bookingService.cancelBooking(bookingId);

        assertEquals(BookingStatus.CANCELLED, response.getStatus());
        assertNotNull(response.getUpdatedAt());
        verify(bookingRepository, never()).save(any(BookingEntity.class));
    }

    private EventEntity createEvent(UUID eventId) {
        EventEntity event = new EventEntity();
        event.setEventId(eventId);
        event.setEventName("Tech Summit");
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
        return slot;
    }

    private BookingEntity createBooking(SlotEntity slot, BookingStatus status) {
        BookingEntity booking = new BookingEntity();
        booking.setBookingId(UUID.randomUUID());
        booking.setSlot(slot);
        booking.setBookedBy(USERNAME);
        booking.setPriceAtBooking(slot.getPrice());
        booking.setStatus(status);
        booking.setCreatedAt(LocalDateTime.of(2025, 7, 15, 16, 0));
        booking.setUpdatedAt(LocalDateTime.of(2025, 7, 15, 16, 0));
        return booking;
    }
}