package com.vks.interfaces.invitation.service;

import com.vks.common.exception.ConflictException;
import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.invitation.entity.InvitationStatus;
import com.vks.interfaces.invitation.entity.TenantInvitationEntity;
import com.vks.interfaces.invitation.model.InvitationRequest;
import com.vks.interfaces.invitation.model.InvitationResponse;
import com.vks.interfaces.invitation.repository.TenantInvitationRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvitationServiceImpl implements InvitationService {

    private final TenantInvitationRepository invitationRepository;
    private final SecurityContextService securityContextService;

    @Override
    @Transactional
    public InvitationResponse createInvitation(InvitationRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        if (!currentUser.tenantId().equals(request.getTenantId())) {
            throw new ConflictException("Invitation tenant does not match authenticated tenant");
        }
        TenantInvitationEntity invitation = new TenantInvitationEntity();
        invitation.setEmail(request.getEmail().trim().toLowerCase());
        invitation.setTenantId(request.getTenantId());
        invitation.setRole(request.getRole());
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));
        invitation.setInvitedBy(currentUser.userId());
        return toResponse(invitationRepository.save(invitation));
    }

    @Override
    @Transactional
    public TenantInvitationEntity validateAndConsume(String token, String email) {
        TenantInvitationEntity invitation = invitationRepository.findByTokenAndStatus(token, InvitationStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found or already used"));
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new ConflictException("Invitation has expired");
        }
        if (!invitation.getEmail().equalsIgnoreCase(email)) {
            throw new ConflictException("Invitation email does not match Google account");
        }
        invitation.setStatus(InvitationStatus.ACCEPTED);
        return invitationRepository.save(invitation);
    }

    private InvitationResponse toResponse(TenantInvitationEntity invitation) {
        return new InvitationResponse(
                invitation.getId(), invitation.getEmail(), invitation.getTenantId(), invitation.getRole(),
                invitation.getToken(), invitation.getStatus(), invitation.getExpiresAt());
    }
}
