package com.briefix.profile.service;

import com.briefix.profile.dto.CreateProfileRequest;
import com.briefix.profile.dto.ProfileDto;
import com.briefix.profile.dto.UpdateProfileRequest;

import java.util.List;
import java.util.UUID;

/**
 * Application service interface defining the use cases available for the profile domain.
 *
 * <p>This interface serves as the primary entry point for profile-related business
 * operations within the application layer. All methods accept the authenticated user's
 * email address as a parameter (extracted from the Spring Security context) to enforce
 * ownership-based access control, ensuring users can only read and modify their own profiles.</p>
 *
 * <p>The interface operates exclusively on DTO types ({@link ProfileDto},
 * {@link CreateProfileRequest}, {@link UpdateProfileRequest}) to ensure that domain
 * internals are not exposed to the presentation layer. The default implementation
 * is {@link ProfileServiceImpl}.</p>
 *
 * <p>Controllers must depend on this interface rather than any concrete implementation
 * to preserve testability, enable mocking, and maintain architectural decoupling.</p>
 *
 * <p>Thread-safety: Implementations are expected to be stateless Spring {@code @Service}
 * beans and safe for concurrent use across multiple request threads.</p>
 */
public interface ProfileService {

    /**
     * Retrieves all profiles belonging to the authenticated user.
     *
     * <p>The user is identified by their email address extracted from the security
     * context. Returns an empty list if the user has not yet created any profiles.</p>
     *
     * @param email the email address of the authenticated user; must not be {@code null} or blank
     * @return an unmodifiable list of {@link ProfileDto} records owned by the user;
     *         never {@code null}, but may be empty
     * @throws com.briefix.user.exception.UserNotFoundException if no user account exists for the given email
     */
    List<ProfileDto> getMyProfiles(String email);

    /**
     * Retrieves a specific profile by its UUID, enforcing ownership by the authenticated user.
     *
     * <p>If the profile exists but belongs to a different user, an
     * {@link org.springframework.security.access.AccessDeniedException} is thrown to
     * prevent cross-user data access.</p>
     *
     * @param id    the UUID of the profile to retrieve; must not be {@code null}
     * @param email the email address of the authenticated user used for ownership verification
     * @return the {@link ProfileDto} corresponding to the requested profile
     * @throws com.briefix.profile.exception.ProfileNotFoundException if no profile exists with the given ID
     * @throws org.springframework.security.access.AccessDeniedException if the profile is owned by a different user
     */
    ProfileDto getById(UUID id, String email);

    /**
     * Creates a new profile for the authenticated user from the given request data.
     *
     * <p>The profile is associated with the user account identified by {@code email}.
     * If {@code country} is not specified in the request, it defaults to {@code "Deutschland"}.
     * A new UUID is generated and {@code createdAt} is set server-side.</p>
     *
     * @param req   the validated request object containing all profile creation data; must not be {@code null}
     * @param email the email address of the authenticated user who will own the new profile
     * @return the {@link ProfileDto} representing the newly created profile, including
     *         the server-assigned UUID and {@code createdAt} timestamp
     * @throws com.briefix.user.exception.UserNotFoundException if no user account exists for the given email
     */
    ProfileDto create(CreateProfileRequest req, String email);

    /**
     * Updates an existing profile identified by its UUID with the data from the given request.
     *
     * <p>This is a full replacement (PUT semantics): all editable fields on the existing profile
     * are overwritten with values from the request. Immutable fields ({@code id}, {@code userId},
     * {@code createdAt}) are preserved from the existing record. Ownership is verified before
     * applying the update.</p>
     *
     * @param id    the UUID of the profile to update; must not be {@code null}
     * @param req   the validated request object containing the updated profile data; must not be {@code null}
     * @param email the email address of the authenticated user used for ownership verification
     * @return the {@link ProfileDto} representing the updated profile state
     * @throws com.briefix.profile.exception.ProfileNotFoundException if no profile exists with the given ID
     * @throws org.springframework.security.access.AccessDeniedException if the profile is owned by a different user
     */
    ProfileDto update(UUID id, UpdateProfileRequest req, String email);

    /**
     * Deletes the profile identified by the given UUID after verifying ownership.
     *
     * <p>This is a hard delete with no soft-delete or recovery mechanism. Ownership
     * is verified before deletion; if the profile belongs to a different user,
     * an {@link org.springframework.security.access.AccessDeniedException} is thrown.</p>
     *
     * @param id    the UUID of the profile to delete; must not be {@code null}
     * @param email the email address of the authenticated user used for ownership verification
     * @throws com.briefix.profile.exception.ProfileNotFoundException if no profile exists with the given ID
     * @throws org.springframework.security.access.AccessDeniedException if the profile is owned by a different user
     */
    void delete(UUID id, String email);
}
