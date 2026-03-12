package com.briefix.user.repository;

import com.briefix.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository providing database access for {@link UserEntity} records.
 *
 * <p>This interface is an infrastructure-layer detail and should only be accessed by
 * {@link UserRepositoryImpl}, which translates between {@link UserEntity} and the
 * {@link com.briefix.user.model.User} domain model. Domain-layer classes and service
 * components must depend on {@link UserRepository} rather than this interface
 * to preserve the hexagonal architecture boundary.</p>
 *
 * <p>Spring Data JPA automatically generates the implementation of this interface
 * at application startup based on method name conventions and the inherited
 * {@link JpaRepository} contract. All operations execute within the active
 * Spring transaction context.</p>
 *
 * <p>Thread-safety: The generated proxy implementation is stateless and safe
 * for concurrent use across multiple threads.</p>
 */
public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    /**
     * Queries the {@code users} table for a record whose {@code email} column
     * matches the given value (case-sensitive comparison as stored).
     *
     * <p>Used during authentication flows and existence checks to locate
     * an account by its primary email address.</p>
     *
     * @param email the email address to search for; must not be {@code null}
     * @return an {@link Optional} containing the matching {@link UserEntity}
     *         if a record with that email exists, or {@link Optional#empty()} otherwise
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Checks whether any record in the {@code users} table has the given email address.
     *
     * <p>More efficient than {@link #findByEmail(String)} for duplicate-registration
     * checks because it issues a {@code SELECT 1} or {@code COUNT} query rather
     * than fetching and mapping the full row.</p>
     *
     * @param email the email address to check; must not be {@code null}
     * @return {@code true} if at least one user record with this email exists;
     *         {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Queries the {@code users} table for a record whose {@code verification_token}
     * column matches the given value.
     *
     * <p>Used by {@link UserRepositoryImpl#findByVerificationToken(String)} to
     * locate the entity for the email verification flow.  Spring Data JPA derives
     * the query from this method name at application startup.  Note that this query
     * does not filter on expiry — callers are responsible for checking the
     * {@code verification_token_expiry} column after retrieval.</p>
     *
     * @param verificationToken the verification token string to search for;
     *                          must not be {@code null}
     * @return an {@link Optional} containing the matching {@link UserEntity} if found,
     *         or {@link Optional#empty()} otherwise
     */
    Optional<UserEntity> findByVerificationToken(String verificationToken);

    Optional<UserEntity> findByPasswordResetToken(String passwordResetToken);

    Page<UserEntity> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}
