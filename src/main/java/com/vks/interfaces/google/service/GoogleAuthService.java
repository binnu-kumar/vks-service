package com.vks.interfaces.google.service;

import com.vks.interfaces.google.model.GoogleAuthRequest;
import com.vks.interfaces.login.model.LoginResponse;

public interface GoogleAuthService {

    LoginResponse authenticate(GoogleAuthRequest request);
}
