package com.briefix.auth.exception;

/**
 * Thrown when an authentication attempt is made for a user account whose email
 * address has not yet been verified.
 *
 * <p>This exception is raised by
 * {@link com.briefix.auth.service.AuthServiceImpl#login(com.briefix.auth.dto.LoginRequest)}
 * when the resolved user record has {@code emailVerified = false}. Callers should
 * prompt the user to check their inbox or request a verification-email resend.</p>
 *
 * <p>The global exception handler maps this exception to an appropriate HTTP error
 * response (typically {@code 403 Forbidden} or {@code 401 Unauthorized}) so that the
 * frontend can distinguish it from a wrong-password failure.</p>
 *
 * <p>This class is an unchecked exception and need not be declared in {@code throws}
 * clauses.</p>
 */
public class EmailNotVerifiedException extends RuntimeException {

    /**
     * Constructs an {@code EmailNotVerifiedException} for the specified email address.
     *
     * @param email the unverified email address; included verbatim in the exception message
     */
    public EmailNotVerifiedException(String email) {
        super("Email address is not verified: " + email);
    }
}
