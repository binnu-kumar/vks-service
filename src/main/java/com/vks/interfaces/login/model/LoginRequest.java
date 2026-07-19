package com.vks.interfaces.login.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username; // mobileno or emailid

    @NotBlank(message = "Password is required")
    private String password;
}
