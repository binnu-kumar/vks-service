package com.vks.interfaces.signup.service;

import com.vks.interfaces.signup.entity.SignupEntity;
import com.vks.interfaces.signup.model.SignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;
import com.vks.interfaces.signup.repository.SignupRepository;
import de.mkammerer.argon2.Argon2Factory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SignupServiceImplTest {

    @Mock
    private SignupRepository signupRepository;

    @InjectMocks
    private SignupServiceImpl signupService;

    @Test
    void signupReturnsFailureWhenPasswordsDoNotMatch() {
        SignupRequest request = signupRequest();
        request.setConfirmPassword("Mismatch@123");

        SignupResponse response = signupService.signup(request);

        assertFalse(response.isSuccess());
        assertEquals("Password and confirm password do not match", response.getMessage());
        verify(signupRepository, never()).save(any(SignupEntity.class));
    }

    @Test
    void signupReturnsFailureWhenMobileAlreadyExists() {
        SignupRequest request = signupRequest();
        when(signupRepository.existsByMobileno(request.getMobileno())).thenReturn(true);

        SignupResponse response = signupService.signup(request);

        assertFalse(response.isSuccess());
        assertEquals("Mobile number already registered", response.getMessage());
        verify(signupRepository, never()).save(any(SignupEntity.class));
    }

    @Test
    void signupHashesPasswordAndSavesUser() {
        SignupRequest request = signupRequest();
        when(signupRepository.existsByMobileno(request.getMobileno())).thenReturn(false);

        SignupResponse response = signupService.signup(request);

        ArgumentCaptor<SignupEntity> captor = ArgumentCaptor.forClass(SignupEntity.class);
        verify(signupRepository).save(captor.capture());
        SignupEntity saved = captor.getValue();

        assertTrue(response.isSuccess());
        assertEquals("User registered successfully", response.getMessage());
        assertEquals(request.getFirstname(), saved.getFirstname());
        assertEquals(request.getLastname(), saved.getLastname());
        assertEquals(request.getMobileno(), saved.getMobileno());
        assertEquals(request.getEmailid(), saved.getEmailid());
        assertTrue(Argon2Factory.create().verify(saved.getPassword(), request.getPassword().toCharArray()));
    }

    private SignupRequest signupRequest() {
        SignupRequest request = new SignupRequest();
        request.setFirstname("John");
        request.setLastname("Doe");
        request.setMobileno("9876543210");
        request.setEmailid("john@example.com");
        request.setPassword("Pass@1234");
        request.setConfirmPassword("Pass@1234");
        return request;
    }
}