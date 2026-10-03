package com.vks.interfaces.login.controller;

import com.vks.common.ApiEndpoints;
import com.vks.common.OtpDeliveryService;
import com.vks.interfaces.forgotpassword.entity.OtpEntity;
import com.vks.interfaces.forgotpassword.repository.OtpRepository;
import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.model.RefreshTokenRequest;
import com.vks.interfaces.login.service.LoginService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping(ApiEndpoints.BASE_AUTH)
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final OtpRepository otpRepository;
    private final OtpDeliveryService otpDeliveryService;

    @Transactional
    @PostMapping("/login-otp/request")
    public ResponseEntity<Map<String, Object>> requestOtp(@RequestBody Map<String, String> body) {
        String mobile = body.get("mobile");
        otpRepository.deleteByUsername(mobile);
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        OtpEntity entity = new OtpEntity();
        entity.setUsername(mobile); entity.setOtp(otp); entity.setExpiry(LocalDateTime.now().plusMinutes(5)); entity.setUsed(false);
        otpRepository.save(entity); otpDeliveryService.deliver(mobile, otp);
        return ResponseEntity.ok(otpDeliveryService.sendsSms() && !otpDeliveryService.isTrial()
                ? Map.of("success", true, "message", "OTP sent to your mobile number.")
                : Map.of("success", true, "message", "Use the local OTP for testing.", "otp", otp));
    }

    @PostMapping("/login-otp/verify")
    public ResponseEntity<LoginResponse> verifyOtp(@RequestBody Map<String, String> body) {
        LoginResponse response = loginService.loginWithOtp(body.get("mobile"), body.get("otp"));
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    @PostMapping(ApiEndpoints.LOGIN)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.login(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping(ApiEndpoints.REFRESH)
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = loginService.refresh(request);
        if (!response.isSuccess()) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }
}
