package com.briefix.profile.service;

import com.briefix.profile.dto.CreateProfileRequest;
import com.briefix.profile.dto.ProfileDto;
import com.briefix.profile.dto.UpdateProfileRequest;
import com.briefix.profile.exception.ProfileNotFoundException;
import com.briefix.profile.mapper.ProfileMapper;
import com.briefix.profile.model.Profile;
import com.briefix.profile.repository.ProfileRepository;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Default implementation of the {@link ProfileService} application service interface.
 *
 * <p>This class orchestrates all profile-related CRUD operations by coordinating between
 * {@link ProfileRepository} for profile persistence, {@link UserRepository} for user
 * lookups, and {@link ProfileMapper} for translating between domain records and DTOs.
 * It enforces ownership-based access control on all operations by verifying that the
 * authenticated user (identified by their email address) owns the profile being accessed
 * or modified.</p>
 *
 * <p>The ownership check is implemented in the private {@link #checkOwnership(Profile, String)}
 * helper method, which is invoked before any read or mutation on a specific profile ID.
 * Violations result in a Spring Security {@link AccessDeniedException}, which the security
 * framework maps to an HTTP {@code 403 Forbidden} response.</p>
 *
 * <p>This class is registered as a Spring {@code @Service} singleton and is injected
 * wherever {@link ProfileService} is required, enabling mocking in unit tests.</p>
 *
 * <p>Thread-safety: This class is stateless and relies exclusively on thread-safe,
 * singleton-scoped dependencies. It is safe for concurrent use across multiple threads.</p>
 */
@Service
public class ProfileServiceImpl implements ProfileService {

    /**
     * Repository port for profile persistence operations. Used to load, save,
     * and delete {@link Profile} domain records.
     */
    private final ProfileRepository profileRepository;

    /**
     * Mapper for converting between {@link Profile} domain records and
     * {@link ProfileDto} API representations.
     */
    private final ProfileMapper profileMapper;

    /**
     * Repository port for user lookups. Used to resolve the authenticated user's
     * {@link com.briefix.user.model.User} record by email address during ownership
     * verification and profile creation.
     */
    private final UserRepository userRepository;

    /**
     * Constructs a {@code ProfileServiceImpl} with the required repository and mapper dependencies.
     *
     * <p>Constructor injection is used to make all dependencies mandatory and to facilitate
     * straightforward unit testing with mock implementations.</p>
     *
     * @param profileRepository the repository for profile persistence operations; must not be {@code null}
     * @param profileMapper     the mapper for converting between domain records and DTOs; must not be {@code null}
     * @param userRepository    the repository for user lookup operations; must not be {@code null}
     */
    public ProfileServiceImpl(ProfileRepository profileRepository,
                               ProfileMapper profileMapper,
                               UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.profileMapper = profileMapper;
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user by email via {@link UserRepository#findByEmail(String)}, then
     * delegates to {@link ProfileRepository#findByUserId(UUID)} to load all owned profiles.
     * Each result is mapped to a {@link ProfileDto} via a stream pipeline.</p>
     *
     * @param email the email address of the authenticated user; must not be {@code null}
     * @return a list of {@link ProfileDto} records owned by the user; never {@code null}
     * @throws UserNotFoundException if no user exists with the given email
     */
    @Override
    public List<ProfileDto> getMyProfiles(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return profileRepository.findByUserId(user.id()).stream()
                .map(profileMapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the profile by UUID, then delegates to {@link #checkOwnership(Profile, String)}
     * to verify that the authenticated user owns the profile before returning it.</p>
     *
     * @param id    the UUID of the profile to retrieve; must not be {@code null}
     * @param email the authenticated user's email used for ownership verification
     * @return the {@link ProfileDto} if found and owned by the authenticated user
     * @throws ProfileNotFoundException if no profile with the given ID exists
     * @throws AccessDeniedException    if the profile is owned by a different user
     */
    @Override
    public ProfileDto getById(UUID id, String email) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        checkOwnership(profile, email);
        return profileMapper.toDto(profile);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the authenticated user, constructs a new {@link Profile} domain record
     * from the request (with {@code id} and {@code createdAt} set to {@code null} for
     * server-side generation), and persists it via {@link ProfileRepository#save(Profile, UUID)}.
     * If the request's {@code country} field is {@code null}, it defaults to {@code "Deutschland"}.</p>
     *
     * @param req   the validated creation request; must not be {@code null}
     * @param email the authenticated user's email; used to resolve the owning user account
     * @return the {@link ProfileDto} of the newly persisted profile, including generated fields
     * @throws UserNotFoundException if no user exists with the given email
     */
    @Override
    public ProfileDto create(CreateProfileRequest req, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        var profile = new Profile(
                null,
                user.id(),
                req.profileLabel(),
                req.isDefault(),
                req.type(),
                req.salutation(),
                req.title(),
                req.firstName(),
                req.lastName(),
                req.companyName(),
                req.department(),
                req.street(),
                req.streetNumber(),
                req.postalCode(),
                req.city(),
                req.country() != null ? req.country() : "Deutschland",
                req.vatId(),
                req.taxNumber(),
                req.managingDirector(),
                req.registerCourt(),
                req.registerNumber(),
                req.iban(),
                req.bic(),
                req.bankName(),
                req.website(),
                req.phone(),
                req.fax(),
                req.email(),
                req.contactPerson(),
                null,
                false
        );
        var saved = profileRepository.save(profile, user.id());
        return profileMapper.toDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the existing profile to retrieve its immutable fields ({@code id},
     * {@code userId}, {@code createdAt}), verifies ownership, then constructs a new
     * {@link Profile} domain record that merges the existing immutable fields with the
     * updated values from the request. The updated record is persisted via
     * {@link ProfileRepository#save(Profile, UUID)}.</p>
     *
     * @param id    the UUID of the profile to update; must not be {@code null}
     * @param req   the validated update request containing replacement field values; must not be {@code null}
     * @param email the authenticated user's email used for ownership verification
     * @return the {@link ProfileDto} reflecting the updated state of the profile
     * @throws ProfileNotFoundException if no profile with the given ID exists
     * @throws AccessDeniedException    if the profile is owned by a different user
     */
    @Override
    public ProfileDto update(UUID id, UpdateProfileRequest req, String email) {
        var existing = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        checkOwnership(existing, email);
        var updated = new Profile(
                existing.id(),
                existing.userId(),
                req.profileLabel(),
                req.isDefault(),
                req.type(),
                req.salutation(),
                req.title(),
                req.firstName(),
                req.lastName(),
                req.companyName(),
                req.department(),
                req.street(),
                req.streetNumber(),
                req.postalCode(),
                req.city(),
                req.country() != null ? req.country() : "Deutschland",
                req.vatId(),
                req.taxNumber(),
                req.managingDirector(),
                req.registerCourt(),
                req.registerNumber(),
                req.iban(),
                req.bic(),
                req.bankName(),
                req.website(),
                req.phone(),
                req.fax(),
                req.email(),
                req.contactPerson(),
                existing.createdAt(),
                existing.hasLogo()
        );
        var saved = profileRepository.save(updated, existing.userId());
        return profileMapper.toDto(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the profile by UUID, verifies ownership, then delegates to
     * {@link ProfileRepository#deleteById(UUID)} to permanently remove the record.</p>
     *
     * @param id    the UUID of the profile to delete; must not be {@code null}
     * @param email the authenticated user's email used for ownership verification
     * @throws ProfileNotFoundException if no profile with the given ID exists
     * @throws AccessDeniedException    if the profile is owned by a different user
     */
    @Override
    public void delete(UUID id, String email) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        checkOwnership(profile, email);
        profileRepository.deleteById(id);
    }

    /**
     * Verifies that the given profile is owned by the user identified by the provided email.
     *
     * <p>Resolves the user account from the repository by email, then compares the user's UUID
     * against the profile's {@code userId}. If they do not match, an {@link AccessDeniedException}
     * is thrown to prevent unauthorized cross-user access to profile data.</p>
     *
     * <p>This method is invoked as a guard before any profile-level read or mutation
     * operation that targets a specific profile ID.</p>
     *
     * @param profile the profile whose ownership is being validated; must not be {@code null}
     * @param email   the email address of the authenticated user requesting access; must not be {@code null}
     * @throws UserNotFoundException  if no user account exists for the given email
     * @throws AccessDeniedException if the authenticated user does not own the given profile
     */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/png", "image/jpeg", "image/x-icon", "image/vnd.microsoft.icon", "image/svg+xml"
    );
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024; // 2 MB

    @Override
    public void uploadLogo(UUID id, MultipartFile file, String email) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        checkOwnership(profile, email);
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new IllegalArgumentException("Logo must not exceed 2 MB");
        }
        try {
            profileRepository.saveLogo(id, file.getBytes(), contentType);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }

    @Override
    public ProfileService.LogoData getLogo(UUID id, String email) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        checkOwnership(profile, email);
        byte[] bytes = profileRepository.findLogoById(id)
                .orElseThrow(() -> new IllegalStateException("No logo for profile: " + id));
        String contentType = profileRepository.findLogoContentTypeById(id).orElse("application/octet-stream");
        return new ProfileService.LogoData(bytes, contentType);
    }

    @Override
    public void deleteLogo(UUID id, String email) {
        var profile = profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
        checkOwnership(profile, email);
        profileRepository.deleteLogo(id);
    }

    private void checkOwnership(Profile profile, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        if (!profile.userId().equals(user.id())) {
            throw new AccessDeniedException("Access denied to profile: " + profile.id());
        }
    }
}
