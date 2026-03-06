package com.briefix.auth.service;

import com.briefix.auth.dto.AuthResponse;
import com.briefix.auth.dto.LoginRequest;
import com.briefix.auth.dto.RegisterRequest;
import com.briefix.auth.exception.EmailNotVerifiedException;
import com.briefix.auth.exception.InvalidVerificationTokenException;

/**
 * Application service interface defining the authentication use cases for Briefix.
 *
 * <p>Handles the full lifecycle of user authentication: account registration,
 * credential-based login, access-token refresh via a long-lived refresh token,
 * and explicit logout with server-side token invalidation.</p>
 *
 * <p>All operations accept and return DTOs exclusively — domain model objects
 * ({@code User}, etc.) must not cross this service boundary into the presentation
 * layer. This separation ensures that the REST layer remains decoupled from the
 * persistence model.</p>
 *
 * <p><b>Thread safety:</b> Implementations must be safe for concurrent use from
 * multiple request-handling threads, as Spring will typically register a single
 * bean instance.</p>
 */
public interface AuthService {

    /**
     * Registers a new local user account and issues a JWT token pair upon success.
     *
     * <p>The implementation is expected to:
     * <ol>
     *   <li>Verify that the email is not already in use.</li>
     *   <li>Hash the plain-text password before persistence.</li>
     *   <li>Persist the new user entity.</li>
     *   <li>Generate and return an access token and a refresh token.</li>
     * </ol>
     * </p>
     *
     * @param request the validated registration payload containing email, plain-text
     *                password, full name, and optional phone number; must not be {@code null}
     * @return an {@link AuthResponse} containing a signed access token, a signed refresh
     *         token, and the token type ({@code "Bearer"})
     * @throws com.briefix.auth.exception.EmailAlreadyRegisteredException if the email
     *         address in {@code request} is already associated with an existing account
     */
    void register(RegisterRequest request);

    /**
     * Authenticates an existing user by verifying their email and password, then issues
     * a fresh JWT token pair upon success.
     *
     * <p>Credential verification is delegated to Spring Security's
     * {@link org.springframework.security.authentication.AuthenticationManager}, which
     * uses BCrypt to compare the supplied plain-text password against the stored hash.</p>
     *
     * @param request the validated login payload containing the user's email and
     *                plain-text password; must not be {@code null}
     * @return an {@link AuthResponse} containing a newly signed access token, a newly
     *         signed refresh token, and the token type ({@code "Bearer"})
     * @throws org.springframework.security.authentication.BadCredentialsException if the
     *         email does not exist or the password does not match the stored hash
     */
    AuthResponse login(LoginRequest request);

    /**
     * Invalidates the provided access token by adding its {@code jti} to the
     * Redis blacklist, effectively logging the user out.
     *
     * <p>The token's {@code jti} (JWT ID) is stored in Redis with a TTL equal to the
     * token's remaining validity period. Once blacklisted, {@link com.briefix.security.JwtAuthFilter}
     * will reject the token on all subsequent requests, even though it has not yet expired
     * cryptographically. The Redis entry is automatically evicted by TTL when the token
     * would have expired naturally, preventing unbounded memory growth.</p>
     *
     * @param bearerToken the full {@code Authorization} header value received by the
     *                    controller (e.g., {@code "Bearer eyJhbGci..."}), or just the
     *                    raw token string; both forms are accepted. Must not be {@code null}.
     */
    void logout(String bearerToken);

    /**
     * Issues a new access token in exchange for a valid, non-expired refresh token.
     *
     * <p>The refresh token is validated for signature integrity and expiry. If valid,
     * a new short-lived access token is generated for the token's subject (email).
     * The original refresh token is returned unchanged — it is not rotated by this
     * implementation.</p>
     *
     * @param refreshToken the signed refresh token previously issued at login or
     *                     registration; must not be {@code null}
     * @return an {@link AuthResponse} containing the newly issued access token, the
     *         original (unchanged) refresh token, and the token type ({@code "Bearer"})
     * @throws io.jsonwebtoken.JwtException if the refresh token is malformed, the
     *         signature is invalid, or the token has expired
     */
    AuthResponse refresh(String refreshToken);

    /**
     * Verifies a user's email address using the one-time token sent at registration.
     *
     * @param token the verification token from the email link; must not be {@code null}
     * @throws InvalidVerificationTokenException if the token does not exist or has expired
     */
    void verifyEmail(String token);

    /**
     * Resends the verification email for the account associated with the given email address.
     * If the account does not exist or is already verified, the call completes silently
     * to prevent user enumeration.
     *
     * @param email the email address of the account to resend verification for
     */
    void resendVerification(String email);
}
