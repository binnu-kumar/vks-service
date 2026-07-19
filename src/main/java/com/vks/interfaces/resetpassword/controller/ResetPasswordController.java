package com.vks.interfaces.resetpassword.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.resetpassword.model.ResetPasswordRequest;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;
import com.vks.interfaces.resetpassword.service.ResetPasswordService;
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
public class ResetPasswordController {

    private final ResetPasswordService resetPasswordService;

    @PostMapping(ApiEndpoints.RESET_PASSWORD)
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = resetPasswordService.resetPassword(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
