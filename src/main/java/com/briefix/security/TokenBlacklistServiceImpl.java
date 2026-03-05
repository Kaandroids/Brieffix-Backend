package com.briefix.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed implementation of {@link TokenBlacklistService}.
 *
 * <p>Blacklisted token IDs are stored as Redis string keys with the prefix
 * {@code jwt:blacklist:} and a TTL equal to the token's remaining validity period.
 * Expired entries are automatically evicted by Redis, keeping the memory footprint
 * proportional to the number of currently active (but logged-out) tokens rather than
 * the total number of tokens ever issued.</p>
 *
 * <p>Key format: {@code jwt:blacklist:{jti}}, e.g.,
 * {@code jwt:blacklist:550e8400-e29b-41d4-a716-446655440000}.</p>
 *
 * <p><b>Thread safety:</b> This class is a stateless Spring singleton. All Redis
 * operations are delegated to {@link StringRedisTemplate}, which is itself
 * thread-safe. Concurrent invocations of {@link #blacklist} and {@link #isBlacklisted}
 * are therefore safe.</p>
 */
@Service
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    /**
     * Namespace prefix prepended to every Redis key managed by this service.
     * Scoping keys under {@code jwt:blacklist:} prevents collisions with other
     * Redis key spaces used by the application.
     */
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    /**
     * Spring Data Redis template used for low-level string key/value operations.
     * Injected by the Spring container; must not be {@code null}.
     */
    private final StringRedisTemplate redisTemplate;

    /**
     * Constructs a new {@code TokenBlacklistServiceImpl} with the provided Redis template.
     *
     * @param redisTemplate the {@link StringRedisTemplate} to use for Redis operations;
     *                      must not be {@code null}
     */
    public TokenBlacklistServiceImpl(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stores a key of the form {@code jwt:blacklist:{jti}} in Redis with the given TTL.
     * The value is set to the literal string {@code "blacklisted"} — only the key's
     * existence is meaningful; the value itself is never read.</p>
     *
     * @param jti        the JWT ID ({@code jti} claim) to blacklist; must not be {@code null}
     * @param ttlSeconds the duration in seconds after which the Redis key will be automatically
     *                   deleted; should match the token's remaining lifetime
     */
    @Override
    public void blacklist(String jti, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                BLACKLIST_KEY_PREFIX + jti,
                "blacklisted",
                Duration.ofSeconds(ttlSeconds)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code true} if the Redis key {@code jwt:blacklist:{jti}} exists.
     * The {@link Boolean#TRUE} equality check is used to safely handle the potentially
     * {@code null} return value of {@link org.springframework.data.redis.core.RedisTemplate#hasKey}.</p>
     *
     * @param jti the JWT ID ({@code jti} claim) to check; must not be {@code null}
     * @return {@code true} if the corresponding Redis key exists; {@code false} otherwise
     */
    @Override
    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_KEY_PREFIX + jti));
    }
}
