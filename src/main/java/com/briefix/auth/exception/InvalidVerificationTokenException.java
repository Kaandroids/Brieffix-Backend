package com.briefix.auth.exception;

/**
 * Thrown when an email-verification token is unknown, has already been consumed,
 * or has passed its 24-hour expiry window.
 *
 * <p>This exception is raised by
 * {@link com.briefix.auth.service.AuthServiceImpl#verifyEmail(String)} in two
 * scenarios:</p>
 * <ol>
 *   <li>No user record is found for the supplied token string (token is unknown
 *       or was already cleared after a previous successful verification).</li>
 *   <li>The token's {@code verificationTokenExpiry} timestamp is in the past.</li>
 * </ol>
 *
 * <p>The global exception handler maps this exception to a {@code 400 Bad Request}
 * HTTP response. Callers should direct the user to request a new verification email
 * via the resend endpoint.</p>
 *
 * <p>This class is an unchecked exception and need not be declared in {@code throws}
 * clauses.</p>
 */
public class InvalidVerificationTokenException extends RuntimeException {

    /**
     * Constructs an {@code InvalidVerificationTokenException} with a fixed detail
     * message indicating that the token is invalid or expired.
     */
    public InvalidVerificationTokenException() {
        super("Verification token is invalid or has expired");
    }
}
