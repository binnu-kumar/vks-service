package com.vks.interfaces.google.service;

public interface GoogleIdentityVerifier {

    GoogleIdentity verifyAuthorizationCode(String code, String redirectUri);
}
