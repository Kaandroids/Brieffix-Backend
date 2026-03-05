package com.briefix.profile.repository;

import com.briefix.profile.model.Profile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository port defining the persistence contract for the {@link Profile} aggregate.
 *
 * <p>This interface follows the repository pattern from Domain-Driven Design (DDD).
 * It serves as a domain-facing port that decouples the core domain from any specific
 * persistence technology. Implementations are provided in the infrastructure layer
 * (e.g., {@link ProfileRepositoryImpl} backed by JPA/PostgreSQL) and injected via
 * Spring's dependency injection mechanism.</p>
 *
 * <p>All methods operate exclusively on domain objects ({@link Profile}) and primitive
 * identifiers ({@link UUID}). Infrastructure-specific concerns such as entity mapping,
 * foreign key resolution, transaction management, and query optimization are the sole
 * responsibility of the implementing class.</p>
 *
 * <p>Note that {@link #save(Profile, UUID)} requires the owning user's UUID as a
 * separate parameter because the profile aggregate does not carry a full user object —
 * only a reference ID. The implementation resolves the corresponding
 * {@link com.briefix.user.entity.UserEntity} to satisfy the JPA relationship.</p>
 *
 * <p>Thread-safety: Implementations are expected to be stateless Spring-managed
 * singletons and safe for concurrent use.</p>
 */
public interface ProfileRepository {

    /**
     * Retrieves all profiles belonging to the specified user.
     *
     * <p>Returns profiles in the order determined by the underlying query. The list
     * may be empty if the user has not yet created any profiles; it will never be
     * {@code null}.</p>
     *
     * @param userId the UUID of the user whose profiles should be loaded; must not be {@code null}
     * @return an unmodifiable list of {@link Profile} domain records owned by the user;
     *         never {@code null}, but may be empty
     */
    List<Profile> findByUserId(UUID userId);

    /**
     * Retrieves a single profile by its unique identifier.
     *
     * @param id the UUID of the profile to look up; must not be {@code null}
     * @return an {@link Optional} containing the {@link Profile} if found,
     *         or {@link Optional#empty()} if no profile with the given ID exists
     */
    Optional<Profile> findById(UUID id);

    /**
     * Persists a new profile or updates an existing one, associating it with the given user.
     *
     * <p>If the provided {@link Profile} has a non-null {@code id} that matches an existing
     * record, the operation behaves as a full update. If the {@code id} is {@code null},
     * a new profile is created with a server-generated UUID.</p>
     *
     * <p>The {@code userId} parameter is required to resolve the owning
     * {@link com.briefix.user.entity.UserEntity} in the JPA layer and populate the
     * {@code user_id} foreign key on the resulting {@link com.briefix.profile.entity.ProfileEntity}.</p>
     *
     * @param profile the profile domain record to save; must not be {@code null}
     * @param userId  the UUID of the user who owns this profile; must not be {@code null}
     *                and must correspond to an existing user account
     * @return the saved {@link Profile} instance, with a generated UUID and
     *         server-assigned {@code createdAt} timestamp if newly created
     * @throws IllegalArgumentException if no user exists with the given {@code userId}
     */
    Profile save(Profile profile, UUID userId);

    /**
     * Permanently removes the profile identified by the given UUID.
     *
     * <p>This is a hard delete. Callers must ensure that ownership verification
     * has been performed at the service layer before invoking this method.
     * If no profile with the given ID exists, the operation completes silently.</p>
     *
     * @param id the UUID of the profile to delete; must not be {@code null}
     */
    void deleteById(UUID id);
}
