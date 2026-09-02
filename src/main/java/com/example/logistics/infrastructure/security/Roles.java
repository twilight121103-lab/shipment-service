package com.example.logistics.infrastructure.security;

/**
 * RBAC role authority names used by Spring authorization.
 *
 * <p>Roles are assigned to Keycloak users (as realm roles or client roles); the JWT is
 * expected to carry them as granted authorities. Prefixing with {@code ROLE_} keeps the
 * role claims compatible with Spring Security's {@code hasRole}.
 */
public final class Roles {

    public static final String USER = "LOGISTICS_USER";
    public static final String OPERATOR = "LOGISTICS_OPERATOR";
    public static final String ADMIN = "LOGISTICS_ADMIN";

    public static final String ROLE_USER = "ROLE_" + USER;
    public static final String ROLE_OPERATOR = "ROLE_" + OPERATOR;
    public static final String ROLE_ADMIN = "ROLE_" + ADMIN;

    private Roles() {
    }
}
