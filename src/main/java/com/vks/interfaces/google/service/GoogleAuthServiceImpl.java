package com.vks.interfaces.google.service;

import com.vks.common.exception.ConflictException;
import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.google.entity.ExternalIdentityEntity;
import com.vks.interfaces.google.model.GoogleAuthRequest;
import com.vks.interfaces.google.repository.ExternalIdentityRepository;
import com.vks.interfaces.invitation.entity.TenantInvitationEntity;
import com.vks.interfaces.invitation.service.InvitationService;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.repository.SignupRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.JwtUtil;
import com.vks.security.UserRole;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private static final String GOOGLE_PROVIDER = "google";

    private final GoogleIdentityVerifier identityVerifier;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final SignupRepository signupRepository;
    private final InvitationService invitationService;
    private final JwtUtil jwtUtil;

    @Value("${security.admin-onboarding.secret}")
    private String adminOnboardingSecret;

    private final Argon2 argon2 = Argon2Factory.create();

    @Override
    @Transactional
    public LoginResponse authenticate(GoogleAuthRequest request) {
        UserRole requestedRole = parseRole(request.getRole());
        GoogleIdentity identity = identityVerifier.verifyAuthorizationCode(request.getCode(), request.getRedirectUri());

        ExternalIdentityEntity linkedIdentity = externalIdentityRepository
                .findByProviderAndSubject(GOOGLE_PROVIDER, identity.subject())
                .orElse(null);
        SignupEntity user = linkedIdentity == null
                ? resolveOrCreateUser(identity, requestedRole, request)
                : signupRepository.findById(linkedIdentity.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Linked user account not found"));

        if (linkedIdentity == null) {
            linkIdentity(identity, user);
        }
        return issueTokens(user);
    }

    private SignupEntity resolveOrCreateUser(GoogleIdentity identity, UserRole requestedRole, GoogleAuthRequest request) {
        TenantInvitationEntity invitation = null;
        if (request.getInvitationToken() != null && !request.getInvitationToken().isBlank()) {
            invitation = invitationService.validateAndConsume(request.getInvitationToken(), identity.email());
            if (invitation.getRole() != requestedRole) {
                throw new ConflictException("Invitation role does not match requested role");
            }
        } else if (requestedRole == UserRole.CUSTOMER) {
            throw new ConflictException("A valid tenant invitation is required");
        }

        String tenantId = invitation == null ? request.getTenantId() : invitation.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ConflictException("Tenant id is required for Google authentication");
        }

        if (requestedRole == UserRole.TENANT_ADMIN
                && invitation == null
                && (request.getOnboardingSecret() == null
                || !adminOnboardingSecret.equals(request.getOnboardingSecret())
                || signupRepository.existsByTenantIdAndRole(tenantId, UserRole.TENANT_ADMIN))) {
            throw new ConflictException("Admin invitation or valid first-admin onboarding secret is required");
        }

        SignupEntity existing = signupRepository.findByEmailidIgnoreCase(identity.email()).orElse(null);
        if (existing != null) {
            if (!tenantId.equals(existing.getTenantId()) || existing.getRole() != requestedRole) {
                throw new ConflictException("Google account is already linked to another tenant or role");
            }
            return existing;
        }

        SignupEntity user = new SignupEntity();
        user.setFirstname(defaultName(identity.firstName(), identity.email()));
        user.setLastname(defaultName(identity.lastName(), "User"));
        user.setEmailid(identity.email());
        user.setMobileno("google-" + UUID.randomUUID());
        user.setPassword(argon2.hash(2, 65536, 1, UUID.randomUUID().toString().toCharArray()));
        user.setTenantId(tenantId);
        user.setRole(requestedRole);
        return signupRepository.save(user);
    }

    private void linkIdentity(GoogleIdentity identity, SignupEntity user) {
        ExternalIdentityEntity externalIdentity = new ExternalIdentityEntity();
        externalIdentity.setProvider(GOOGLE_PROVIDER);
        externalIdentity.setSubject(identity.subject());
        externalIdentity.setUserId(user.getId());
        externalIdentity.setEmail(identity.email());
        externalIdentityRepository.save(externalIdentity);
    }

    private LoginResponse issueTokens(SignupEntity user) {
        UserRole role = user.getRole() == null ? UserRole.CUSTOMER : user.getRole();
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                String.valueOf(user.getId()), user.getEmailid(), user.getTenantId(), role, role.defaultScopes());
        return new LoginResponse(true, "Google login successful", jwtUtil.generateToken(authenticatedUser),
                jwtUtil.generateRefreshToken(authenticatedUser), user.getTenantId(), role, role.defaultScopes());
    }

    private UserRole parseRole(String role) {
        try {
            return UserRole.valueOf(role);
        } catch (RuntimeException exception) {
            throw new ConflictException("Unsupported Google authentication role");
        }
    }

    private String defaultName(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
