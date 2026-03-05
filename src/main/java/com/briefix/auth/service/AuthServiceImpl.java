package com.briefix.auth.service;

import com.briefix.auth.dto.AuthResponse;
import com.briefix.auth.dto.LoginRequest;
import com.briefix.auth.dto.RegisterRequest;
import com.briefix.auth.exception.EmailAlreadyRegisteredException;
import com.briefix.security.JwtService;
import com.briefix.security.TokenBlacklistService;
import com.briefix.user.model.AuthProvider;
import com.briefix.user.model.User;
import com.briefix.user.model.UserPlan;
import com.briefix.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link AuthService}.
 *
 * <p>Orchestrates the full authentication lifecycle for local (email + password) accounts:
 * <ul>
 *   <li><b>Registration</b> – validates email uniqueness, hashes the password with BCrypt,
 *       persists a new {@link User} entity, and issues a JWT token pair.</li>
 *   <li><b>Login</b> – delegates credential verification to Spring Security's
 *       {@link AuthenticationManager} (which invokes BCrypt comparison), then issues
 *       a fresh JWT token pair on success.</li>
 *   <li><b>Logout</b> – extracts the token's {@code jti} and stores it in the Redis
 *       blacklist via {@link TokenBlacklistService} with a TTL matching the token's
 *       remaining lifetime.</li>
 *   <li><b>Token refresh</b> – validates the supplied refresh token and issues a new
 *       access token; the refresh token itself is returned unchanged.</li>
 * </ul>
 * </p>
 *
 * <p><b>Thread safety:</b> This class is a stateless Spring singleton. All mutable
 * state is held by the injected collaborators, each of which is independently
 * thread-safe. Concurrent invocations of all public methods are therefore safe.</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * The {@code "Bearer "} prefix (including trailing space) used to strip the scheme
     * from a raw {@code Authorization} header value when extracting the token string
     * during logout processing.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Repository used to persist new {@link User} entities and check for existing
     * email addresses during registration. Injected by the Spring container.
     */
    private final UserRepository userRepository;

    /**
     * BCrypt password encoder used to hash plain-text passwords before persistence
     * and to verify passwords during login (via the {@link AuthenticationManager}).
     * Injected by the Spring container.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT utility service used to generate access and refresh tokens, extract claims,
     * validate token signatures, and compute remaining token lifetimes.
     * Injected by the Spring container.
     */
    private final JwtService jwtService;

    /**
     * Spring Security authentication manager that verifies email/password credentials
     * by delegating to the configured {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}.
     * Injected by the Spring container.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Spring Security user-details service used to load the full {@link UserDetails}
     * principal after successful authentication or token validation.
     * Injected by the Spring container.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Redis-backed blacklist service used to invalidate specific JWT tokens on logout
     * without requiring all user tokens to be revoked.
     * Injected by the Spring container.
     */
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Constructs a new {@code AuthServiceImpl} with all required collaborators.
     *
     * @param userRepository         the JPA repository for user persistence and email
     *                               uniqueness checks; must not be {@code null}
     * @param passwordEncoder        the BCrypt encoder for hashing and verifying passwords;
     *                               must not be {@code null}
     * @param jwtService             the JWT utility for token generation and claim extraction;
     *                               must not be {@code null}
     * @param authenticationManager  the Spring Security manager for credential verification;
     *                               must not be {@code null}
     * @param userDetailsService     the service for loading {@link UserDetails} by email;
     *                               must not be {@code null}
     * @param tokenBlacklistService  the Redis-backed service for token invalidation on logout;
     *                               must not be {@code null}
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           UserDetailsService userDetailsService,
                           TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Creates a new {@link User} with {@link AuthProvider#LOCAL}, a BCrypt-hashed
     * password, and {@link UserPlan#STANDARD} as the default plan, then persists it.
     * After successful persistence, loads the user's {@link UserDetails} and generates
     * a token pair.</p>
     *
     * @throws EmailAlreadyRegisteredException if {@code request.email()} is already
     *         associated with an existing account in the database
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        var user = new User(
                null,
                request.email(),
                passwordEncoder.encode(request.password()),
                AuthProvider.LOCAL,
                null,
                false,
                request.fullName(),
                request.phone(),
                UserPlan.STANDARD,
                null
        );

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        return AuthResponse.of(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates credential verification to Spring Security's {@link AuthenticationManager},
     * which internally loads the user via {@link UserDetailsService} and uses the configured
     * BCrypt {@link PasswordEncoder} to compare the supplied password against the stored hash.
     * On success, a fresh token pair is generated for the authenticated user.</p>
     *
     * @throws org.springframework.security.authentication.BadCredentialsException if the
     *         email does not exist or the password does not match the BCrypt hash stored
     *         in the database
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        return AuthResponse.of(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Strips the optional {@code "Bearer "} prefix from {@code bearerToken}, then
     * extracts the {@code jti} claim and the remaining lifetime from the raw token.
     * The {@code jti} is stored in Redis via {@link TokenBlacklistService#blacklist(String, long)}
     * with a TTL equal to the remaining lifetime in seconds, ensuring the blacklist entry
     * is automatically evicted once the token would have expired anyway.</p>
     */
    @Override
    public void logout(String bearerToken) {
        String token = bearerToken.startsWith(BEARER_PREFIX)
                ? bearerToken.substring(BEARER_PREFIX.length())
                : bearerToken;

        String jti = jwtService.extractJti(token);
        long remainingSeconds = jwtService.extractRemainingSeconds(token);
        tokenBlacklistService.blacklist(jti, remainingSeconds);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Extracts the subject (email) from the refresh token, loads the corresponding
     * {@link UserDetails}, and verifies that the token's signature is valid and it has
     * not expired. If the token passes validation, a new access token is generated for
     * the same user. The refresh token itself is returned unchanged (no rotation).</p>
     *
     * @throws io.jsonwebtoken.JwtException if the refresh token's signature is invalid,
     *         the token is malformed, or the token has passed its expiration timestamp
     */
    @Override
    public AuthResponse refresh(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            throw new io.jsonwebtoken.JwtException("Refresh token is invalid or expired");
        }

        return AuthResponse.of(
                jwtService.generateAccessToken(userDetails),
                refreshToken
        );
    }
}
