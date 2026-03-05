package com.briefix.profile.repository;

import com.briefix.profile.mapper.ProfileMapper;
import com.briefix.profile.model.Profile;
import com.briefix.user.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL-backed implementation of the {@link ProfileRepository} domain port.
 *
 * <p>This class bridges the profile domain layer and the JPA infrastructure by delegating
 * all persistence operations to {@link ProfileJpaRepository} and translating between
 * {@link com.briefix.profile.entity.ProfileEntity} and the {@link Profile} domain record
 * via {@link ProfileMapper}. It is the only class permitted to interact directly
 * with {@link ProfileJpaRepository}.</p>
 *
 * <p>This implementation also depends on {@link UserJpaRepository} to resolve the owning
 * {@link com.briefix.user.entity.UserEntity} during save operations, which is required to
 * populate the JPA {@code @ManyToOne} relationship on {@link com.briefix.profile.entity.ProfileEntity}.</p>
 *
 * <p>This class is registered as a Spring {@code @Repository} bean, enabling Spring's
 * exception translation mechanism to convert JPA-specific exceptions into the
 * {@link org.springframework.dao.DataAccessException} hierarchy.</p>
 *
 * <p>Thread-safety: This class is stateless and relies exclusively on injected,
 * thread-safe singleton dependencies. It is safe for concurrent use.</p>
 */
@Repository
public class ProfileRepositoryImpl implements ProfileRepository {

    /**
     * The Spring Data JPA repository used to execute all database queries and mutations
     * against the {@code profiles} table. Accessed exclusively through this implementation.
     */
    private final ProfileJpaRepository jpaRepository;

    /**
     * Mapper responsible for converting between {@link com.briefix.profile.entity.ProfileEntity}
     * and the {@link Profile} domain record. Injected to maintain clean layer separation.
     */
    private final ProfileMapper profileMapper;

    /**
     * Used to resolve the owning {@link com.briefix.user.entity.UserEntity} by UUID during
     * profile save operations. Required because the JPA {@code @ManyToOne} relationship on
     * {@link com.briefix.profile.entity.ProfileEntity} requires a managed entity reference
     * rather than a raw UUID.
     */
    private final UserJpaRepository userJpaRepository;

    /**
     * Constructs a {@code ProfileRepositoryImpl} with the required repositories and mapper.
     *
     * <p>All dependencies are mandatory. Spring injects them via constructor injection,
     * which is the preferred pattern for testability and immutability of the dependency graph.</p>
     *
     * @param jpaRepository     the Spring Data JPA repository for profile entities; must not be {@code null}
     * @param profileMapper     the mapper for converting between entity and domain model; must not be {@code null}
     * @param userJpaRepository the JPA repository used to look up user entities by UUID; must not be {@code null}
     */
    public ProfileRepositoryImpl(ProfileJpaRepository jpaRepository,
                                  ProfileMapper profileMapper,
                                  UserJpaRepository userJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.profileMapper = profileMapper;
        this.userJpaRepository = userJpaRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Queries {@link ProfileJpaRepository#findByUserId(UUID)} for all profile entities
     * belonging to the specified user and maps each result to a {@link Profile} domain record
     * using a stream pipeline. The list is collected and returned as an immutable list.</p>
     *
     * @param userId the UUID of the user whose profiles should be loaded; must not be {@code null}
     * @return a list of {@link Profile} domain records; never {@code null}, may be empty
     */
    @Override
    public List<Profile> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(profileMapper::toModel)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs a primary-key lookup via {@link ProfileJpaRepository#findById(Object)}
     * and maps the result to a {@link Profile} domain record if present.</p>
     *
     * @param id the UUID of the profile to look up; must not be {@code null}
     * @return an {@link Optional} containing the {@link Profile} if found, or empty if not
     */
    @Override
    public Optional<Profile> findById(UUID id) {
        return jpaRepository.findById(id).map(profileMapper::toModel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the owning {@link com.briefix.user.entity.UserEntity} from
     * {@link UserJpaRepository} using the provided {@code userId}. If no user exists
     * with that UUID, an {@link IllegalArgumentException} is thrown to surface the
     * referential integrity violation before reaching the database. The profile is then
     * mapped to a {@link com.briefix.profile.entity.ProfileEntity} and persisted
     * via {@link ProfileJpaRepository#save(Object)}. The saved entity is mapped back
     * to a domain record to capture any server-generated fields.</p>
     *
     * @param profile the profile domain record to persist; must not be {@code null}
     * @param userId  the UUID of the owning user; must not be {@code null} and must correspond
     *                to an existing user record
     * @return the saved {@link Profile} with all server-assigned fields populated
     * @throws IllegalArgumentException if no user exists with the given {@code userId}
     */
    @Override
    public Profile save(Profile profile, UUID userId) {
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        var entity = profileMapper.toEntity(profile, user);
        var saved = jpaRepository.save(entity);
        return profileMapper.toModel(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link ProfileJpaRepository#deleteById(Object)}. If no profile
     * with the given ID exists, the call completes silently without throwing an exception.</p>
     *
     * @param id the UUID of the profile to delete; must not be {@code null}
     */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
