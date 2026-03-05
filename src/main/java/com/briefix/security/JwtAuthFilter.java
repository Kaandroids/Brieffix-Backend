package com.briefix.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts every incoming HTTP request and validates the JWT
 * present in the {@code Authorization: Bearer <token>} header.
 *
 * <p>This filter extends {@link OncePerRequestFilter} to guarantee exactly one
 * execution per request, even when requests are forwarded internally within the
 * servlet container.</p>
 *
 * <p>Processing pipeline for each request:
 * <ol>
 *   <li>Extract the Bearer token from the {@code Authorization} header; pass through
 *       immediately if the header is absent or does not start with {@code "Bearer "}.</li>
 *   <li>Parse the {@code jti} claim from the token and check the Redis blacklist —
 *       pass through without authenticating if the token has been blacklisted
 *       (e.g., due to a prior logout).</li>
 *   <li>Extract the username (email) from the token's {@code sub} claim and load
 *       the corresponding {@link UserDetails} from the database.</li>
 *   <li>Validate the token's signature and expiry against the loaded user.</li>
 *   <li>On success, populate the {@link SecurityContextHolder} with a fully
 *       authenticated {@link UsernamePasswordAuthenticationToken}.</li>
 * </ol>
 * </p>
 *
 * <p>Any token that is malformed, expired, or blacklisted is silently ignored (the
 * exception is swallowed), leaving the {@link SecurityContextHolder} unauthenticated.
 * Spring Security's downstream access-control rules are then responsible for
 * returning a 401 or 403 response as appropriate.</p>
 *
 * <p><b>Thread safety:</b> This component is a stateless Spring singleton. Each
 * invocation of {@link #doFilterInternal} operates exclusively on method-local
 * variables and is therefore safe for concurrent use.</p>
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    /**
     * The prefix expected at the start of a well-formed {@code Authorization} header value.
     * Tokens are extracted by stripping this prefix from the header string.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Service used to parse, sign, and validate JWT tokens.
     * Injected by the Spring container; must not be {@code null}.
     */
    private final JwtService jwtService;

    /**
     * Service used to load the full {@link UserDetails} for a given email address
     * after the token's subject claim has been extracted.
     * Injected by the Spring container; must not be {@code null}.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Service used to check whether a token's {@code jti} has been placed on the
     * blacklist (e.g., because the user explicitly logged out).
     * Injected by the Spring container; must not be {@code null}.
     */
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Constructs a new {@code JwtAuthFilter} with all required collaborators.
     *
     * @param jwtService             the JWT utility service for token parsing and validation;
     *                               must not be {@code null}
     * @param userDetailsService     the Spring Security user-details loader used to
     *                               reconstruct the principal from the token subject;
     *                               must not be {@code null}
     * @param tokenBlacklistService  the Redis-backed blacklist service used to detect
     *                               explicitly invalidated tokens; must not be {@code null}
     */
    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsService userDetailsService,
                         TokenBlacklistService tokenBlacklistService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * Core filter logic executed once per HTTP request.
     *
     * <p>Attempts to authenticate the request using the JWT in the
     * {@code Authorization: Bearer} header. If authentication succeeds, the
     * {@link SecurityContextHolder} is populated so that downstream components
     * (e.g., {@code @PreAuthorize}, method security) can inspect the principal.</p>
     *
     * <p>The filter always forwards the request to the next element in the chain via
     * {@code filterChain.doFilter(request, response)}, regardless of whether
     * authentication succeeded or failed. It is the responsibility of Spring
     * Security's access-decision infrastructure to reject unauthenticated requests
     * to protected resources.</p>
     *
     * @param request     the current HTTP servlet request; must not be {@code null}
     * @param response    the current HTTP servlet response; must not be {@code null}
     * @param filterChain the remaining filter chain to invoke after this filter;
     *                    must not be {@code null}
     * @throws ServletException if the filter chain throws a servlet-level exception
     * @throws IOException      if an I/O error occurs during request or response processing
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        try {
            String jti = jwtService.extractJti(token);

            if (tokenBlacklistService.isBlacklisted(jti)) {
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtService.extractUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception ignored) {
            // Malformed or tampered token — let the security context remain empty
        }

        filterChain.doFilter(request, response);
    }
}
