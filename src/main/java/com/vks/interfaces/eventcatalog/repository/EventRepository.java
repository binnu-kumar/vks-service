package com.vks.interfaces.eventcatalog.repository;

import com.vks.interfaces.eventcatalog.entity.EventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<EventEntity, UUID>, JpaSpecificationExecutor<EventEntity> {

	List<EventEntity> findAllByTenantIdOrderByStartDateAsc(String tenantId);

	Optional<EventEntity> findByEventIdAndTenantId(UUID eventId, String tenantId);
}
