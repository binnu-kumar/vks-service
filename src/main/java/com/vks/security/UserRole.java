package com.vks.security;

import java.util.List;

public enum UserRole {
    TENANT_ADMIN,
    CUSTOMER;

    public List<String> defaultScopes() {
        return switch (this) {
            case TENANT_ADMIN -> List.of("events:read", "events:write", "slots:read", "slots:write", "bookings:read");
            case CUSTOMER -> List.of("events:read", "slots:read", "bookings:read", "bookings:write");
        };
    }
}