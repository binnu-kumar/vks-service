package com.vks.interfaces.google.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String redirectUri;

    @NotBlank
    private String role;

    private String invitationToken;
    private String onboardingSecret;
    private String tenantId;
}
