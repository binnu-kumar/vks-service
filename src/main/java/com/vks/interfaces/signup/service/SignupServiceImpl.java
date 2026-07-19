package com.vks.interfaces.signup.service;

import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.model.SignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;
import com.vks.interfaces.signup.repository.SignupRepository;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignupServiceImpl implements SignupService {

    private final SignupRepository signupRepository;

    private final Argon2 argon2 = Argon2Factory.create();

    @Override
    public SignupResponse signup(SignupRequest request) {
        log.info("Signup attempt for mobileno: {}", request.getMobileno());

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

        signupRepository.save(user);

        log.info("Signup successful for mobileno: {}, id: {}", request.getMobileno(), user.getId());
        return new SignupResponse(true, "User registered successfully");
    }
}
