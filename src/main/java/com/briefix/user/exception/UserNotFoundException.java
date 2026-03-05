package com.briefix.user.exception;

import java.util.UUID;

/**
 * Unchecked exception thrown when a requested user cannot be located in the system.
 *
 * <p>This exception is surfaced by the service and repository layers when a lookup
 * by UUID or email address returns no result. It is intended to be caught by the
 * application's global exception handler (e.g., a {@code @ControllerAdvice} class)
 * and mapped to an HTTP {@code 404 Not Found} response with an appropriate error body.</p>
 *
 * <p>Two constructors are provided to support the two primary lookup strategies:
 * by {@link UUID} (used in direct ID-based queries) and by email address
 * (used in authentication and profile resolution flows).</p>
 *
 * <p>As a {@link RuntimeException}, this exception does not need to be declared in
 * method signatures and can propagate up the call stack until handled.</p>
 *
 * <p>Thread-safety: Exception instances are immutable after construction and safe
 * to share across threads, though doing so is atypical.</p>
 */
public class UserNotFoundException extends RuntimeException {

    /**
     * Constructs a {@code UserNotFoundException} for a failed lookup by unique identifier.
     *
     * <p>The resulting message follows the pattern:
     * {@code "User not found with id: <id>"}, which provides enough context
     * for log correlation and client-facing error responses.</p>
     *
     * @param id the UUID that was searched for but did not match any user record;
     *           must not be {@code null}
     */
    public UserNotFoundException(UUID id) {
        super("User not found with id: " + id);
    }

    /**
     * Constructs a {@code UserNotFoundException} for a failed lookup by email address.
     *
     * <p>The resulting message follows the pattern:
     * {@code "User not found with email: <email>"}. Note that this message
     * will be included in server logs; ensure that logging configuration avoids
     * emitting this at a level visible in production outputs if email addresses
     * are considered personally identifiable information.</p>
     *
     * @param email the email address that was searched for but did not match any
     *              user record; must not be {@code null}
     */
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
