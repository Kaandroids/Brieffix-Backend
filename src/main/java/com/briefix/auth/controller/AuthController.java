package com.briefix.auth.controller;

import com.briefix.auth.dto.AuthResponse;
import com.briefix.auth.dto.GoogleAuthRequest;
import com.briefix.auth.dto.LoginRequest;
import com.briefix.auth.dto.RefreshRequest;
import com.briefix.auth.dto.RegisterRequest;
import com.briefix.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing the public authentication endpoints for the Briefix API.
 *
 * <p><b>Base path:</b> {@code /api/v1/auth}</p>
 *
 * <p>All endpoints under this path are publicly accessible (no JWT required), as
 * configured in {@link com.briefix.security.SecurityConfig}. This controller is
 * intentionally thin — it performs only HTTP-layer concerns (request deserialization,
 * response status mapping) and delegates all business logic to {@link AuthService}.</p>
 *
 * <p>Bean Validation is enforced at the parameter level via {@code @Valid}. Constraint
 * violations produce an HTTP 400 response via
 * {@link com.briefix.common.GlobalExceptionHandler}.</p>
 *
 * <p><b>Thread safety:</b> This class is a stateless Spring singleton. All request
 * handling is performed through method-local variables; concurrent invocations are safe.</p>
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    /**
     * The authentication service that implements the business logic for registration,
     * login, logout, and token refresh. Injected by the Spring container; must not
     * be {@code null}.
     */
    private final AuthService authService;

    /**
     * Constructs a new {@code AuthController} with the required authentication service.
     *
     * @param authService the service handling registration, login, logout, and token
     *                    refresh operations; must not be {@code null}
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account and returns a JWT token pair on success.
     *
     * <p><b>Endpoint:</b> {@code POST /api/v1/auth/register}</p>
     *
     * <p>The request body is validated before reaching the service layer. If the email
     * is already registered, the service throws
     * {@link com.briefix.auth.exception.EmailAlreadyRegisteredException}, which is
     * mapped to HTTP 409 Conflict by the global exception handler.</p>
     *
     * @param request the validated registration payload containing email, plain-text
     *                password, full name, and optional phone number; must not be {@code null}
     * @return HTTP 201 Created with an {@link AuthResponse} body containing the access
     *         token, refresh token, and token type on success
     */
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@RequestBody Map<String, String> body) {
        authService.resendVerification(body.get("email"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(authService.loginWithGoogle(request));
    }

    /**
     * Authenticates an existing user and returns a JWT token pair on success.
     *
     * <p><b>Endpoint:</b> {@code POST /api/v1/auth/login}</p>
     *
     * <p>If the credentials are invalid, Spring Security throws a
     * {@link org.springframework.security.authentication.BadCredentialsException}, which
     * is mapped to HTTP 401 Unauthorized by the global exception handler.</p>
     *
     * @param request the validated login payload containing the user's email address
     *                and plain-text password; must not be {@code null}
     * @return HTTP 200 OK with an {@link AuthResponse} body containing the access token,
     *         refresh token, and token type on success
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Invalidates the caller's current access token, effectively logging the user out.
     *
     * <p><b>Endpoint:</b> {@code POST /api/v1/auth/logout}</p>
     *
     * <p>The caller must supply a valid {@code Authorization: Bearer <token>} header.
     * The token's {@code jti} (JWT ID) is added to the Redis blacklist with a TTL equal
     * to the token's remaining lifetime. Subsequent requests using the same token will
     * be rejected by {@link com.briefix.security.JwtAuthFilter} even if the token has
     * not yet expired cryptographically.</p>
     *
     * @param authorizationHeader the full value of the {@code Authorization} header,
     *                            expected to be in the form {@code "Bearer <token>"};
     *                            must not be {@code null}
     * @return HTTP 204 No Content on success; no response body is returned
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
        authService.logout(authorizationHeader);
        return ResponseEntity.noContent().build();
    }

    /**
     * Issues a new access token in exchange for a valid, non-expired refresh token.
     *
     * <p><b>Endpoint:</b> {@code POST /api/v1/auth/refresh}</p>
     *
     * <p>The refresh token is validated for signature integrity and expiry. If valid,
     * a new short-lived access token is issued while the refresh token is returned
     * unchanged. If the token is invalid or expired, an
     * {@link io.jsonwebtoken.JwtException} is thrown and mapped to HTTP 401 by the
     * global exception handler.</p>
     *
     * @param request the validated refresh request payload containing the refresh token;
     *                must not be {@code null}
     * @return HTTP 200 OK with a new {@link AuthResponse} body containing the fresh
     *         access token, the original refresh token, and the token type
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }
}
