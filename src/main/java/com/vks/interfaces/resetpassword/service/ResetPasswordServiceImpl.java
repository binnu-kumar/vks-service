package com.vks.interfaces.resetpassword.service;

import com.vks.interfaces.resetpassword.model.ResetPasswordRequest;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;
import com.vks.interfaces.resetpassword.repository.ResetPasswordRepository;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResetPasswordServiceImpl implements ResetPasswordService {

    private final ResetPasswordRepository resetPasswordRepository;
    private final SecurityContextService securityContextService;

    private final Argon2 argon2 = Argon2Factory.create();

    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Reset password attempt for authenticated user id: {}", currentUser.userId());

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Reset password failed - password mismatch for authenticated user id: {}", currentUser.userId());
            return new ResetPasswordResponse(false, "New password and confirm password do not match");
        }

        Optional<SignupEntity> userOpt = resetPasswordRepository.findById(Long.valueOf(currentUser.userId()));

        if (userOpt.isEmpty()) {
            log.warn("Reset password failed - user not found for authenticated user id: {}", currentUser.userId());
            return new ResetPasswordResponse(false, "User not found");
        }

        SignupEntity user = userOpt.get();
        user.setPassword(argon2.hash(2, 65536, 1, request.getNewPassword().toCharArray()));
        resetPasswordRepository.save(user);

        log.info("Password reset successful for authenticated user id: {}", currentUser.userId());
        return new ResetPasswordResponse(true, "Password reset successful");
    }
}
