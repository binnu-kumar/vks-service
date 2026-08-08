package com.vks.security;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        String userId,
        String username,
        String tenantId,
        UserRole role,
        List<String> scopes
) implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return userId;
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}