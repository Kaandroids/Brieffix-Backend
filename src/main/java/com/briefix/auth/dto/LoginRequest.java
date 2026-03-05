package com.briefix.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Immutable request payload for the login endpoint
 * ({@code POST /api/v1/auth/login}).
 *
 * <p>Both fields are validated at the controller boundary via Bean Validation
 * ({@code @Valid}). Validation failures produce an HTTP 400 response. If the
 * credentials are structurally valid but incorrect, the service layer throws a
 * {@link org.springframework.security.authentication.BadCredentialsException},
 * which is mapped to HTTP 401 by
 * {@link com.briefix.common.GlobalExceptionHandler}.</p>
 *
 * <p>The {@code password} field is transmitted in plain text over HTTPS and
 * must never be logged or persisted. Credential verification is performed by
 * Spring Security's {@link org.springframework.security.authentication.AuthenticationManager}
 * using BCrypt comparison against the stored hash.</p>
 *
 * <p><b>Thread safety:</b> Java records are immutable by design; instances of this
 * class are safe to share across threads.</p>
 *
 * @param email    The registered email address of the account to authenticate.
 *                 Must not be blank and must conform to standard email format.
 * @param password The plain-text password to authenticate with. Must not be blank.
 *                 Compared against the BCrypt-hashed value stored in the database.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}
