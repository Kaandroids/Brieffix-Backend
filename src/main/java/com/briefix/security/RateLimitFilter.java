package com.briefix.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that enforces per-IP, per-endpoint rate limiting on the public
 * authentication endpoints using a Redis-backed fixed-window counter.
 *
 * <h2>Algorithm</h2>
 * <p>For each incoming request to a rate-limited path, the filter:
 * <ol>
 *   <li>Derives a Redis key of the form {@code rate_limit:<path>:<clientIp>}.</li>
 *   <li>Atomically increments the key's counter via {@code INCR}.</li>
 *   <li>On the first increment (counter == 1), sets a TTL equal to the configured
 *       window so that the counter expires automatically at the end of the window.</li>
 *   <li>If the counter exceeds the configured maximum, short-circuits the filter chain
 *       and writes an RFC 7807 {@link ProblemDetail} response with HTTP 429.</li>
 *   <li>Otherwise, continues down the filter chain normally.</li>
 * </ol>
 * </p>
 *
 * <h2>Scope</h2>
 * <p>Rate limiting is applied exclusively to:
 * <ul>
 *   <li>{@code POST /api/v1/auth/login} — default: 10 requests / 60 s per IP</li>
 *   <li>{@code POST /api/v1/auth/register} — default: 5 requests / 60 s per IP</li>
 * </ul>
 * All other requests pass through without inspection.
 * </p>
 *
 * <h2>Client IP Resolution</h2>
 * <p>The client IP is read from the {@code X-Forwarded-For} header when present
 * (taking the first hop), falling back to {@link HttpServletRequest#getRemoteAddr()}.
 * This ensures correct behaviour behind reverse proxies and load balancers.</p>
 *
 * <h2>Configuration</h2>
 * <p>Limits are externalised as application properties and can be overridden per
 * environment via environment variables or {@code application-*.yaml} profiles:
 * <pre>
 * app.rate-limit.login.max-requests      (default: 10)
 * app.rate-limit.login.window-seconds    (default: 60)
 * app.rate-limit.register.max-requests   (default: 5)
 * app.rate-limit.register.window-seconds (default: 60)
 * </pre>
 * </p>
 *
 * <h2>Error Response</h2>
 * <p>Rejected requests receive an RFC 7807-compliant JSON body to match the format
 * used by {@link com.briefix.common.GlobalExceptionHandler} across the rest of the API:
 * <pre>{@code
 * {
 *   "status": 429,
 *   "title": "Too Many Requests",
 *   "detail": "Too many requests. Please try again in 60 seconds."
 * }
 * }</pre>
 * </p>
 *
 * <h2>Thread Safety</h2>
 * <p>This class is a stateless Spring singleton. Redis INCR is atomic, so concurrent
 * requests from the same IP are correctly counted without race conditions. All
 * mutable state is stored exclusively in Redis.</p>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH          = "/api/v1/auth/login";
    private static final String REGISTER_PATH       = "/api/v1/auth/register";
    private static final String PUBLIC_PREVIEW_PATH = "/api/v1/public/letter-preview";

    private final StringRedisTemplate redis;
    private final ObjectMapper        objectMapper;

    /** Maximum number of login requests allowed per IP within the window. */
    @Value("${app.rate-limit.login.max-requests:10}")
    private int loginMaxRequests;

    /** Duration of the fixed window in seconds for the login endpoint. */
    @Value("${app.rate-limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    /** Maximum number of registration requests allowed per IP within the window. */
    @Value("${app.rate-limit.register.max-requests:5}")
    private int registerMaxRequests;

    /** Duration of the fixed window in seconds for the register endpoint. */
    @Value("${app.rate-limit.register.window-seconds:60}")
    private int registerWindowSeconds;

    /** Maximum number of public letter-preview requests allowed per IP per day. */
    @Value("${app.rate-limit.public-preview.max-requests:3}")
    private int publicPreviewMaxRequests;

    /** Duration of the fixed window in seconds for the public preview endpoint (1 day). */
    @Value("${app.rate-limit.public-preview.window-seconds:86400}")
    private int publicPreviewWindowSeconds;

    /**
     * Constructs a new {@code RateLimitFilter} with the required Redis template and
     * JSON serialiser.
     *
     * @param redis        the {@link StringRedisTemplate} used for atomic counter operations;
     *                     must not be {@code null}
     * @param objectMapper the Jackson {@link ObjectMapper} used to serialise the 429
     *                     {@link ProblemDetail} response body; must not be {@code null}
     */
    public RateLimitFilter(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis        = redis;
        this.objectMapper = objectMapper;
    }

    /**
     * Intercepts requests to rate-limited paths, increments the per-IP counter in Redis,
     * and either allows the request to proceed or rejects it with HTTP 429.
     *
     * @param request     the incoming HTTP request; never {@code null}
     * @param response    the outgoing HTTP response; never {@code null}
     * @param filterChain the remainder of the filter chain to invoke on success;
     *                    never {@code null}
     * @throws ServletException if a servlet error occurs while processing the request
     * @throws IOException      if an I/O error occurs while writing the 429 response
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        int maxRequests;
        int windowSeconds;

        if (LOGIN_PATH.equals(path)) {
            maxRequests   = loginMaxRequests;
            windowSeconds = loginWindowSeconds;
        } else if (REGISTER_PATH.equals(path)) {
            maxRequests   = registerMaxRequests;
            windowSeconds = registerWindowSeconds;
        } else if (PUBLIC_PREVIEW_PATH.equals(path)) {
            maxRequests   = publicPreviewMaxRequests;
            windowSeconds = publicPreviewWindowSeconds;
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        String ip  = resolveClientIp(request);
        String key = "rate_limit:" + path + ":" + ip;

        Long count = redis.opsForValue().increment(key);
        if (count == null) count = 1L;
        if (count == 1) {
            // Set the TTL only on the first increment so the window expires naturally.
            redis.expire(key, windowSeconds, TimeUnit.SECONDS);
        }

        if (count > maxRequests) {
            rejectWithTooManyRequests(response, windowSeconds);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolves the originating client IP address, honouring the {@code X-Forwarded-For}
     * header when the application is deployed behind a reverse proxy or load balancer.
     *
     * <p>When {@code X-Forwarded-For} is present, the first (leftmost) address in the
     * comma-separated list is used, as it represents the original client. Falls back to
     * {@link HttpServletRequest#getRemoteAddr()} for direct connections.</p>
     *
     * @param request the current HTTP request; must not be {@code null}
     * @return the resolved client IP string; never {@code null}
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes an RFC 7807 {@link ProblemDetail} response with HTTP status 429 and a
     * human-readable retry hint, then terminates the filter chain for this request.
     *
     * @param response      the HTTP response to write to; must not be {@code null}
     * @param windowSeconds the window duration, included in the detail message so
     *                      clients know how long to wait before retrying
     * @throws IOException if an I/O error occurs while writing the response body
     */
    private void rejectWithTooManyRequests(HttpServletResponse response, int windowSeconds)
            throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too many requests. Please try again in " + windowSeconds + " seconds."
        );
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), problem);
    }
}