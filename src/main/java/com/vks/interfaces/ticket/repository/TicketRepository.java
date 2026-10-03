package com.vks.interfaces.ticket.repository;

import com.vks.interfaces.ticket.entity.TicketEntity;
import com.vks.interfaces.ticket.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    List<TicketEntity> findByBookingIdAndTenantIdOrderByCreatedAtAsc(UUID bookingId, String tenantId);

    List<TicketEntity> findByCustomerIdAndTenantIdOrderByCreatedAtDesc(String customerId, String tenantId);

    Optional<TicketEntity> findByTicketIdAndCustomerIdAndTenantId(UUID ticketId, String customerId, String tenantId);

    Optional<TicketEntity> findByTicketNumberAndTenantId(String ticketNumber, String tenantId);
}
