package com.vks.interfaces.forgotpassword.controller;

import com.vks.common.ApiEndpoints;
import com.vks.interfaces.forgotpassword.model.ForgotPasswordRequest;
import com.vks.interfaces.forgotpassword.model.ForgotPasswordResponse;
import com.vks.interfaces.forgotpassword.model.ResetPasswordWithTokenRequest;
import com.vks.interfaces.forgotpassword.model.VerifyOtpRequest;
import com.vks.interfaces.forgotpassword.model.VerifyOtpResponse;
import com.vks.interfaces.forgotpassword.service.ForgotPasswordService;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;
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
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping(ApiEndpoints.FORGOT_PASSWORD)
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ForgotPasswordResponse response = forgotPasswordService.forgotPassword(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(ApiEndpoints.VERIFY_OTP)
    public ResponseEntity<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        VerifyOtpResponse response = forgotPasswordService.verifyOtp(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(ApiEndpoints.RESET_PASSWORD_WITH_TOKEN)
    public ResponseEntity<ResetPasswordResponse> resetPasswordWithToken(@Valid @RequestBody ResetPasswordWithTokenRequest request) {
        ResetPasswordResponse response = forgotPasswordService.resetPasswordWithToken(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
