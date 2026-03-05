package com.briefix.user.dto;

import com.briefix.user.model.AuthProvider;
import com.briefix.user.model.UserPlan;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a sanitized, outbound view of a {@link com.briefix.user.model.User}.
 *
 * <p>This record is the canonical representation of user data exposed through
 * the application's REST API layer. It deliberately excludes sensitive fields such
 * as {@code passwordHash} and {@code providerId} to prevent accidental
 * exposure of credentials or federated identity tokens to API consumers.</p>
 *
 * <p>Instances are produced exclusively by {@link com.briefix.user.mapper.UserMapper#toDto(com.briefix.user.model.User)}
 * and should be the only user-related type returned from service or controller layers.
 * Controllers must never return the raw {@link com.briefix.user.model.User} domain record
 * or the {@link com.briefix.user.entity.UserEntity} directly.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 *
 * @param id               Unique identifier of the user account (UUID v4).
 * @param email            The verified primary email address associated with this account.
 * @param provider         The authentication provider ({@code LOCAL}, {@code GOOGLE}, or {@code APPLE})
 *                         through which this account was created.
 * @param isEmailVerified  {@code true} if the user has completed email verification; {@code false} otherwise.
 * @param fullName         The user's display name as shown in the UI and generated documents.
 * @param phone            The user's optional contact phone number; may be {@code null} if not provided.
 * @param plan             The subscription plan tier currently active for this user account.
 * @param createdAt        The UTC-aligned server timestamp at which the account was first registered.
 */
public record UserDto(
        UUID id,
        String email,
        AuthProvider provider,
        boolean isEmailVerified,
        String fullName,
        String phone,
        UserPlan plan,
        LocalDateTime createdAt
) {}
