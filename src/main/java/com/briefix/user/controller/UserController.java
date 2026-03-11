package com.briefix.user.controller;

import com.briefix.user.dto.UpdateBillingRequest;
import com.briefix.user.dto.UpdatePasswordRequest;
import com.briefix.user.dto.UpdateProfileRequest;
import com.briefix.user.dto.UserDto;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.mapper.UserMapper;
import com.briefix.user.repository.UserRepository;
import com.briefix.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing user-related endpoints under the {@code /api/v1/users} base path.
 *
 * <p>This controller handles HTTP requests related to the authenticated user's own account.
 * All endpoints in this controller require an authenticated security context; unauthenticated
 * requests will be rejected by Spring Security before reaching any handler method.</p>
 *
 * <p>The controller depends directly on {@link UserRepository} and {@link UserMapper}
 * for lightweight read operations. For more complex business logic or orchestration,
 * consider delegating to {@link com.briefix.user.service.UserService} instead.</p>
 *
 * <p>All responses return {@link UserDto} instances, which exclude sensitive fields
 * such as password hashes and OAuth2 provider tokens, ensuring API safety.</p>
 *
 * <p>Thread-safety: This class is a stateless Spring singleton and is safe
 * for concurrent use across multiple HTTP request threads.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    /**
     * Repository used to load the authenticated user's account record from the data store.
     * The email extracted from the security context serves as the lookup key.
     */
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserService userService;

    public UserController(UserRepository userRepository, UserMapper userMapper, UserService userService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userService = userService;
    }

    /**
     * Returns the profile information of the currently authenticated user.
     *
     * <p>The authenticated principal's email address (extracted from the
     * {@link Authentication#getName()} value set by Spring Security) is used
     * to locate the user record. This endpoint is equivalent to a "current user"
     * or "whoami" lookup.</p>
     *
     * <p>HTTP Method: {@code GET}</p>
     * <p>Path: {@code /api/v1/users/me}</p>
     * <p>Response: {@code 200 OK} with a {@link UserDto} body on success.</p>
     * <p>Response: {@code 404 Not Found} if the authenticated email has no corresponding account.</p>
     *
     * @param authentication the Spring Security authentication token for the current request;
     *                       injected automatically by the framework and must not be {@code null}
     * @return a {@link ResponseEntity} containing the {@link UserDto} of the authenticated user
     *         with HTTP status {@code 200 OK}
     * @throws UserNotFoundException if no user record is found for the authenticated principal's email
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(Authentication authentication) {
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new UserNotFoundException(authentication.getName()));
        return ResponseEntity.ok(userMapper.toDto(user));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserDto> updateProfile(Authentication authentication,
                                                  @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authentication.getName(),
                request.fullName(), request.phone()));
    }

    @PutMapping("/me/password")
    public ResponseEntity<Void> updatePassword(Authentication authentication,
                                                @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(authentication.getName(),
                request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/billing")
    public ResponseEntity<UserDto> updateBilling(Authentication authentication,
                                                  @RequestBody UpdateBillingRequest request) {
        return ResponseEntity.ok(userService.updateBilling(authentication.getName(), request));
    }
}
