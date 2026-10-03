package com.vks.interfaces.invitation.service;

import com.vks.interfaces.invitation.entity.TenantInvitationEntity;
import com.vks.interfaces.invitation.model.InvitationRequest;
import com.vks.interfaces.invitation.model.InvitationResponse;

public interface InvitationService {

    InvitationResponse createInvitation(InvitationRequest request);

    TenantInvitationEntity validateAndConsume(String token, String email);
}
