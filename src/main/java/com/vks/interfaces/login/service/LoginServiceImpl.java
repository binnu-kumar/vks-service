package com.vks.interfaces.login.service;

import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.model.RefreshTokenRequest;
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
            return new LoginResponse(false, "Invalid username or password", null, null);
        }

        SignupEntity user = userOpt.get();

        if (!argon2.verify(user.getPassword(), request.getPassword().toCharArray())) {
            log.warn("Login failed - incorrect password for username: {}", request.getUsername());
            return new LoginResponse(false, "Invalid username or password", null, null);
        }

        String token = jwtUtil.generateToken(request.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(request.getUsername());
        log.info("Login successful for username: {}, id: {}", request.getUsername(), user.getId());

        return new LoginResponse(true, "Login successful", token, refreshToken);
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (!jwtUtil.validateToken(token) || !jwtUtil.isRefreshToken(token)) {
            log.warn("Refresh token failed - invalid or expired refresh token");
            return new LoginResponse(false, "Invalid or expired refresh token", null, null);
        }

        String subject = jwtUtil.extractSubject(token);
        String newAccessToken = jwtUtil.generateToken(subject);
        String newRefreshToken = jwtUtil.generateRefreshToken(subject);
        log.info("Token refreshed for subject: {}", subject);

        return new LoginResponse(true, "Token refreshed", newAccessToken, newRefreshToken);
    }
}
