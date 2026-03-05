package com.briefix.security;

/**
 * Port defining the contract for JWT token blacklisting.
 *
 * <p>When a user logs out, the token's {@code jti} (JWT ID) is stored in this
 * blacklist with a TTL matching the token's remaining validity period.
 * The {@link JwtAuthFilter} checks this blacklist on every authenticated request
 * before granting access to protected resources, ensuring that a logged-out token
 * cannot be reused even if it has not yet expired.</p>
 *
 * <p>Implementations are expected to provide efficient, low-latency lookups suitable
 * for use within the per-request filter chain. The reference implementation
 * {@link TokenBlacklistServiceImpl} is backed by Redis and performs O(1) key
 * existence checks.</p>
 *
 * <p><b>Thread safety:</b> Implementations must be safe for concurrent use from
 * multiple request-handling threads.</p>
 */
public interface TokenBlacklistService {

    /**
     * Adds a token's unique identifier to the blacklist with an expiring TTL.
     *
     * <p>The {@code ttlSeconds} value should be set to the token's remaining lifetime
     * so that the blacklist entry is automatically evicted once the token would have
     * expired naturally, preventing unbounded growth of the underlying store.</p>
     *
     * @param jti        the JWT ID ({@code jti} claim) uniquely identifying the token
     *                   to blacklist; must not be {@code null} or empty
     * @param ttlSeconds the duration in seconds to retain the blacklist entry; must be
     *                   greater than zero — entries with a zero or negative TTL may be
     *                   discarded immediately by the underlying store
     */
    void blacklist(String jti, long ttlSeconds);

    /**
     * Checks whether a token has been explicitly blacklisted (e.g., via logout).
     *
     * <p>This method is called on every request that carries a {@code Bearer} token.
     * Returning {@code true} causes the {@link JwtAuthFilter} to reject the token and
     * leave the security context unauthenticated, resulting in a 401 response for
     * protected resources.</p>
     *
     * @param jti the JWT ID ({@code jti} claim) to check; must not be {@code null}
     * @return {@code true} if the token identified by {@code jti} has been blacklisted
     *         and should be rejected; {@code false} if the token is not on the blacklist
     */
    boolean isBlacklisted(String jti);
}
