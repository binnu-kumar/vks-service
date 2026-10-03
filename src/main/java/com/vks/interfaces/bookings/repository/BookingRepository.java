package com.vks.interfaces.bookings.repository;

import com.vks.interfaces.bookings.entity.BookingEntity;
import com.vks.interfaces.bookings.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {

    boolean existsBySlotSlotIdAndBookedByAndTenantIdAndStatusIn(
            UUID slotId,
            String bookedBy,
            String tenantId,
            Collection<BookingStatus> statuses
    );

    List<BookingEntity> findByBookedByAndTenantIdOrderByCreatedAtDesc(String bookedBy, String tenantId);

    Optional<BookingEntity> findByBookingIdAndBookedByAndTenantId(UUID bookingId, String bookedBy, String tenantId);

    Optional<BookingEntity> findByBookingIdAndTenantId(UUID bookingId, String tenantId);

    Optional<BookingEntity> findByTenantIdAndBookedByAndIdempotencyKey(String tenantId, String bookedBy, String idempotencyKey);

    List<BookingEntity> findBySlotSlotIdAndTenantIdAndStatusIn(UUID slotId, String tenantId, Collection<BookingStatus> statuses);

    @Query("SELECT b FROM BookingEntity b WHERE b.tenantId = :tenantId"
            + " AND (:eventId IS NULL OR b.slot.event.eventId = :eventId)"
            + " AND (:slotId IS NULL OR b.slot.slotId = :slotId)"
            + " AND (:status IS NULL OR b.status = :status)"
            + " ORDER BY b.createdAt DESC")
    List<BookingEntity> findForTenantAdmin(
            @Param("tenantId") String tenantId,
            @Param("eventId") UUID eventId,
            @Param("slotId") UUID slotId,
            @Param("status") BookingStatus status);

    @Query("SELECT b FROM BookingEntity b WHERE b.status IN :statuses AND b.expiresAt IS NOT NULL AND b.expiresAt < :now")
    List<BookingEntity> findExpiredPendingBookings(@Param("statuses") Collection<BookingStatus> statuses, @Param("now") LocalDateTime now);
}