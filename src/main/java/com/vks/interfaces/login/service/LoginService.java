package com.vks.interfaces.login.service;

import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;
import com.vks.interfaces.login.model.RefreshTokenRequest;

public interface LoginService {

    LoginResponse login(LoginRequest request);

    LoginResponse refresh(RefreshTokenRequest request);
}
