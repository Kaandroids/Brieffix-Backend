package com.briefix.auth.exception;

/**
 * Thrown when Google OAuth2 authentication fails for any reason.
 *
 * <p>This exception is raised by
 * {@link com.briefix.auth.service.AuthServiceImpl#verifyGoogleToken(String)} when
 * the supplied Google ID token cannot be verified (e.g., invalid signature, wrong
 * audience, or network error), and also when the email extracted from a valid token
 * is already registered with a non-Google authentication provider, which would
 * constitute an account-takeover attempt.</p>
 *
 * <p>The global exception handler maps this exception to a {@code 401 Unauthorized}
 * or {@code 400 Bad Request} HTTP response so that the frontend can display an
 * appropriate error message to the user.</p>
 *
 * <p>This class is an unchecked exception and need not be declared in {@code throws}
 * clauses.</p>
 */
public class GoogleAuthException extends RuntimeException {

    /**
     * Constructs a {@code GoogleAuthException} with the given detail message.
     *
     * @param message a human-readable description of the authentication failure;
     *                must not be {@code null}
     */
    public GoogleAuthException(String message) {
        super(message);
    }
}
