package com.vks.interfaces.bookings.repository;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    boolean existsBySlotSlotIdAndBookedByAndStatus(UUID slotId, String bookedBy, BookingStatus status);

    List<BookingEntity> findByBookedByOrderByCreatedAtDesc(String bookedBy);

    Optional<BookingEntity> findByBookingIdAndBookedBy(UUID bookingId, String bookedBy);
}