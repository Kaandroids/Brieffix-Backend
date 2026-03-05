package com.briefix.user.controller;

import com.briefix.user.dto.UserDto;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.mapper.UserMapper;
import com.briefix.user.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    /**
     * Mapper used to convert the loaded {@link com.briefix.user.model.User} domain record
     * into a safe {@link UserDto} before serializing it to the HTTP response body.
     */
    private final UserMapper userMapper;

    /**
     * Constructs a {@code UserController} with the required repository and mapper.
     *
     * <p>Constructor injection is used to make all dependencies explicit and to
     * support unit testing with mock implementations.</p>
     *
     * @param userRepository the repository for loading user records; must not be {@code null}
     * @param userMapper     the mapper for converting domain records to DTOs; must not be {@code null}
     */
    public UserController(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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
}
