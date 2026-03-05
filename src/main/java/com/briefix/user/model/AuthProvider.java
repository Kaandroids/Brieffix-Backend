package com.briefix.user.model;

/**
 * Represents the authentication provider used during user registration or sign-in.
 *
 * <p>This enum is used to distinguish between locally authenticated users
 * and those authenticated via third-party OAuth2 providers. It drives
 * conditional logic in authentication flows, such as skipping password
 * validation for federated identity users.</p>
 *
 * <p>The value is persisted as a string column in the {@code users} table via
 * {@code @Enumerated(EnumType.STRING)} to ensure database readability and
 * resilience against enum reordering.</p>
 *
 * <ul>
 *   <li>{@link #LOCAL} – User registered with email and password directly.</li>
 *   <li>{@link #GOOGLE} – User authenticated via Google OAuth2.</li>
 *   <li>{@link #APPLE} – User authenticated via Apple Sign-In.</li>
 * </ul>
 *
 * <p>This enum is immutable and safe for use in concurrent contexts.</p>
 */
public enum AuthProvider {

    /**
     * Standard email and password authentication managed by this application.
     * Users with this provider have a bcrypt-hashed password stored in the database
     * and are subject to local credential verification during login.
     */
    LOCAL,

    /**
     * Federated authentication delegated to Google OAuth2.
     * Users with this provider do not have a local password; their identity is
     * verified exclusively through Google's OpenID Connect token exchange.
     */
    GOOGLE,

    /**
     * Federated authentication delegated to Apple Sign-In.
     * Users with this provider do not have a local password; their identity is
     * verified through Apple's identity token issued during the Sign in with Apple flow.
     */
    APPLE
}
