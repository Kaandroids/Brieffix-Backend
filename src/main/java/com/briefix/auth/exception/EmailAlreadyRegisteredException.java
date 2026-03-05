package com.briefix.auth.exception;

/**
 * Unchecked exception thrown during user registration when the provided email address
 * is already associated with an existing account in the system.
 *
 * <p>This exception is thrown by {@link com.briefix.auth.service.AuthServiceImpl#register}
 * after confirming via a database uniqueness check that the requested email is taken.
 * It is mapped to an HTTP 409 Conflict response by
 * {@link com.briefix.common.GlobalExceptionHandler#handleEmailTaken}.</p>
 *
 * <p><b>Thread safety:</b> As an immutable {@link RuntimeException} subclass, instances
 * of this class are safe to share across threads.</p>
 */
public class EmailAlreadyRegisteredException extends RuntimeException {

    /**
     * Constructs a new {@code EmailAlreadyRegisteredException} with a detail message
     * that identifies the conflicting email address.
     *
     * <p>The message is surfaced directly to API clients in the RFC 7807 problem detail
     * response body, so it intentionally includes the email address to allow clients
     * to display a meaningful error (e.g., "try logging in instead").</p>
     *
     * @param email the email address that is already registered; included verbatim in
     *              the exception message and the resulting HTTP response body
     */
    public EmailAlreadyRegisteredException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}
