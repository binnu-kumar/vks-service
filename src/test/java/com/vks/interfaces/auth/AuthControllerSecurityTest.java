package com.vks.interfaces.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vks.interfaces.forgotpassword.model.ForgotPasswordResponse;
import com.vks.interfaces.forgotpassword.service.ForgotPasswordService;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.service.LoginService;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;
import com.vks.interfaces.resetpassword.service.ResetPasswordService;
import com.vks.interfaces.signup.model.AdminSignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;
import com.vks.interfaces.signup.service.SignupService;
import com.vks.security.AuthenticatedUser;
import com.vks.security.JwtUtil;
import com.vks.security.SecurityConfig;
import com.vks.security.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        com.vks.interfaces.signup.controller.SignupController.class,
        com.vks.interfaces.login.controller.LoginController.class,
        com.vks.interfaces.forgotpassword.controller.ForgotPasswordController.class,
        com.vks.interfaces.resetpassword.controller.ResetPasswordController.class
}, properties = {
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "spring.cloud.config.import-check.enabled=false",
        "spring.config.import="
})
@Import(SecurityConfig.class)
class AuthControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SignupService signupService;

    @MockBean
    private LoginService loginService;

    @MockBean
    private ForgotPasswordService forgotPasswordService;

    @MockBean
    private ResetPasswordService resetPasswordService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    void signupIsPublicAndReturnsOk() throws Exception {
        when(signupService.signup(any())).thenReturn(new SignupResponse(true, "User registered successfully"));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SignupPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void signupReturnsBadRequestOnValidationFailure() throws Exception {
        SignupPayload payload = new SignupPayload();
        payload.firstname = "";

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verify(signupService, never()).signup(any());
    }

    @Test
    void forgotPasswordIsPublicAndReturnsOk() throws Exception {
        when(forgotPasswordService.forgotPassword(any()))
                .thenReturn(new ForgotPasswordResponse(true, "OTP sent to registered email address. Valid for 5 minutes."));

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginIsPublicAndReturnsOk() throws Exception {
        when(loginService.login(any()))
                                .thenReturn(new LoginResponse(true, "Login successful", "access", "refresh", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes()));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"9876543210\",\"password\":\"Pass@1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    void adminSignupIsPublicAndReturnsOk() throws Exception {
        when(signupService.adminSignup(any(AdminSignupRequest.class)))
                .thenReturn(new SignupResponse(true, "Tenant admin registered successfully"));

        mockMvc.perform(post("/api/v1/auth/tenant-admin/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminSignupPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void resetPasswordIsProtectedWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"NewPass@123\",\"confirmPassword\":\"NewPass@123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void resetPasswordAllowsAuthenticatedRequest() throws Exception {
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.isAccessToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractAuthenticatedUser("valid-token")).thenReturn(
                new AuthenticatedUser("42", "9876543210", "tenant-123", UserRole.CUSTOMER, UserRole.CUSTOMER.defaultScopes()));
        when(resetPasswordService.resetPassword(any()))
                .thenReturn(new ResetPasswordResponse(true, "Password reset successful"));

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"NewPass@123\",\"confirmPassword\":\"NewPass@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    static class SignupPayload {
        public String firstname = "John";
        public String lastname = "Doe";
                public String tenantId = "tenant-123";
        public String mobileno = "9876543210";
        public String emailid = "john@example.com";
        public String password = "Pass@1234";
        public String confirmPassword = "Pass@1234";
    }

        static class AdminSignupPayload extends SignupPayload {
                public String onboardingSecret = "admin-bootstrap-secret";
        }
}