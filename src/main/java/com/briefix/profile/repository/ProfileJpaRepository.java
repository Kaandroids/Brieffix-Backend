package com.briefix.profile.repository;

import com.briefix.profile.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository providing database access for {@link ProfileEntity} records.
 *
 * <p>This interface is an infrastructure-layer detail and should only be accessed by
 * {@link ProfileRepositoryImpl}, which translates between {@link ProfileEntity} and the
 * {@link com.briefix.profile.model.Profile} domain model. Domain-layer classes and service
 * components must depend on {@link ProfileRepository} rather than this interface
 * to preserve the hexagonal architecture boundary.</p>
 *
 * <p>Spring Data JPA automatically generates the implementation of this interface
 * at application startup based on method name conventions and the inherited
 * {@link JpaRepository} contract. All operations execute within the active Spring
 * transaction context.</p>
 *
 * <p>Thread-safety: The generated proxy implementation is stateless and safe
 * for concurrent use across multiple threads.</p>
 */
public interface ProfileJpaRepository extends JpaRepository<ProfileEntity, UUID> {

    /**
     * Queries the {@code profiles} table for all records whose {@code user_id} column
     * matches the given user UUID.
     *
     * <p>Used to load all profiles belonging to a specific user. The returned list
     * reflects the order returned by the database and may be empty if the user has
     * no profiles. Each {@link ProfileEntity} in the list will have its
     * {@code user} association initialized (due to the join on {@code user_id}).</p>
     *
     * @param userId the UUID of the owning user to filter by; must not be {@code null}
     * @return a list of {@link ProfileEntity} records associated with the given user UUID;
     *         never {@code null}, but may be empty
     */
    List<ProfileEntity> findByUserId(UUID userId);
}
