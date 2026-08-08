package com.vks.interfaces.signup.service;

import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.model.AdminSignupRequest;
import com.vks.interfaces.signup.model.SignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;
import com.vks.interfaces.signup.repository.SignupRepository;
import com.vks.security.UserRole;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final SignupRepository signupRepository;

    @Value("${security.admin-onboarding.secret:admin-bootstrap-secret}")
    private String adminOnboardingSecret;

    private final Argon2 argon2 = Argon2Factory.create();

    @Override
    public SignupResponse signup(SignupRequest request) {
        log.info("Signup attempt for mobileno: {}", request.getMobileno());

        return registerUser(request, UserRole.CUSTOMER);
    }

    @Override
    public SignupResponse adminSignup(AdminSignupRequest request) {
        log.info("Admin signup attempt for mobileno: {} and tenant: {}", request.getMobileno(), request.getTenantId());

        if (!adminOnboardingSecret.equals(request.getOnboardingSecret())) {
            log.warn("Admin signup failed - invalid onboarding secret for mobileno: {}", request.getMobileno());
            return new SignupResponse(false, "Invalid admin onboarding secret");
        }

        return registerUser(request, UserRole.TENANT_ADMIN);
    }

    private SignupResponse registerUser(SignupRequest request, UserRole role) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            log.warn("Signup failed - password mismatch for mobileno: {}", request.getMobileno());
            return new SignupResponse(false, "Password and confirm password do not match");
        }

        if (signupRepository.existsByMobileno(request.getMobileno())) {
            log.warn("Signup failed - mobile number already registered: {}", request.getMobileno());
            return new SignupResponse(false, "Mobile number already registered");
        }

        String hashedPassword = argon2.hash(2, 65536, 1, request.getPassword().toCharArray());

        SignupEntity user = new SignupEntity();
        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setMobileno(request.getMobileno());
        user.setEmailid(request.getEmailid());
        user.setPassword(hashedPassword);
        user.setTenantId(request.getTenantId());
        user.setRole(role);

        signupRepository.save(user);

        log.info("{} signup successful for mobileno: {}, id: {}", role, request.getMobileno(), user.getId());
        return new SignupResponse(true, role == UserRole.TENANT_ADMIN ? "Tenant admin registered successfully" : "User registered successfully");
    }
}
