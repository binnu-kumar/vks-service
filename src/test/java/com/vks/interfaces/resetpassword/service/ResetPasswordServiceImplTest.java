package com.vks.interfaces.resetpassword.service;

import com.vks.interfaces.resetpassword.model.ResetPasswordRequest;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;
import com.vks.interfaces.resetpassword.repository.ResetPasswordRepository;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResetPasswordServiceImplTest {

    @Mock
    private ResetPasswordRepository resetPasswordRepository;

    @Mock
    private SecurityContextService securityContextService;

    @InjectMocks
    private ResetPasswordServiceImpl resetPasswordService;

    private static final AuthenticatedUser CURRENT_USER = new AuthenticatedUser(
            "42", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes());

    @Test
    void resetPasswordReturnsFailureWhenPasswordsDoNotMatch() {
        ResetPasswordRequest request = request();
        request.setConfirmPassword("Mismatch@123");
        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);

        ResetPasswordResponse response = resetPasswordService.resetPassword(request);

        assertFalse(response.isSuccess());
        assertEquals("New password and confirm password do not match", response.getMessage());
        verify(resetPasswordRepository, never()).save(org.mockito.ArgumentMatchers.any(SignupEntity.class));
    }

    @Test
    void resetPasswordReturnsFailureWhenUserMissing() {
        ResetPasswordRequest request = request();
        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(resetPasswordRepository.findById(42L)).thenReturn(Optional.empty());

        ResetPasswordResponse response = resetPasswordService.resetPassword(request);

        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
    }

    @Test
    void resetPasswordHashesAndSavesPasswordForAuthenticatedUser() {
        ResetPasswordRequest request = request();
        SignupEntity user = new SignupEntity();
        user.setPassword(Argon2Factory.create().hash(2, 65536, 1, "OldPass@123".toCharArray()));
        when(securityContextService.currentUser()).thenReturn(CURRENT_USER);
        when(resetPasswordRepository.findById(42L)).thenReturn(Optional.of(user));

        ResetPasswordResponse response = resetPasswordService.resetPassword(request);

        assertTrue(response.isSuccess());
        assertEquals("Password reset successful", response.getMessage());
        assertTrue(Argon2Factory.create().verify(user.getPassword(), request.getNewPassword().toCharArray()));
        verify(resetPasswordRepository).save(user);
    }

    private ResetPasswordRequest request() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setNewPassword("NewPass@123");
        request.setConfirmPassword("NewPass@123");
        return request;
    }
}