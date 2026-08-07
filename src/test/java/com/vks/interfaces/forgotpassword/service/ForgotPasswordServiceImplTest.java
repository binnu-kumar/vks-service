package com.vks.interfaces.forgotpassword.service;

import com.vks.common.EmailService;
import com.vks.interfaces.forgotpassword.entity.OtpEntity;
import com.vks.interfaces.forgotpassword.model.ForgotPasswordRequest;
import com.vks.interfaces.forgotpassword.model.ForgotPasswordResponse;
import com.vks.interfaces.forgotpassword.model.ResetPasswordWithTokenRequest;
import com.vks.interfaces.forgotpassword.model.VerifyOtpRequest;
import com.vks.interfaces.forgotpassword.model.VerifyOtpResponse;
import com.vks.interfaces.forgotpassword.repository.ForgotPasswordRepository;
import com.vks.interfaces.forgotpassword.repository.OtpRepository;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.security.JwtUtil;
import de.mkammerer.argon2.Argon2Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForgotPasswordServiceImplTest {

    @Mock
    private ForgotPasswordRepository forgotPasswordRepository;

    @Mock
    private OtpRepository otpRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private ForgotPasswordServiceImpl forgotPasswordService;

    @Test
    void forgotPasswordReturnsFailureWhenUserMissing() {
        ForgotPasswordRequest request = forgotPasswordRequest();
        when(forgotPasswordRepository.findByMobilenoOrEmailid(request.getUsername())).thenReturn(Optional.empty());

        ForgotPasswordResponse response = forgotPasswordService.forgotPassword(request);

        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
    }

    @Test
    void forgotPasswordReturnsFailureWhenEmailMissing() {
        ForgotPasswordRequest request = forgotPasswordRequest();
        SignupEntity user = new SignupEntity();
        user.setMobileno(request.getUsername());
        when(forgotPasswordRepository.findByMobilenoOrEmailid(request.getUsername())).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = forgotPasswordService.forgotPassword(request);

        assertFalse(response.isSuccess());
        assertEquals("No email address registered for this account", response.getMessage());
    }

    @Test
    void forgotPasswordGeneratesOtpAndSendsEmail() {
        ForgotPasswordRequest request = forgotPasswordRequest();
        SignupEntity user = new SignupEntity();
        user.setMobileno(request.getUsername());
        user.setEmailid("john@example.com");
        when(forgotPasswordRepository.findByMobilenoOrEmailid(request.getUsername())).thenReturn(Optional.of(user));

        ForgotPasswordResponse response = forgotPasswordService.forgotPassword(request);

        ArgumentCaptor<OtpEntity> captor = ArgumentCaptor.forClass(OtpEntity.class);
        verify(otpRepository).save(captor.capture());
        OtpEntity otpEntity = captor.getValue();

        assertTrue(response.isSuccess());
        assertEquals(request.getUsername(), otpEntity.getUsername());
        assertEquals(6, otpEntity.getOtp().length());
        assertFalse(otpEntity.isUsed());
        assertNotNull(otpEntity.getExpiry());
        verify(otpRepository).deleteByUsername(request.getUsername());
        verify(emailService).sendOtp(user.getEmailid(), otpEntity.getOtp());
    }

    @Test
    void verifyOtpReturnsFailureWhenOtpMissing() {
        VerifyOtpRequest request = verifyOtpRequest();
        when(otpRepository.findTopByUsernameAndUsedFalseOrderByExpiryDesc(request.getUsername())).thenReturn(Optional.empty());

        VerifyOtpResponse response = forgotPasswordService.verifyOtp(request);

        assertFalse(response.isSuccess());
        assertEquals("OTP not generated or already used", response.getMessage());
        assertNull(response.getResetToken());
    }

    @Test
    void verifyOtpDeletesExpiredOtp() {
        VerifyOtpRequest request = verifyOtpRequest();
        OtpEntity otpEntity = otpEntity(request.getUsername(), request.getOtp(), LocalDateTime.now().minusMinutes(1));
        when(otpRepository.findTopByUsernameAndUsedFalseOrderByExpiryDesc(request.getUsername())).thenReturn(Optional.of(otpEntity));

        VerifyOtpResponse response = forgotPasswordService.verifyOtp(request);

        assertFalse(response.isSuccess());
        assertEquals("OTP has expired", response.getMessage());
        verify(otpRepository).delete(otpEntity);
    }

    @Test
    void verifyOtpReturnsFailureWhenOtpInvalid() {
        VerifyOtpRequest request = verifyOtpRequest();
        OtpEntity otpEntity = otpEntity(request.getUsername(), "999999", LocalDateTime.now().plusMinutes(5));
        when(otpRepository.findTopByUsernameAndUsedFalseOrderByExpiryDesc(request.getUsername())).thenReturn(Optional.of(otpEntity));

        VerifyOtpResponse response = forgotPasswordService.verifyOtp(request);

        assertFalse(response.isSuccess());
        assertEquals("Invalid OTP", response.getMessage());
    }

    @Test
    void verifyOtpMarksOtpUsedAndReturnsResetToken() {
        VerifyOtpRequest request = verifyOtpRequest();
        OtpEntity otpEntity = otpEntity(request.getUsername(), request.getOtp(), LocalDateTime.now().plusMinutes(5));
        when(otpRepository.findTopByUsernameAndUsedFalseOrderByExpiryDesc(request.getUsername())).thenReturn(Optional.of(otpEntity));
        when(jwtUtil.generatePasswordResetToken(request.getUsername())).thenReturn("reset-token");

        VerifyOtpResponse response = forgotPasswordService.verifyOtp(request);

        assertTrue(response.isSuccess());
        assertEquals("reset-token", response.getResetToken());
        assertTrue(otpEntity.isUsed());
        verify(otpRepository).save(otpEntity);
    }

    @Test
    void resetPasswordWithTokenReturnsFailureWhenPasswordsMismatch() {
        ResetPasswordWithTokenRequest request = resetWithTokenRequest();
        request.setConfirmPassword("Mismatch@123");

        ResetPasswordResponse response = forgotPasswordService.resetPasswordWithToken(request);

        assertFalse(response.isSuccess());
        assertEquals("Passwords do not match", response.getMessage());
    }

    @Test
    void resetPasswordWithTokenReturnsFailureWhenTokenInvalid() {
        ResetPasswordWithTokenRequest request = resetWithTokenRequest();
        when(jwtUtil.validateToken(request.getResetToken())).thenReturn(false);

        ResetPasswordResponse response = forgotPasswordService.resetPasswordWithToken(request);

        assertFalse(response.isSuccess());
        assertEquals("Reset token is invalid or expired", response.getMessage());
    }

    @Test
    void resetPasswordWithTokenReturnsFailureWhenUserMissing() {
        ResetPasswordWithTokenRequest request = resetWithTokenRequest();
        when(jwtUtil.validateToken(request.getResetToken())).thenReturn(true);
        when(jwtUtil.isPasswordResetToken(request.getResetToken())).thenReturn(true);
        when(jwtUtil.extractSubject(request.getResetToken())).thenReturn("9876543210");
        when(forgotPasswordRepository.findByMobilenoOrEmailid("9876543210")).thenReturn(Optional.empty());

        ResetPasswordResponse response = forgotPasswordService.resetPasswordWithToken(request);

        assertFalse(response.isSuccess());
        assertEquals("User not found", response.getMessage());
    }

    @Test
    void resetPasswordWithTokenHashesAndSavesPassword() {
        ResetPasswordWithTokenRequest request = resetWithTokenRequest();
        SignupEntity user = new SignupEntity();
        user.setPassword(Argon2Factory.create().hash(2, 65536, 1, "OldPass@123".toCharArray()));
        when(jwtUtil.validateToken(request.getResetToken())).thenReturn(true);
        when(jwtUtil.isPasswordResetToken(request.getResetToken())).thenReturn(true);
        when(jwtUtil.extractSubject(request.getResetToken())).thenReturn("9876543210");
        when(forgotPasswordRepository.findByMobilenoOrEmailid("9876543210")).thenReturn(Optional.of(user));

        ResetPasswordResponse response = forgotPasswordService.resetPasswordWithToken(request);

        assertTrue(response.isSuccess());
        assertEquals("Password reset successful", response.getMessage());
        assertTrue(Argon2Factory.create().verify(user.getPassword(), request.getNewPassword().toCharArray()));
        verify(forgotPasswordRepository).save(user);
    }

    private ForgotPasswordRequest forgotPasswordRequest() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("9876543210");
        return request;
    }

    private VerifyOtpRequest verifyOtpRequest() {
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setUsername("9876543210");
        request.setOtp("123456");
        return request;
    }

    private ResetPasswordWithTokenRequest resetWithTokenRequest() {
        ResetPasswordWithTokenRequest request = new ResetPasswordWithTokenRequest();
        request.setResetToken("reset-token");
        request.setNewPassword("NewPass@123");
        request.setConfirmPassword("NewPass@123");
        return request;
    }

    private OtpEntity otpEntity(String username, String otp, LocalDateTime expiry) {
        OtpEntity entity = new OtpEntity();
        entity.setUsername(username);
        entity.setOtp(otp);
        entity.setExpiry(expiry);
        entity.setUsed(false);
        return entity;
    }
}