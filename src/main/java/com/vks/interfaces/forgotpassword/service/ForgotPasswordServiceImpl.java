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
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordServiceImpl implements ForgotPasswordService {

    private final ForgotPasswordRepository forgotPasswordRepository;
    private final OtpRepository otpRepository;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    private final Argon2 argon2 = Argon2Factory.create();

    private static final int OTP_EXPIRY_MINUTES = 5;

    @Override
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password request for username: {}", request.getUsername());

        Optional<SignupEntity> userOpt = forgotPasswordRepository.findByMobilenoOrEmailid(request.getUsername());

        if (userOpt.isEmpty()) {
            log.warn("Forgot password - user not found for username: {}", request.getUsername());
            return new ForgotPasswordResponse(false, "User not found");
        }

        SignupEntity user = userOpt.get();

        if (user.getEmailid() == null) {
            log.warn("Forgot password - no email registered for username: {}", request.getUsername());
            return new ForgotPasswordResponse(false, "No email address registered for this account");
        }

        otpRepository.deleteByUsername(request.getUsername());

        String otp = generateOtp();

        OtpEntity otpEntity = new OtpEntity();
        otpEntity.setUsername(request.getUsername());
        otpEntity.setOtp(otp);
        otpEntity.setExpiry(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
        otpEntity.setUsed(false);
        otpRepository.save(otpEntity);

        emailService.sendOtp(user.getEmailid(), otp);

        log.info("OTP sent to email for username: {}", request.getUsername());
        return new ForgotPasswordResponse(true, "OTP sent to registered email address. Valid for 5 minutes.");
    }

    @Override
    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        log.info("OTP verification attempt for username: {}", request.getUsername());

        OtpEntity otpEntity = otpRepository
                .findTopByUsernameAndUsedFalseOrderByExpiryDesc(request.getUsername())
                .orElse(null);

        if (otpEntity == null) {
            log.warn("OTP verification failed - no OTP found for username: {}", request.getUsername());
            return new VerifyOtpResponse(false, "OTP not generated or already used", null);
        }

        if (LocalDateTime.now().isAfter(otpEntity.getExpiry())) {
            otpRepository.delete(otpEntity);
            log.warn("OTP verification failed - OTP expired for username: {}", request.getUsername());
            return new VerifyOtpResponse(false, "OTP has expired", null);
        }

        if (!otpEntity.getOtp().equals(request.getOtp())) {
            log.warn("OTP verification failed - invalid OTP for username: {}", request.getUsername());
            return new VerifyOtpResponse(false, "Invalid OTP", null);
        }

        otpEntity.setUsed(true);
        otpRepository.save(otpEntity);

        String resetToken = jwtUtil.generatePasswordResetToken(request.getUsername());
        log.info("OTP verified successfully for username: {}", request.getUsername());

        return new VerifyOtpResponse(true, "OTP verified successfully", resetToken);
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPasswordWithToken(ResetPasswordWithTokenRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return new ResetPasswordResponse(false, "Passwords do not match");
        }

        if (!jwtUtil.validateToken(request.getResetToken()) || !jwtUtil.isPasswordResetToken(request.getResetToken())) {
            log.warn("Reset password with token failed - invalid or expired token");
            return new ResetPasswordResponse(false, "Reset token is invalid or expired");
        }

        String username = jwtUtil.extractSubject(request.getResetToken());
        log.info("Reset password with token for username: {}", username);

        return forgotPasswordRepository.findByMobilenoOrEmailid(username)
                .map(user -> {
                    user.setPassword(argon2.hash(2, 65536, 1, request.getNewPassword().toCharArray()));
                    forgotPasswordRepository.save(user);
                    log.info("Password reset successful for username: {}", username);
                    return new ResetPasswordResponse(true, "Password reset successful");
                })
                .orElseGet(() -> {
                    log.warn("Reset password with token - user not found for username: {}", username);
                    return new ResetPasswordResponse(false, "User not found");
                });
    }

    private String generateOtp() {
        return String.format("%06d", new SecureRandom().nextInt(999999));
    }
}
