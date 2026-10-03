package com.vks.interfaces.invitation.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.invitation.model.InvitationRequest;
import com.vks.interfaces.invitation.model.InvitationResponse;
import com.vks.interfaces.invitation.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.BASE_TENANT_ADMIN + "/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(@Valid @RequestBody InvitationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.createInvitation(request));
    }
}
