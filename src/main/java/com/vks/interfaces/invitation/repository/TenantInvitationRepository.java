package com.vks.interfaces.invitation.repository;

import com.vks.interfaces.invitation.entity.InvitationStatus;
import com.vks.interfaces.invitation.entity.TenantInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantInvitationRepository extends JpaRepository<TenantInvitationEntity, UUID> {

    Optional<TenantInvitationEntity> findByTokenAndStatus(String token, InvitationStatus status);
}
