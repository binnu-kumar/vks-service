package com.vks.interfaces.signup.controller;

import com.vks.common.OtpDeliveryService;
import com.vks.interfaces.forgotpassword.entity.OtpEntity;
import com.vks.interfaces.forgotpassword.repository.OtpRepository;
import com.vks.interfaces.signup.model.SignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;
import com.vks.interfaces.signup.model.AdminSignupRequest;
import com.vks.interfaces.signup.service.SignupService;
import com.vks.common.ApiEndpoints;
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
public class SignupController {

    private final SignupService signupService;
    private final OtpRepository otpRepository;
    private final OtpDeliveryService otpDeliveryService;

    @Transactional
    @PostMapping("/signup-otp")
    public ResponseEntity<Map<String, Object>> signupOtp(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobileno");
        if (mobile == null || !mobile.matches("^[0-9]{10}$")) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Mobile number must be 10 digits"));
        }
        otpRepository.deleteByUsername(mobile);
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        OtpEntity entity = new OtpEntity();
        entity.setUsername(mobile);
        entity.setOtp(otp);
        entity.setExpiry(LocalDateTime.now().plusMinutes(5));
        entity.setUsed(false);
        otpRepository.save(entity);
        otpDeliveryService.deliver(mobile, otp);
        if (otpDeliveryService.sendsSms() && !otpDeliveryService.isTrial()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "OTP sent to your mobile number."));
        }
        if (otpDeliveryService.sendsSms()) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Trial SMS requested. Use this local OTP for verification.", "otp", otp));
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "OTP generated. Check the VKS service console in local mode.", "otp", otp));
    }

    @PostMapping(ApiEndpoints.SIGNUP)
    public ResponseEntity<SignupResponse> signup(@RequestBody SignupRequest request) {
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
