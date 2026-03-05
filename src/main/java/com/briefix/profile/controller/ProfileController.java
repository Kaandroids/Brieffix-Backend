package com.briefix.profile.controller;

import com.briefix.profile.dto.CreateProfileRequest;
import com.briefix.profile.dto.ProfileDto;
import com.briefix.profile.dto.UpdateProfileRequest;
import com.briefix.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing profile management endpoints under the {@code /api/v1/profiles} base path.
 *
 * <p>This controller handles all HTTP requests related to profile CRUD operations for the
 * currently authenticated user. All endpoints require an authenticated security context;
 * unauthenticated requests are rejected by Spring Security before reaching any handler method.</p>
 *
 * <p>The authenticated principal's email address (obtained via {@link Authentication#getName()})
 * is passed to the service layer on every request, where it is used both to resolve the owning
 * user account and to enforce ownership-based access control. Users can only read and modify
 * their own profiles; attempts to access another user's profile result in an HTTP {@code 403 Forbidden}.</p>
 *
 * <p>All request bodies are validated with Jakarta Bean Validation ({@code @Valid}) before
 * reaching the service layer. Validation failures result in an HTTP {@code 400 Bad Request}
 * response handled by the global exception handler.</p>
 *
 * <p>Thread-safety: This class is a stateless Spring singleton and is safe for concurrent
 * use across multiple HTTP request threads.</p>
 */
@RestController
@RequestMapping("/api/v1/profiles")
public class ProfileController {

    /**
     * The application service that handles all profile business logic, including
     * ownership verification, domain object construction, and persistence delegation.
     */
    private final ProfileService profileService;

    /**
     * Constructs a {@code ProfileController} with the required profile service dependency.
     *
     * <p>Constructor injection is used to make the dependency explicit and to support
     * unit testing with a mock {@link ProfileService}.</p>
     *
     * @param profileService the service handling profile use cases; must not be {@code null}
     */
    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    /**
     * Returns all profiles belonging to the currently authenticated user.
     *
     * <p>HTTP Method: {@code GET}</p>
     * <p>Path: {@code /api/v1/profiles}</p>
     * <p>Response: {@code 200 OK} with a JSON array of {@link ProfileDto} objects.
     * The array is empty if the user has no profiles.</p>
     *
     * @param authentication the Spring Security authentication token for the current request;
     *                       injected by the framework; must not be {@code null}
     * @return a list of {@link ProfileDto} records owned by the authenticated user;
     *         never {@code null}, but may be empty
     */
    @GetMapping
    public List<ProfileDto> getMyProfiles(Authentication authentication) {
        return profileService.getMyProfiles(authentication.getName());
    }

    /**
     * Creates a new profile for the authenticated user.
     *
     * <p>HTTP Method: {@code POST}</p>
     * <p>Path: {@code /api/v1/profiles}</p>
     * <p>Request body: A JSON object conforming to {@link CreateProfileRequest}.
     * The {@code profileLabel} and {@code type} fields are required.</p>
     * <p>Response: {@code 201 Created} with the newly created {@link ProfileDto} as the response body.</p>
     *
     * @param req            the validated profile creation request bound from the request body; must not be {@code null}
     * @param authentication the Spring Security authentication token for the current request;
     *                       used to identify the owning user
     * @return the {@link ProfileDto} representing the newly created profile,
     *         including the server-assigned UUID and {@code createdAt} timestamp
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProfileDto create(@Valid @RequestBody CreateProfileRequest req,
                             Authentication authentication) {
        return profileService.create(req, authentication.getName());
    }

    /**
     * Returns a specific profile by its UUID, provided it belongs to the authenticated user.
     *
     * <p>HTTP Method: {@code GET}</p>
     * <p>Path: {@code /api/v1/profiles/{id}}</p>
     * <p>Response: {@code 200 OK} with the matching {@link ProfileDto} as the response body.</p>
     * <p>Response: {@code 404 Not Found} if no profile with the given ID exists.</p>
     * <p>Response: {@code 403 Forbidden} if the profile is owned by a different user.</p>
     *
     * @param id             the UUID of the profile to retrieve, bound from the path variable
     * @param authentication the Spring Security authentication token for ownership verification
     * @return the {@link ProfileDto} of the requested profile
     */
    @GetMapping("/{id}")
    public ProfileDto getById(@PathVariable UUID id, Authentication authentication) {
        return profileService.getById(id, authentication.getName());
    }

    /**
     * Replaces all editable fields of an existing profile with the data from the request body.
     *
     * <p>HTTP Method: {@code PUT}</p>
     * <p>Path: {@code /api/v1/profiles/{id}}</p>
     * <p>Request body: A JSON object conforming to {@link UpdateProfileRequest}.
     * All fields are applied as a full replacement; omitted optional fields will be set to {@code null}.</p>
     * <p>Response: {@code 200 OK} with the updated {@link ProfileDto} as the response body.</p>
     * <p>Response: {@code 404 Not Found} if no profile with the given ID exists.</p>
     * <p>Response: {@code 403 Forbidden} if the profile is owned by a different user.</p>
     *
     * @param id             the UUID of the profile to update, bound from the path variable
     * @param req            the validated profile update request bound from the request body; must not be {@code null}
     * @param authentication the Spring Security authentication token for ownership verification
     * @return the {@link ProfileDto} reflecting the updated state of the profile
     */
    @PutMapping("/{id}")
    public ProfileDto update(@PathVariable UUID id,
                             @Valid @RequestBody UpdateProfileRequest req,
                             Authentication authentication) {
        return profileService.update(id, req, authentication.getName());
    }

    /**
     * Permanently deletes the profile identified by the given UUID.
     *
     * <p>HTTP Method: {@code DELETE}</p>
     * <p>Path: {@code /api/v1/profiles/{id}}</p>
     * <p>Response: {@code 204 No Content} on successful deletion (no response body).</p>
     * <p>Response: {@code 404 Not Found} if no profile with the given ID exists.</p>
     * <p>Response: {@code 403 Forbidden} if the profile is owned by a different user.</p>
     *
     * @param id             the UUID of the profile to delete, bound from the path variable
     * @param authentication the Spring Security authentication token for ownership verification
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        profileService.delete(id, authentication.getName());
    }

    @PostMapping(value = "/{id}/logo", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadLogo(@PathVariable UUID id,
                           @RequestParam("file") MultipartFile file,
                           Authentication authentication) {
        profileService.uploadLogo(id, file, authentication.getName());
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id, Authentication authentication) {
        var logo = profileService.getLogo(id, authentication.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, logo.contentType())
                .body(logo.bytes());
    }

    @DeleteMapping("/{id}/logo")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLogo(@PathVariable UUID id, Authentication authentication) {
        profileService.deleteLogo(id, authentication.getName());
    }
}
