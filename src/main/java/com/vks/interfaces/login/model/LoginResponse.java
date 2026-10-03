package com.vks.interfaces.login.model;

import com.vks.security.UserRole;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class LoginResponse {

    private boolean success;
    private String message;
    private String token;
    private String refreshToken;
    private String tenantId;
    private UserRole role;
    private List<String> scopes;
    private String registrationToken;

    public LoginResponse(boolean success, String message, String token, String refreshToken,
                         String tenantId, UserRole role, List<String> scopes) {
        this(success, message, token, refreshToken, tenantId, role, scopes, null);
    }

    public LoginResponse(boolean success, String message, String token, String refreshToken,
                         String tenantId, UserRole role, List<String> scopes, String registrationToken) {
        this.success = success;
        this.message = message;
        this.token = token;
        this.refreshToken = refreshToken;
        this.tenantId = tenantId;
        this.role = role;
        this.scopes = scopes;
        this.registrationToken = registrationToken;
    }
}
