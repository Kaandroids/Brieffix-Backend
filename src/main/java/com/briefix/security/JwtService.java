package com.briefix.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service responsible for all JWT operations including token generation,
 * parsing, validation, and expiration management.
 *
 * <p>Tokens are signed with HMAC-SHA256 using a configurable secret key.
 * Each token embeds a unique JWT ID ({@code jti}) to support blacklisting
 * on logout via {@link TokenBlacklistService}.</p>
 *
 * <p>Two token types are issued:
 * <ul>
 *   <li><b>Access token</b> – short-lived (default 15 min), used for API authorization.</li>
 *   <li><b>Refresh token</b> – long-lived (default 7 days), used to obtain new access tokens.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread safety:</b> This service is a stateless Spring singleton. All methods
 * are safe for concurrent use; no mutable state is held beyond the injected
 * configuration properties, which are written once at startup.</p>
 */
@Service
public class JwtService {

    /**
     * Base64-encoded HMAC-SHA256 secret key loaded from the {@code app.jwt.secret}
     * application property. This value is decoded at signing/verification time via
     * {@link #getSigningKey()}.
     */
    @Value("${app.jwt.secret}")
    private String secretKey;

    /**
     * Lifetime of a newly issued access token, expressed in milliseconds.
     * Sourced from the {@code app.jwt.access-token-expiration-ms} application property.
     * Typical production value: {@code 900000} (15 minutes).
     */
    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    /**
     * Lifetime of a newly issued refresh token, expressed in milliseconds.
     * Sourced from the {@code app.jwt.refresh-token-expiration-ms} application property.
     * Typical production value: {@code 604800000} (7 days).
     */
    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    // -------------------------------------------------------------------------
    // Token generation
    // -------------------------------------------------------------------------

    /**
     * Generates a short-lived access token for the given user.
     *
     * <p>The token is signed with the HMAC-SHA256 key configured via
     * {@code app.jwt.secret} and expires after the duration defined by
     * {@code app.jwt.access-token-expiration-ms}.</p>
     *
     * @param userDetails the authenticated user principal whose username (email)
     *                    will be embedded as the JWT {@code sub} claim; must not be {@code null}
     * @return a compact, signed JWT access token string suitable for use in an
     *         {@code Authorization: Bearer} header
     */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(userDetails, accessTokenExpirationMs);
    }

    /**
     * Generates a long-lived refresh token for the given user.
     *
     * <p>The token is signed with the same HMAC-SHA256 key as the access token but
     * carries a longer expiration defined by {@code app.jwt.refresh-token-expiration-ms}.
     * Refresh tokens are exchanged at the {@code /api/v1/auth/refresh} endpoint for new
     * access tokens without requiring the user to re-authenticate.</p>
     *
     * @param userDetails the authenticated user principal whose username (email)
     *                    will be embedded as the JWT {@code sub} claim; must not be {@code null}
     * @return a compact, signed JWT refresh token string
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails, refreshTokenExpirationMs);
    }

    /**
     * Internal factory that constructs and signs a JWT with the given expiration window.
     *
     * <p>Each token receives a randomly generated UUID as its {@code jti} claim so that
     * individual tokens can be targeted for blacklisting upon logout, independent of
     * the user's other active tokens.</p>
     *
     * @param userDetails  the principal whose email becomes the {@code sub} claim
     * @param expirationMs token lifetime in milliseconds from the current system time
     * @return a compact, URL-safe JWT string
     */
    private String buildToken(UserDetails userDetails, long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // -------------------------------------------------------------------------
    // Claims extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts the subject (email / username) from the given token.
     *
     * <p>The subject is the value stored in the JWT {@code sub} claim, which in this
     * application is always the user's email address.</p>
     *
     * @param token the compact JWT string to parse; must be non-null and properly signed
     * @return the subject claim value representing the user's email address
     * @throws io.jsonwebtoken.JwtException if the token is malformed, the signature is
     *                                      invalid, or the token has expired
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the unique token ID ({@code jti}) from the given token.
     *
     * <p>The {@code jti} is a UUID generated at token-creation time and used as the
     * blacklist key in Redis. Extracting it allows the logout flow to invalidate a
     * specific token without affecting other tokens belonging to the same user.</p>
     *
     * @param token the compact JWT string to parse; must be non-null and properly signed
     * @return the {@code jti} claim value (a UUID string)
     * @throws io.jsonwebtoken.JwtException if the token is malformed, the signature is
     *                                      invalid, or the token has expired
     */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Returns the number of seconds until the given token expires.
     *
     * <p>This value is used to set the Redis TTL when blacklisting a token on logout,
     * ensuring that blacklist entries are automatically evicted once the token would
     * have expired anyway, preventing unbounded Redis memory growth.</p>
     *
     * @param token the compact JWT string to inspect; must be non-null and properly signed
     * @return remaining lifetime in whole seconds; {@code 0} if the token is already expired
     * @throws io.jsonwebtoken.JwtException if the token is malformed or the signature is invalid
     */
    public long extractRemainingSeconds(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        long remaining = expiration.getTime() - System.currentTimeMillis();
        return Math.max(remaining / 1000, 0);
    }

    /**
     * Validates the token against the given user principal and checks its expiration.
     *
     * <p>A token is considered valid if and only if:
     * <ol>
     *   <li>The {@code sub} claim matches the username of the supplied {@code userDetails}.</li>
     *   <li>The token has not yet reached its {@code exp} (expiration) timestamp.</li>
     * </ol>
     * Note that blacklist membership is checked separately by {@link JwtAuthFilter} before
     * this method is called.</p>
     *
     * @param token       the compact JWT string to validate; must be non-null and properly signed
     * @param userDetails the user principal to match against the token's subject claim
     * @return {@code true} if the token belongs to {@code userDetails} and has not expired;
     *         {@code false} otherwise
     * @throws io.jsonwebtoken.JwtException if the token is malformed or the signature is invalid
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Determines whether the given token's expiration timestamp is in the past.
     *
     * @param token the compact JWT string to inspect
     * @return {@code true} if the token's {@code exp} claim is before the current system time
     */
    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Generic claims extractor that parses the JWT and applies the supplied resolver function.
     *
     * <p>All claim-extraction methods delegate here. The token is verified against the
     * HMAC-SHA256 signing key before any claim is read, ensuring tampered tokens are
     * rejected with a {@link io.jsonwebtoken.JwtException}.</p>
     *
     * @param <T>            the type of the extracted claim value
     * @param token          the compact JWT string to parse and verify
     * @param claimsResolver a function that maps the parsed {@link Claims} object to the
     *                       desired claim value
     * @return the result of applying {@code claimsResolver} to the verified claims
     * @throws io.jsonwebtoken.JwtException if the token is malformed, the signature does
     *                                      not match, or the token has expired
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    /**
     * Decodes the Base64-encoded secret and constructs the HMAC-SHA256 signing key
     * used for both token signing and signature verification.
     *
     * <p>The secret is sourced from the {@code app.jwt.secret} property and must be
     * a Base64-encoded string of at least 256 bits (32 bytes) to satisfy the HS256
     * minimum key length requirement.</p>
     *
     * @return the {@link SecretKey} derived from the configured HS256 secret
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
