package com.vks.interfaces.eventcatalog.slot.repository;

import com.vks.interfaces.eventcatalog.slot.entity.SlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<SlotEntity, UUID> {

    List<SlotEntity> findByEventEventId(UUID eventId);
}
