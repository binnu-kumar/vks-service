package com.vks.interfaces.google.service;

import com.vks.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleIdentityVerifierImpl implements GoogleIdentityVerifier {

    private final WebClient.Builder webClientBuilder;

    @Value("${google.client-id:}")
    private String clientId;

    @Value("${google.client-secret:}")
    private String clientSecret;

    @Value("${google.issuer:https://accounts.google.com}")
    private String issuer;

    @Value("${google.allowed-audience:}")
    private String allowedAudience;

    @Override
    public GoogleIdentity verifyAuthorizationCode(String code, String redirectUri) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new ConflictException("Google OAuth is not configured");
        }

        Map<?, ?> tokenResponse;
        try {
            tokenResponse = webClientBuilder.build()
                    .post()
                    .uri("https://oauth2.googleapis.com/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData("code", code)
                            .with("client_id", clientId)
                            .with("client_secret", clientSecret)
                            .with("redirect_uri", redirectUri)
                            .with("grant_type", "authorization_code"))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new IllegalArgumentException("Invalid Google authorization code");
        }

        if (tokenResponse == null || tokenResponse.get("id_token") == null) {
            throw new IllegalArgumentException("Google authorization code could not be exchanged");
        }

        Map<?, ?> claims;
        try {
            claims = webClientBuilder.build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("oauth2.googleapis.com")
                            .path("/tokeninfo")
                            .queryParam("id_token", tokenResponse.get("id_token"))
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (WebClientResponseException exception) {
            throw new IllegalArgumentException("Invalid Google identity token");
        }

        validateClaims(claims);
        return new GoogleIdentity(
                value(claims, "sub"),
                value(claims, "email"),
                value(claims, "given_name"),
                value(claims, "family_name"));
    }

    private void validateClaims(Map<?, ?> claims) {
        if (claims == null) {
            throw new ConflictException("Google identity could not be verified");
        }
        String claimIssuer = value(claims, "iss");
        String audience = value(claims, "aud");
        String emailVerified = value(claims, "email_verified");
        long expiry = parseLong(value(claims, "exp"));
        if (!issuer.equals(claimIssuer)
                || allowedAudience == null
                || allowedAudience.isBlank()
                || !allowedAudience.equals(audience)
                || !"true".equalsIgnoreCase(emailVerified)
                || expiry <= Instant.now().getEpochSecond()) {
            throw new ConflictException("Google identity claims are invalid or expired");
        }
    }

    private String value(Map<?, ?> claims, String key) {
        Object value = claims.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }
}
