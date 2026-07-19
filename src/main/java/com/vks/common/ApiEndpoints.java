package com.vks.common;

public final class ApiEndpoints {

    private ApiEndpoints() {}

    public static final String BASE_AUTH = "/api/v1/auth";

    public static final String SIGNUP = "/signup";
    public static final String LOGIN = "/login";
    public static final String RESET_PASSWORD = "/reset-password";
    public static final String FORGOT_PASSWORD = "/forgot-password";
    public static final String VERIFY_OTP = "/verify-otp";
    public static final String RESET_PASSWORD_WITH_TOKEN = "/reset-password-with-token";

    public static final String SIGNUP_FULL = BASE_AUTH + SIGNUP;
    public static final String LOGIN_FULL = BASE_AUTH + LOGIN;
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
