package com.briefix.profile.exception;

import java.util.UUID;

/**
 * Unchecked exception thrown when a requested profile cannot be located in the system.
 *
 * <p>This exception is surfaced by the service and repository layers when a lookup
 * by UUID returns no result. It is intended to be caught by the application's global
 * exception handler (e.g., a {@code @ControllerAdvice} class) and mapped to an HTTP
 * {@code 404 Not Found} response with an appropriate error body.</p>
 *
 * <p>As a {@link RuntimeException}, this exception does not need to be declared in
 * method signatures and propagates naturally up the call stack until handled by the
 * exception handler. It is deliberately unchecked to keep service and controller
 * method signatures clean.</p>
 *
 * <p>Thread-safety: Exception instances are immutable after construction and are
 * safe to share across threads, although this is atypical in practice.</p>
 */
public class ProfileNotFoundException extends RuntimeException {

    /**
     * Constructs a {@code ProfileNotFoundException} for a failed lookup by profile UUID.
     *
     * <p>The resulting detail message follows the pattern:
     * {@code "Profile not found: <id>"}, providing enough context for log
     * correlation and for inclusion in client-facing error responses.</p>
     *
     * @param id the UUID that was searched for but did not match any profile record;
     *           must not be {@code null}
     */
    public ProfileNotFoundException(UUID id) {
        super("Profile not found: " + id);
    }
}
