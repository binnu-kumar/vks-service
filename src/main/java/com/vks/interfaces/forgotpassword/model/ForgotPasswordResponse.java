package com.vks.interfaces.forgotpassword.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ForgotPasswordResponse {

    private boolean success;
    private String message;
}
