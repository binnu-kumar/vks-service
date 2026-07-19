package com.vks.interfaces.forgotpassword.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Username is required")
    private String username; // mobileno or emailid

    @NotBlank(message = "OTP is required")
    private String otp;
}
