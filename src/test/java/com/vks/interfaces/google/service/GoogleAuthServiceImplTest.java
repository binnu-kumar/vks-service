package com.vks.interfaces.google.service;

import com.vks.common.exception.ConflictException;
import com.vks.interfaces.google.entity.ExternalIdentityEntity;
import com.vks.interfaces.google.model.GoogleAuthRequest;
import com.vks.interfaces.google.repository.ExternalIdentityRepository;
import com.vks.interfaces.invitation.entity.InvitationStatus;
import com.vks.interfaces.invitation.entity.TenantInvitationEntity;
import com.vks.interfaces.invitation.service.InvitationService;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.repository.SignupRepository;
import com.vks.security.JwtUtil;
import com.vks.security.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceImplTest {

    @Mock private GoogleIdentityVerifier identityVerifier;
    @Mock private ExternalIdentityRepository externalIdentityRepository;
    @Mock private SignupRepository signupRepository;
    @Mock private InvitationService invitationService;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private GoogleAuthServiceImpl googleAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleAuthService, "adminOnboardingSecret", "admin-secret");
        lenient().when(jwtUtil.generateToken(any())).thenReturn("access-token");
        lenient().when(jwtUtil.generateRefreshToken(any())).thenReturn("refresh-token");
    }

    @Test
    void customerWithInvitationIsLinkedAndIssuedTokens() {
        GoogleAuthRequest request = request("CUSTOMER");
        request.setInvitationToken("invite-token");
        GoogleIdentity identity = new GoogleIdentity("google-sub", "customer@example.com", "Google", "Customer");
        TenantInvitationEntity invitation = invitation(UserRole.CUSTOMER);
        SignupEntity user = newUser(42L, "tenant-1", UserRole.CUSTOMER, identity.email());

        when(identityVerifier.verifyAuthorizationCode(request.getCode(), request.getRedirectUri())).thenReturn(identity);
        when(externalIdentityRepository.findByProviderAndSubject("google", identity.subject())).thenReturn(Optional.empty());
        when(invitationService.validateAndConsume("invite-token", identity.email())).thenReturn(invitation);
        when(signupRepository.findByEmailidIgnoreCase(identity.email())).thenReturn(Optional.empty());
        when(signupRepository.save(any(SignupEntity.class))).thenReturn(user);
        when(externalIdentityRepository.save(any(ExternalIdentityEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = googleAuthService.authenticate(request);

        assertEquals(true, response.isSuccess());
        assertEquals(UserRole.CUSTOMER, response.getRole());
        assertEquals("tenant-1", response.getTenantId());
    }

    @Test
    void customerWithoutInvitationIsRejected() {
        GoogleAuthRequest request = request("CUSTOMER");
        GoogleIdentity identity = new GoogleIdentity("google-sub", "customer@example.com", "Google", "Customer");
        when(identityVerifier.verifyAuthorizationCode(request.getCode(), request.getRedirectUri())).thenReturn(identity);
        when(externalIdentityRepository.findByProviderAndSubject("google", identity.subject())).thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> googleAuthService.authenticate(request));
    }

    @Test
    void firstAdminCanBootstrapWithOnboardingSecret() {
        GoogleAuthRequest request = request("TENANT_ADMIN");
        request.setTenantId("tenant-1");
        request.setOnboardingSecret("admin-secret");
        GoogleIdentity identity = new GoogleIdentity("google-admin", "admin@example.com", "Google", "Admin");
        SignupEntity user = newUser(43L, "tenant-1", UserRole.TENANT_ADMIN, identity.email());

        when(identityVerifier.verifyAuthorizationCode(request.getCode(), request.getRedirectUri())).thenReturn(identity);
        when(externalIdentityRepository.findByProviderAndSubject("google", identity.subject())).thenReturn(Optional.empty());
        when(signupRepository.existsByTenantIdAndRole("tenant-1", UserRole.TENANT_ADMIN)).thenReturn(false);
        when(signupRepository.findByEmailidIgnoreCase(identity.email())).thenReturn(Optional.empty());
        when(signupRepository.save(any(SignupEntity.class))).thenReturn(user);
        when(externalIdentityRepository.save(any(ExternalIdentityEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LoginResponse response = googleAuthService.authenticate(request);

        assertEquals(UserRole.TENANT_ADMIN, response.getRole());
        assertEquals("tenant-1", response.getTenantId());
    }

    @Test
    void invalidOrExpiredGoogleIdentityIsRejected() {
        GoogleAuthRequest request = request("CUSTOMER");
        when(identityVerifier.verifyAuthorizationCode(request.getCode(), request.getRedirectUri()))
                .thenThrow(new ConflictException("Google identity claims are invalid or expired"));

        assertThrows(ConflictException.class, () -> googleAuthService.authenticate(request));
    }

    @Test
    void linkedIdentityLogsInWithoutInvitation() {
        GoogleAuthRequest request = request("CUSTOMER");
        GoogleIdentity identity = new GoogleIdentity("linked-sub", "customer@example.com", "Google", "Customer");
        SignupEntity user = newUser(44L, "tenant-1", UserRole.CUSTOMER, identity.email());
        ExternalIdentityEntity linked = new ExternalIdentityEntity();
        linked.setUserId(44L);

        when(identityVerifier.verifyAuthorizationCode(request.getCode(), request.getRedirectUri())).thenReturn(identity);
        when(externalIdentityRepository.findByProviderAndSubject("google", identity.subject())).thenReturn(Optional.of(linked));
        when(signupRepository.findById(44L)).thenReturn(Optional.of(user));

        LoginResponse response = googleAuthService.authenticate(request);

        assertEquals(true, response.isSuccess());
        assertEquals("tenant-1", response.getTenantId());
    }

    private GoogleAuthRequest request(String role) {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setCode("authorization-code");
        request.setRedirectUri("http://localhost/callback");
        request.setRole(role);
        return request;
    }

    private TenantInvitationEntity invitation(UserRole role) {
        TenantInvitationEntity invitation = new TenantInvitationEntity();
        invitation.setEmail("customer@example.com");
        invitation.setTenantId("tenant-1");
        invitation.setRole(role);
        invitation.setToken("invite-token");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));
        return invitation;
    }

    private SignupEntity newUser(Long id, String tenantId, UserRole role, String email) {
        SignupEntity user = new SignupEntity();
        user.setId(id);
        user.setFirstname("Google");
        user.setLastname("User");
        user.setEmailid(email);
        user.setMobileno("google-" + UUID.randomUUID());
        user.setPassword("password");
        user.setTenantId(tenantId);
        user.setRole(role);
        return user;
    }
}
