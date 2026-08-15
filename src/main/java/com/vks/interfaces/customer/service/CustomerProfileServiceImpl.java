package com.vks.interfaces.customer.service;

import com.vks.common.exception.ResourceNotFoundException;
import com.vks.interfaces.customer.model.CustomerProfileResponse;
import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.repository.SignupRepository;
import com.vks.security.AuthenticatedUser;
import com.vks.security.SecurityContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final SignupRepository signupRepository;
    private final SecurityContextService securityContextService;

    @Override
    public CustomerProfileResponse getProfile() {
        AuthenticatedUser currentUser = securityContextService.currentUser();
        log.info("Fetching profile for user: {}", currentUser.userId());

        SignupEntity user = signupRepository.findById(Long.valueOf(currentUser.userId()))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUser.userId()));

        return new CustomerProfileResponse(
                currentUser.userId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmailid(),
                user.getMobileno(),
                user.getTenantId(),
                user.getRole()
        );
    }
}
