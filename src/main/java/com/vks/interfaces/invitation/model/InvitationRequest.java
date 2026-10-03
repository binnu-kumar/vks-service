package com.vks.interfaces.invitation.model;

import com.vks.security.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InvitationRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String tenantId;

    private UserRole role = UserRole.CUSTOMER;
}
