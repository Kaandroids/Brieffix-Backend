package com.briefix.user.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing an authenticated user of the Briefix platform.
 *
 * <p>This record serves as the core aggregate root of the user domain.
 * It supports both locally registered users (email + password) and
 * federated identity users authenticated via OAuth2 providers such as
 * Google or Apple.</p>
 *
 * <p>Instances of this record are immutable by design. Any state change
 * (e.g., verifying email, updating profile) must produce a new instance
 * rather than mutating an existing one, consistent with value-object semantics.</p>
 *
 * <p>Validation constraints are applied to fields that are required at
 * the domain boundary. Fields such as {@code passwordHash} and
 * {@code providerId} are intentionally nullable to accommodate both
 * local and federated authentication flows.</p>
 *
 * <p>This record is not intended for direct serialization to API consumers.
 * Use {@link com.briefix.user.dto.UserDto} for outbound representations to
 * avoid accidental exposure of sensitive fields such as {@code passwordHash}.</p>
 *
 * <p>Thread-safety: Records in Java are inherently immutable and therefore
 * safe to share across threads without additional synchronization.</p>
 *
 * @param id               Unique identifier for the user (UUID v4). Generated on first persistence.
 * @param email            Primary email address. Must be non-blank and well-formed per RFC 5322.
 * @param passwordHash     Bcrypt-hashed password. {@code null} for OAuth2 (federated) users.
 * @param provider         The authentication provider used to create this account. Must not be {@code null}.
 * @param providerId       The subject identifier issued by the OAuth2 provider. {@code null} for local users.
 * @param isEmailVerified  Indicates whether the user's email address has been confirmed via verification link.
 * @param fullName         The user's display name shown across the platform. Must be non-blank.
 * @param phone            Optional phone number. When provided, must conform to E.164-compatible format (7–15 digits, optional leading {@code +}).
 * @param plan                      The subscription plan tier currently assigned to this user. Defaults to {@link UserPlan#STANDARD}.
 * @param createdAt                 Timestamp of account creation in the server's local time. Set server-side at registration; never supplied by clients.
 * @param verificationToken         One-time token sent by email to verify account ownership. {@code null} once verified or for OAuth2 users.
 * @param verificationTokenExpiry   Expiry timestamp for {@code verificationToken}. Tokens past this time are rejected.
 */
public record User(

        UUID id,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        String passwordHash,

        @NotNull(message = "Auth provider is required")
        AuthProvider provider,

        String providerId,

        boolean isEmailVerified,

        @NotBlank(message = "Full name is required")
        String fullName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Phone must be a valid phone number")
        String phone,

        UserPlan plan,

        LocalDateTime createdAt,

        String verificationToken,

        LocalDateTime verificationTokenExpiry
) {}
