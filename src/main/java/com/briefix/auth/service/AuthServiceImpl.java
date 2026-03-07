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

@Service
public class AuthServiceImpl implements AuthService {

    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmailService emailService;

    @Value("${app.google.client-id}")
    private String googleClientId;

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

    @Override
    public void logout(String bearerToken) {
        String token = bearerToken.startsWith(BEARER_PREFIX)
                ? bearerToken.substring(BEARER_PREFIX.length())
                : bearerToken;

        String jti = jwtService.extractJti(token);
        long remainingSeconds = jwtService.extractRemainingSeconds(token);
        tokenBlacklistService.blacklist(jti, remainingSeconds);
    }

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
