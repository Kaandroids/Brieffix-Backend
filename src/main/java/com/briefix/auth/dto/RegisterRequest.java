package com.briefix.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Immutable request payload for the user registration endpoint
 * ({@code POST /api/v1/auth/register}).
 *
 * <p>All fields are validated at the controller boundary via Bean Validation
 * ({@code @Valid}) before reaching the service layer. Validation failures result
 * in an HTTP 400 response with a structured error body produced by
 * {@link com.briefix.common.GlobalExceptionHandler}.</p>
 *
 * <p>The {@code password} field is accepted in plain text and is BCrypt-hashed
 * by {@link com.briefix.auth.service.AuthServiceImpl} before being persisted.
 * It must never be logged or stored in its raw form.</p>
 *
 * <p><b>Thread safety:</b> Java records are immutable by design; instances of this
 * class are safe to share across threads.</p>
 *
 * @param email    A unique, well-formed email address that will serve as the user's
 *                 login identifier. Must not be blank and must conform to standard
 *                 email format as validated by {@code @Email}.
 * @param password Plain-text password provided by the user during sign-up.
 *                 Must not be blank and must be at least 8 characters long.
 *                 Hashed with BCrypt before persistence.
 * @param fullName The user's display name shown throughout the application.
 *                 Must not be blank.
 * @param phone    Optional phone number for the account. When provided, must match
 *                 an E.164-compatible pattern ({@code +?[0-9]{7,15}}).
 *                 May be {@code null} if the user chooses not to supply a phone number.
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Full name is required")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be a valid phone number")
        String phone
) {}
