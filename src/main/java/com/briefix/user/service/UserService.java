package com.briefix.user.service;

import com.briefix.user.dto.UserDto;

import java.util.UUID;

/**
 * Application service interface defining the use cases available for the user domain.
 *
 * <p>This interface represents the primary entry point for user-related business
 * operations within the application layer. It operates exclusively on
 * {@link UserDto} objects to ensure that domain internals (e.g., password hashes,
 * provider tokens) are never leaked to the presentation layer.</p>
 *
 * <p>Implementations of this interface are responsible for orchestrating
 * domain logic, delegating persistence to {@link com.briefix.user.repository.UserRepository},
 * and applying any cross-cutting concerns such as authorization checks or
 * audit event publishing. The default implementation is
 * {@link UserServiceImpl}.</p>
 *
 * <p>Controllers and other components in the presentation layer must depend on
 * this interface rather than any concrete implementation to preserve testability
 * and architectural decoupling.</p>
 *
 * <p>Thread-safety: Implementations are expected to be stateless Spring
 * {@code @Service} beans and safe for concurrent use.</p>
 */
public interface UserService {

    /**
     * Retrieves a user by their unique account identifier.
     *
     * <p>This operation is typically used in contexts where the caller already holds
     * a known UUID (e.g., fetched from a JWT claim or another aggregate's foreign key)
     * and needs to load the full user profile.</p>
     *
     * @param id the UUID of the user to retrieve; must not be {@code null}
     * @return a {@link UserDto} representing the found user, with sensitive fields excluded
     * @throws com.briefix.user.exception.UserNotFoundException if no user exists with the given ID
     */
    UserDto findById(UUID id);

    /**
     * Retrieves a user by their registered email address.
     *
     * <p>Typically used during authentication flows to look up an account prior
     * to credential verification, or in secured endpoints where the authenticated
     * principal's email is extracted from the security context.</p>
     *
     * @param email the email address to search for; must not be {@code null} or blank
     * @return a {@link UserDto} representing the found user, with sensitive fields excluded
     * @throws com.briefix.user.exception.UserNotFoundException if no user exists with the given email
     */
    UserDto findByEmail(String email);
}
