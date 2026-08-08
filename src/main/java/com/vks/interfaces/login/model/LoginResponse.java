package com.vks.interfaces.login.model;

import com.vks.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private String tenantId;
    private UserRole role;
    private List<String> scopes;
}
