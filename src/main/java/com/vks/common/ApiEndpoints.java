package com.vks.common;

public final class ApiEndpoints {

    private ApiEndpoints() {}

    public static final String BASE_AUTH = "/api/v1/auth";

    public static final String SIGNUP = "/signup";
    public static final String LOGIN = "/login";

    public static final String SIGNUP_FULL = BASE_AUTH + SIGNUP;
    public static final String LOGIN_FULL = BASE_AUTH + LOGIN;

    // Actuator
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_INFO = "/actuator/info";

    // Swagger
    public static final String API_DOCS = "/api-docs/**";
    public static final String SWAGGER_UI = "/swagger-ui/**";
    public static final String SWAGGER_UI_HTML = "/swagger-ui.html";
}
