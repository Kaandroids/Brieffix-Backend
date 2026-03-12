package com.briefix.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration for the Briefix application.
 *
 * <p>Configures a stateless, JWT-based security model with the following characteristics:
 * <ul>
 *   <li>CSRF protection is disabled — not applicable for stateless REST APIs that do not
 *       rely on browser-managed cookies for authentication.</li>
 *   <li>Session management is set to {@link SessionCreationPolicy#STATELESS} — the server
 *       creates no {@code HttpSession} and never uses one to store security context state.</li>
 *   <li>The {@link JwtAuthFilter} is inserted immediately before the default
 *       {@link UsernamePasswordAuthenticationFilter} so that Bearer-token requests are
 *       authenticated before any form-login processing occurs.</li>
 *   <li>Authentication endpoints under {@code /api/v1/auth/**} are publicly accessible
 *       without a JWT; all other endpoints require a valid, non-blacklisted token.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread safety:</b> This class is a Spring {@code @Configuration} and is
 * instantiated once by the application context. All bean methods are idempotent
 * and safe to call from a single initialization thread.</p>
 */
@Configuration
@EnableWebSecurity
@org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
public class SecurityConfig {

    /**
     * The JWT authentication filter that validates Bearer tokens on each request.
     * Injected by the Spring container; must not be {@code null}.
     */
    private final JwtAuthFilter jwtAuthFilter;

    /**
     * The Spring Security user-details service used by the DAO authentication provider
     * to load user credentials during the login flow.
     * Injected by the Spring container; must not be {@code null}.
     */
    private final UserDetailsService userDetailsService;

    private final RateLimitFilter rateLimitFilter;

    /**
     * Constructs a new {@code SecurityConfig} with the required security collaborators.
     *
     * @param jwtAuthFilter      the stateless JWT filter that authenticates requests via
     *                           Bearer tokens; must not be {@code null}
     * @param userDetailsService the service that loads {@link org.springframework.security.core.userdetails.UserDetails}
     *                           by email for credential verification; must not be {@code null}
     * @param rateLimitFilter    the filter that enforces per-IP rate limits on auth endpoints
     */
    public SecurityConfig(JwtAuthFilter jwtAuthFilter, UserDetailsService userDetailsService,
                          RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.userDetailsService = userDetailsService;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * Defines the main HTTP security filter chain for the application.
     *
     * <p>The chain is configured to:
     * <ul>
     *   <li>Disable CSRF (stateless API — no session cookies).</li>
     *   <li>Permit all requests to {@code /api/v1/auth/**} without authentication.</li>
     *   <li>Require authentication for every other request.</li>
     *   <li>Enforce {@code STATELESS} session creation policy.</li>
     *   <li>Register the {@link DaoAuthenticationProvider} as the authentication provider.</li>
     *   <li>Place {@link JwtAuthFilter} before the default username/password filter.</li>
     * </ul>
     * </p>
     *
     * @param http the {@link HttpSecurity} builder provided by the Spring Security
     *             infrastructure; must not be {@code null}
     * @return the fully configured and built {@link SecurityFilterChain}
     * @throws Exception if an error occurs while building the filter chain (propagated
     *                   from the underlying Spring Security DSL)
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/public/**").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Configures and exposes the DAO authentication provider used during the login flow.
     *
     * <p>The provider is wired with the application's {@link UserDetailsService} (which
     * loads users by email from PostgreSQL) and the BCrypt {@link PasswordEncoder}
     * (which compares the supplied plain-text password against the stored hash).</p>
     *
     * @return a fully configured {@link AuthenticationProvider} backed by the JPA user store
     *         and BCrypt password encoding
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the application-level {@link AuthenticationManager} as a Spring bean.
     *
     * <p>This manager is injected into {@link com.briefix.auth.service.AuthServiceImpl}
     * and used to programmatically authenticate credentials during the login flow by
     * delegating to the configured {@link AuthenticationProvider}.</p>
     *
     * @param config the {@link AuthenticationConfiguration} from which the manager is
     *               retrieved; provided automatically by Spring Security
     * @return the application-level {@link AuthenticationManager}
     * @throws Exception if the manager cannot be retrieved from the configuration
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provides a BCrypt password encoder with the default cost factor (10 rounds).
     *
     * <p>This bean is used in two places:
     * <ul>
     *   <li>By {@link com.briefix.auth.service.AuthServiceImpl} to hash plain-text
     *       passwords before persisting new user accounts.</li>
     *   <li>By the {@link DaoAuthenticationProvider} to verify passwords at login time.</li>
     * </ul>
     * </p>
     *
     * @return a {@link BCryptPasswordEncoder} with strength 10
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
