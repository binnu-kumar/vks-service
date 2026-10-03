package com.vks.interfaces.slot.repository;

import com.vks.interfaces.slot.entity.SlotEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<SlotEntity, UUID> {

    List<SlotEntity> findByEventEventIdAndEventTenantId(UUID eventId, String tenantId);

    Optional<SlotEntity> findBySlotIdAndEventEventIdAndEventTenantId(UUID slotId, UUID eventId, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SlotEntity s WHERE s.slotId = :slotId AND s.event.eventId = :eventId AND s.event.tenantId = :tenantId")
    Optional<SlotEntity> findForUpdateBySlotIdAndEventEventIdAndEventTenantId(
            @Param("slotId") UUID slotId,
            @Param("eventId") UUID eventId,
            @Param("tenantId") String tenantId);
}
