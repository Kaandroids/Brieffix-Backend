package com.briefix.auth.service;

import com.briefix.auth.dto.AuthResponse;
import com.briefix.auth.dto.GoogleAuthRequest;
import com.briefix.auth.dto.LoginRequest;
import com.briefix.auth.dto.RegisterRequest;
import com.briefix.auth.exception.EmailAlreadyRegisteredException;
import com.briefix.auth.exception.EmailNotVerifiedException;
import com.briefix.auth.exception.GoogleAuthException;
import com.briefix.auth.exception.InvalidVerificationTokenException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;

/**
 * Default implementation of {@link AuthService} providing registration, login, logout,
 * token refresh, email verification, and Google OAuth2 authentication flows.
 *
 * <p>This service acts as the central orchestrator for all authentication-related
 * operations in the Briefix backend.  It delegates to the following collaborators:</p>
 * <ul>
 *   <li>{@link UserRepository} — for user persistence and lookup</li>
 *   <li>{@link PasswordEncoder} — for bcrypt hashing and verification</li>
 *   <li>{@link JwtService} — for access and refresh token generation and validation</li>
 *   <li>{@link AuthenticationManager} — for delegating credential verification to Spring Security</li>
 *   <li>{@link UserDetailsService} — for loading {@link org.springframework.security.core.userdetails.UserDetails} after successful authentication</li>
 *   <li>{@link TokenBlacklistService} — for JWT blacklisting on logout</li>
 *   <li>{@link EmailService} — for sending email verification messages</li>
 * </ul>
 *
 * <p>Google ID tokens are verified using the official Google API Client library
 * ({@code GoogleIdTokenVerifier}) against the configured {@code app.google.client-id}
 * audience to prevent token substitution attacks.</p>
 *
 * <p>Thread-safety: This class is a stateless Spring singleton.  The only mutable
 * field is {@code googleClientId}, which is set once during bean initialisation by
 * Spring's {@code @Value} injection and is never modified afterwards.  The class is
 * therefore safe for concurrent use across multiple request threads.</p>
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * The {@code "Bearer "} prefix (including trailing space) prepended to JWT access
     * tokens in the {@code Authorization} HTTP header.  Used to strip the prefix before
     * processing the raw token string.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Repository for persisting and querying {@link com.briefix.user.model.User} aggregates.
     */
    private final UserRepository userRepository;

    /**
     * BCrypt password encoder used to hash passwords at registration time and to
     * verify credentials at login time.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Service responsible for JWT generation, parsing, and validation.
     */
    private final JwtService jwtService;

    /**
     * Spring Security authentication manager that verifies username/password credentials
     * against the configured {@link UserDetailsService} and {@link PasswordEncoder}.
     */
    private final AuthenticationManager authenticationManager;

    /**
     * Spring Security user-details service used to load a fully populated
     * {@link org.springframework.security.core.userdetails.UserDetails} object after
     * successful credential verification.
     */
    private final UserDetailsService userDetailsService;

    /**
     * Service that maintains a Redis-backed blacklist of invalidated JWT JTI values,
     * used to enforce logout on the server side.
     */
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * Service responsible for dispatching email verification messages via SMTP.
     */
    private final EmailService emailService;

    /**
     * Google OAuth2 client ID against which incoming Google ID tokens are verified.
     * Injected from the {@code app.google.client-id} application property.
     */
    @Value("${app.google.client-id}")
    private String googleClientId;

    /**
     * Constructs an {@code AuthServiceImpl} with all required dependencies.
     *
     * @param userRepository          the user aggregate repository; must not be {@code null}
     * @param passwordEncoder         the BCrypt password encoder; must not be {@code null}
     * @param jwtService              the JWT generation and validation service; must not be {@code null}
     * @param authenticationManager   the Spring Security authentication manager; must not be {@code null}
     * @param userDetailsService      the user-details service for post-authentication loading; must not be {@code null}
     * @param tokenBlacklistService   the Redis-backed JWT blacklist service; must not be {@code null}
     * @param emailService            the email dispatch service; must not be {@code null}
     */
    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           UserDetailsService userDetailsService,
                           TokenBlacklistService tokenBlacklistService,
                           EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.emailService = emailService;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Checks for duplicate email registration, hashes the password with BCrypt,
     * creates a new {@link com.briefix.user.model.User} with {@code emailVerified = false}
     * and a 24-hour verification token, persists it, and dispatches a verification email.
     * If the email dispatch fails the exception is swallowed and logged to
     * {@code System.err} — registration is not rolled back so the user can request a
     * resend from the check-email page.</p>
     *
     * @param request the validated registration request; must not be {@code null}
     * @throws EmailAlreadyRegisteredException if an account with the same email already exists
     */
    @Override
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusHours(24);

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
                null,
                token,
                expiry
        );

        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(request.email(), request.fullName(), token);
        } catch (Exception e) {
            // Log but don't fail registration — user can resend from the check-email page
            System.err.println("Failed to send verification email to " + request.email() + ": " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Caused by: " + e.getCause().getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates credential verification to {@link AuthenticationManager}, which throws
     * an {@link org.springframework.security.core.AuthenticationException} on failure.
     * After successful authentication, verifies that the user's email has been confirmed
     * before issuing tokens.  A new access token and refresh token pair is returned
     * on success.</p>
     *
     * @param request the login request containing email and password; must not be {@code null}
     * @return an {@link AuthResponse} containing a fresh access token and refresh token
     * @throws org.springframework.security.core.AuthenticationException if the credentials are invalid
     * @throws EmailNotVerifiedException if the account's email address has not been verified
     */
    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("User not found"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException(request.email());
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        return AuthResponse.of(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
        );
    }

    /**
     * {@inheritDoc}
     *
     * <p>Strips the {@code "Bearer "} prefix if present, extracts the JWT's unique
     * identifier ({@code jti} claim) and its remaining validity period, and records
     * the JTI in the Redis-backed blacklist for the remainder of that period.
     * Subsequent requests presenting this token will be rejected by
     * {@link com.briefix.security.JwtAuthFilter}.</p>
     *
     * @param bearerToken the raw {@code Authorization} header value (with or without
     *                    the {@code "Bearer "} prefix); must not be {@code null}
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
     * <p>Extracts the username from the provided refresh token, loads the corresponding
     * {@link org.springframework.security.core.userdetails.UserDetails}, validates the
     * token's signature and expiry, and issues a fresh access token. The original
     * refresh token is returned unchanged in the response — refresh token rotation is
     * not performed.</p>
     *
     * @param refreshToken the refresh JWT previously issued during login or token refresh
     * @return an {@link AuthResponse} with a new access token and the original refresh token
     * @throws io.jsonwebtoken.JwtException if the refresh token is invalid, expired, or
     *                                      does not match the user's details
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

    /**
     * {@inheritDoc}
     *
     * <p>Looks up the user by the verification token, checks that the token has not
     * expired, and persists an updated {@link com.briefix.user.model.User} with
     * {@code emailVerified = true} and the verification token fields cleared
     * ({@code null}). If the token does not match any user, or if it has expired,
     * an {@link InvalidVerificationTokenException} is thrown.</p>
     *
     * @param token the email-verification token received by the user; must not be {@code null}
     * @throws InvalidVerificationTokenException if the token is unknown or has expired
     */
    @Override
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(InvalidVerificationTokenException::new);

        if (user.verificationTokenExpiry() == null || LocalDateTime.now().isAfter(user.verificationTokenExpiry())) {
            throw new InvalidVerificationTokenException();
        }

        var verified = new User(
                user.id(),
                user.email(),
                user.passwordHash(),
                user.provider(),
                user.providerId(),
                true,
                user.fullName(),
                user.phone(),
                user.plan(),
                user.createdAt(),
                null,
                null
        );

        userRepository.save(verified);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verifies the Google ID token via {@link #verifyGoogleToken(String)}, extracts
     * the {@code email}, {@code sub}, and {@code name} claims, and then either:</p>
     * <ul>
     *   <li><strong>Existing account (Google):</strong> issues JWT tokens for the existing user.</li>
     *   <li><strong>Existing account (non-Google):</strong> throws {@link GoogleAuthException}
     *       to prevent account hijacking via OAuth2.</li>
     *   <li><strong>New account:</strong> creates a user with {@code emailVerified = true},
     *       {@link com.briefix.user.model.AuthProvider#GOOGLE} provider, and issues JWT tokens.</li>
     * </ul>
     *
     * @param request the Google authentication request containing the ID token; must not be {@code null}
     * @return an {@link AuthResponse} with access and refresh tokens
     * @throws GoogleAuthException if the ID token is invalid, or if the email is already
     *                             registered with a non-Google provider
     */
    @Override
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.idToken());

        String email      = payload.getEmail();
        String providerId = payload.getSubject();
        String fullName   = (String) payload.get("name");

        return userRepository.findByEmail(email).map(existing -> {
            if (existing.provider() != AuthProvider.GOOGLE) {
                throw new GoogleAuthException(
                    "This email is already registered with a password. Please use email/password login."
                );
            }
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            return AuthResponse.of(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
            );
        }).orElseGet(() -> {
            var newUser = new User(
                null, email, null, AuthProvider.GOOGLE, providerId,
                true, fullName != null ? fullName : email, null,
                UserPlan.STANDARD, null, null, null
            );
            userRepository.save(newUser);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            return AuthResponse.of(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails)
            );
        });
    }

    /**
     * Verifies a Google ID token using the Google API Client library and returns
     * the token's payload on success.
     *
     * <p>A new {@link GoogleIdTokenVerifier} is constructed for each call with the
     * configured {@code app.google.client-id} as the expected audience. The verifier
     * performs cryptographic signature verification against Google's public keys and
     * checks the {@code aud}, {@code iss}, and {@code exp} claims.</p>
     *
     * @param idToken the raw Google ID token string from the client; must not be {@code null}
     * @return the verified token {@link GoogleIdToken.Payload} containing user claims
     * @throws GoogleAuthException if the token is {@code null} (invalid or unverifiable),
     *                             or if any other exception occurs during verification
     */
    private GoogleIdToken.Payload verifyGoogleToken(String idToken) {
        try {
            var verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new GoogleAuthException("Invalid Google ID token.");
            }
            return token.getPayload();
        } catch (GoogleAuthException e) {
            throw e;
        } catch (Exception e) {
            throw new GoogleAuthException("Failed to verify Google token: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>If a user account exists for the given email and the email has not yet been
     * verified, generates a new UUID verification token with a fresh 24-hour expiry,
     * persists the updated user, and dispatches a new verification email. If the
     * account does not exist or is already verified, the method exits silently (no
     * error is thrown) to prevent user-enumeration attacks. Email dispatch failures
     * are swallowed and logged to {@code System.err}.</p>
     *
     * @param email the email address to resend the verification link to;
     *              must not be {@code null}
     */
    @Override
    public void resendVerification(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) return;

            String token = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusHours(24);

            var updated = new User(
                    user.id(),
                    user.email(),
                    user.passwordHash(),
                    user.provider(),
                    user.providerId(),
                    false,
                    user.fullName(),
                    user.phone(),
                    user.plan(),
                    user.createdAt(),
                    token,
                    expiry
            );

            userRepository.save(updated);

            try {
                emailService.sendVerificationEmail(user.email(), user.fullName(), token);
            } catch (Exception e) {
                System.err.println("Failed to resend verification email to " + user.email() + ": " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("Caused by: " + e.getCause().getMessage());
                }
            }
        });
    }
}
