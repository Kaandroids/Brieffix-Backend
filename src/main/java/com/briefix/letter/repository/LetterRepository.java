package com.briefix.letter.repository;

import com.briefix.letter.model.Letter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level repository abstraction for {@link Letter} persistence
 * operations.
 *
 * <p>{@code LetterRepository} defines the contract for storing and retrieving
 * letters without exposing any JPA or database-specific details to the service
 * layer.  This interface follows the Repository pattern: the service layer
 * depends only on this interface, while the concrete implementation
 * ({@code LetterRepositoryImpl}) handles the translation between the domain
 * model and the JPA entity layer.</p>
 *
 * <p>All operations work exclusively with the {@link Letter} domain model.
 * Callers are shielded from persistence concerns such as entity management,
 * transaction demarcation, JSONB serialisation of snapshots, and lazy-loading
 * of associated entities.</p>
 *
 * <p><strong>Thread safety:</strong> implementations must be thread-safe.
 * The provided {@code LetterRepositoryImpl} delegates to Spring Data JPA,
 * which is thread-safe when used within a transactional context.</p>
 */
public interface LetterRepository {

    /**
     * Retrieves all letters that belong to the specified user.
     *
     * @param userId the UUID of the user whose letters should be returned;
     *               must not be {@code null}
     * @return an unmodifiable list of {@link Letter} domain models owned by the
     *         user; empty list if the user has no letters
     */
    List<Letter> findByUserId(UUID userId);

    /**
     * Retrieves a single letter by its unique identifier.
     *
     * @param id the UUID of the letter to look up; must not be {@code null}
     * @return an {@link Optional} containing the matching {@link Letter}, or an
     *         empty {@link Optional} if no letter with the given id exists
     */
    Optional<Letter> findById(UUID id);

    /**
     * Persists a letter (insert or update) and returns the saved state.
     *
     * <p>If {@code letter.id()} is {@code null} the implementation creates a
     * new record; if an id is present the implementation updates the existing
     * record.  The {@code userId} parameter is required to resolve the
     * {@code UserEntity} association that the JPA layer needs.  Snapshot
     * objects ({@link com.briefix.letter.model.SenderSnapshot} and
     * {@link com.briefix.letter.model.RecipientSnapshot}) are serialised to
     * JSONB automatically by the Hibernate type mapping.</p>
     *
     * @param letter the domain model to persist; must not be {@code null}
     * @param userId the UUID of the user who owns this letter; must correspond
     *               to an existing user record
     * @return the saved {@link Letter} domain model, including any
     *         database-generated values (e.g. the assigned UUID and
     *         {@code createdAt} timestamp)
     * @throws IllegalStateException if no user exists with the given
     *                               {@code userId}
     */
    Letter save(Letter letter, UUID userId);

    /**
     * Permanently removes the letter identified by the given id.
     *
     * <p>If no letter with the supplied id exists the method returns silently
     * without throwing an exception, consistent with idempotent delete
     * semantics.</p>
     *
     * @param id the UUID of the letter to delete; must not be {@code null}
     */
    void deleteById(UUID id);
}
