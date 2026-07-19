package com.vks.interfaces.forgotpassword.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyOtpResponse {

    private boolean success;
    private String message;
    private String resetToken; // use this token to call reset-password
}
