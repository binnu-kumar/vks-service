package com.vks.interfaces.login.service;

import com.vks.interfaces.login.model.LoginRequest;
import com.vks.interfaces.login.model.LoginResponse;

public interface LoginService {

    LoginResponse login(LoginRequest request);
}
