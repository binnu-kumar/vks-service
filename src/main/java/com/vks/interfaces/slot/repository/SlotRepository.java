package com.vks.interfaces.slot.repository;

import com.vks.interfaces.slot.entity.SlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<SlotEntity, UUID> {

    List<SlotEntity> findByEventEventIdAndEventTenantId(UUID eventId, String tenantId);

    Optional<SlotEntity> findBySlotIdAndEventEventIdAndEventTenantId(UUID slotId, UUID eventId, String tenantId);
}
