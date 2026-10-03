package com.vks.interfaces.invitation.model;

import com.vks.interfaces.invitation.entity.InvitationStatus;
import com.vks.security.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record InvitationResponse(
        UUID id,
        String email,
        String tenantId,
        UserRole role,
        String token,
        InvitationStatus status,
        LocalDateTime expiresAt
) {}
