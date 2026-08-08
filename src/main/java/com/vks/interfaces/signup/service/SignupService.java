package com.vks.interfaces.signup.service;

import com.vks.interfaces.signup.model.AdminSignupRequest;
import com.vks.interfaces.signup.model.SignupRequest;
import com.vks.interfaces.signup.model.SignupResponse;

public interface SignupService {

    SignupResponse signup(SignupRequest request);

    SignupResponse adminSignup(AdminSignupRequest request);
}
