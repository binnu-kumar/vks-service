package com.vks.interfaces.login.service;

import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.repository.LoginRepository;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.security.JwtUtil;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final LoginRepository loginRepository;
    private final JwtUtil jwtUtil;

    private final Argon2 argon2 = Argon2Factory.create();

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());

        Optional<SignupEntity> userOpt = loginRepository.findByMobilenoOrEmailid(request.getUsername());

        if (userOpt.isEmpty()) {
            log.warn("Login failed - user not found for username: {}", request.getUsername());
            return new LoginResponse(false, "Invalid username or password", null);
        }

        SignupEntity user = userOpt.get();

        if (!argon2.verify(user.getPassword(), request.getPassword().toCharArray())) {
            log.warn("Login failed - incorrect password for username: {}", request.getUsername());
            return new LoginResponse(false, "Invalid username or password", null);
        }

        String token = jwtUtil.generateToken(String.valueOf(user.getId()));
        log.info("Login successful for username: {}, id: {}", request.getUsername(), user.getId());

        return new LoginResponse(true, "Login successful", token);
    }
}
