package com.vks.interfaces.login.service;

import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.model.RefreshTokenRequest;
import com.vks.interfaces.login.repository.LoginRepository;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.security.AuthenticatedUser;
import com.vks.security.JwtUtil;
import com.vks.security.UserRole;
import de.mkammerer.argon2.Argon2Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private LoginServiceImpl loginService;

    @Test
    void loginReturnsFailureWhenUserMissing() {
        LoginRequest request = loginRequest();
        when(loginRepository.findByMobilenoOrEmailid(request.getUsername())).thenReturn(Optional.empty());

        LoginResponse response = loginService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid username or password", response.getMessage());
    }

    @Test
    void loginReturnsFailureWhenPasswordInvalid() {
        LoginRequest request = loginRequest();
        SignupEntity user = userWithPassword("Other@1234");
        when(loginRepository.findByMobilenoOrEmailid(request.getUsername())).thenReturn(Optional.of(user));

        LoginResponse response = loginService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid username or password", response.getMessage());
    }

    @Test
    void loginReturnsTokensWhenCredentialsValid() {
        LoginRequest request = loginRequest();
        SignupEntity user = userWithPassword(request.getPassword());
        when(loginRepository.findByMobilenoOrEmailid(request.getUsername())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(any(AuthenticatedUser.class))).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken(any(AuthenticatedUser.class))).thenReturn("refresh-token");

        LoginResponse response = loginService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("Login successful", response.getMessage());
        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals("tenant-123", response.getTenantId());
        assertEquals(UserRole.CUSTOMER, response.getRole());
    }

    @Test
    void refreshReturnsFailureWhenTokenInvalid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("bad-token");
        when(jwtUtil.validateToken(request.getRefreshToken())).thenReturn(false);

        LoginResponse response = loginService.refresh(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid or expired refresh token", response.getMessage());
    }

    @Test
    void refreshReturnsNewTokensWhenRefreshTokenValid() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
            "1", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes());
        when(jwtUtil.validateToken(request.getRefreshToken())).thenReturn(true);
        when(jwtUtil.isRefreshToken(request.getRefreshToken())).thenReturn(true);
        when(jwtUtil.extractAuthenticatedUser(request.getRefreshToken())).thenReturn(authenticatedUser);
        when(jwtUtil.generateToken(authenticatedUser)).thenReturn("new-access");
        when(jwtUtil.generateRefreshToken(authenticatedUser)).thenReturn("new-refresh");

        LoginResponse response = loginService.refresh(request);

        assertTrue(response.isSuccess());
        assertEquals("Token refreshed", response.getMessage());
        assertEquals("new-access", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals("tenant-123", response.getTenantId());
        assertEquals(UserRole.CUSTOMER, response.getRole());
        verify(jwtUtil).extractAuthenticatedUser(request.getRefreshToken());
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setUsername("9876543210");
        request.setPassword("Pass@1234");
        return request;
    }

    private SignupEntity userWithPassword(String rawPassword) {
        SignupEntity user = new SignupEntity();
        user.setId(1L);
        user.setMobileno("9876543210");
        user.setTenantId("tenant-123");
        user.setRole(UserRole.CUSTOMER);
        user.setPassword(Argon2Factory.create().hash(2, 65536, 1, rawPassword.toCharArray()));
        return user;
    }
}