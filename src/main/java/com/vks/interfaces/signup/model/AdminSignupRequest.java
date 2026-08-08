package com.vks.interfaces.signup.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminSignupRequest extends SignupRequest {

    @NotBlank(message = "Admin onboarding secret is required")
    private String onboardingSecret;
}