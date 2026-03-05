package com.briefix.auth.dto;

/**
 * Immutable response payload returned to the client after a successful authentication
 * operation (registration, login, or token refresh).
 *
 * <p>Contains both a short-lived access token for immediate API authorization and a
 * long-lived refresh token for obtaining new access tokens without re-authentication.
 * The {@code tokenType} field is always {@code "Bearer"} for compatibility with the
 * OAuth2 Bearer Token specification (RFC 6750).</p>
 *
 * <p>Clients should:
 * <ul>
 *   <li>Include the {@code accessToken} in the {@code Authorization: Bearer <token>}
 *       header of every authenticated API request.</li>
 *   <li>Store the {@code refreshToken} securely (e.g., in an {@code HttpOnly} cookie
 *       or secure storage) and use it at {@code POST /api/v1/auth/refresh} when the
 *       access token expires.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread safety:</b> Java records are immutable by design; instances of this
 * class are safe to share across threads.</p>
 *
 * @param accessToken  Short-lived JWT (default 15 minutes) for authorizing API requests.
 *                     Must be sent as a Bearer token in the {@code Authorization} header.
 * @param refreshToken Long-lived JWT (default 7 days) for obtaining a new access token
 *                     without requiring the user to re-enter credentials.
 * @param tokenType    The token scheme; always {@code "Bearer"} for OAuth2 compliance.
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType
) {
    /**
     * Convenience factory method that constructs an {@code AuthResponse} with
     * {@code tokenType} pre-set to {@code "Bearer"}.
     *
     * <p>This is the preferred construction path for all service-layer code to
     * avoid hard-coding the token type string in multiple places.</p>
     *
     * @param accessToken  the signed JWT access token to include in the response;
     *                     must not be {@code null}
     * @param refreshToken the signed JWT refresh token to include in the response;
     *                     must not be {@code null}
     * @return a new {@code AuthResponse} with the supplied tokens and {@code tokenType}
     *         set to {@code "Bearer"}
     */
    public static AuthResponse of(String accessToken, String refreshToken) {
        return new AuthResponse(accessToken, refreshToken, "Bearer");
    }
}
