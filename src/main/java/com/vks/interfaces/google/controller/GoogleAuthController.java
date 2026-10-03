package com.vks.interfaces.google.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.google.model.GoogleAuthRequest;
import com.vks.interfaces.google.service.GoogleAuthService;
import com.vks.interfaces.login.model.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiEndpoints.BASE_AUTH)
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/google/callback")
    public ResponseEntity<LoginResponse> callback(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticate(request));
    }
}
