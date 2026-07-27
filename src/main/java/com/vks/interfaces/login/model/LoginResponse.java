package com.vks.interfaces.login.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
}
