package com.vks.interfaces.signup.controller;

import com.vks.interfaces.signup.model.SignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;
import com.vks.interfaces.signup.model.AdminSignupRequest;
import com.vks.interfaces.signup.service.SignupService;
import com.vks.common.ApiEndpoints;
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
public class SignupController {

    private final SignupService signupService;

    @PostMapping(ApiEndpoints.SIGNUP)
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = signupService.signup(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(ApiEndpoints.ADMIN_SIGNUP)
    public ResponseEntity<SignupResponse> adminSignup(@Valid @RequestBody AdminSignupRequest request) {
        SignupResponse response = signupService.adminSignup(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
