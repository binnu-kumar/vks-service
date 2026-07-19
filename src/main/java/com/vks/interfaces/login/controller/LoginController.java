package com.vks.interfaces.login.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.service.LoginService;
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
public class LoginController {

    private final LoginService loginService;

    @PostMapping(ApiEndpoints.LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
