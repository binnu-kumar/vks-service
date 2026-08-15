package com.vks.common;

public final class ApiEndpoints {

    private ApiEndpoints() {}

    public static final String BASE_AUTH = "/api/v1/auth";
    public static final String BASE_TENANT_ADMIN = "/api/v1/tenant-admin";
    public static final String BASE_CUSTOMER = "/api/v1/customer";

    public static final String SIGNUP = "/signup";
    public static final String ADMIN_SIGNUP = "/tenant-admin/signup";
    public static final String LOGIN = "/login";
    public static final String REFRESH = "/refresh";
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String VERIFY_OTP = "/verify-otp";
    public static final String RESET_PASSWORD_WITH_TOKEN = "/reset-password-with-token";

    public static final String EVENTS = "/events";
    public static final String EVENTS_BY_ID = "/events/{eventId}";
    public static final String EVENTS_SEARCH = "/events/search";
    public static final String SLOTS = "/events/{eventId}/slots";
    public static final String SLOTS_BY_ID = "/events/{eventId}/slots/{slotId}";
    public static final String BOOKINGS = "/bookings";
    public static final String MY_BOOKINGS = "/bookings";
    public static final String BOOKING_BY_ID = "/bookings/{bookingId}";
    public static final String BOOKINGS_CANCEL = "/bookings/{bookingId}/cancel";
    public static final String CUSTOMER_PROFILE = "/profile";

    // Internal — called by payment-service
    public static final String BASE_INTERNAL = "/api/v1/internal";
    public static final String BOOKING_CONFIRM_INTERNAL = "/bookings/{bookingId}/confirm";

    public static final String SIGNUP_FULL = BASE_AUTH + SIGNUP;
    public static final String ADMIN_SIGNUP_FULL = BASE_AUTH + ADMIN_SIGNUP;
    public static final String LOGIN_FULL = BASE_AUTH + LOGIN;
    public static final String REFRESH_FULL = BASE_AUTH + REFRESH;
    public static final String RESET_PASSWORD_FULL = BASE_AUTH + RESET_PASSWORD;
    public static final String FORGOT_PASSWORD_FULL = BASE_AUTH + FORGOT_PASSWORD;
    public static final String VERIFY_OTP_FULL = BASE_AUTH + VERIFY_OTP;
    public static final String RESET_PASSWORD_WITH_TOKEN_FULL = BASE_AUTH + RESET_PASSWORD_WITH_TOKEN;

    // Actuator
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_INFO = "/actuator/info";

    // Swagger
    public static final String API_DOCS = "/api-docs/**";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
}
