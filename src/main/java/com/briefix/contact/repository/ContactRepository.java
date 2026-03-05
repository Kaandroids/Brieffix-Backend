package com.briefix.contact.repository;

import com.briefix.contact.model.Contact;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain-level repository abstraction for {@link Contact} persistence operations.
 *
 * <p>{@code ContactRepository} defines the contract for storing and retrieving
 * contacts without exposing any JPA or database-specific details to the service
 * layer.  This interface follows the Repository pattern: the service layer
 * depends only on this interface, while the concrete implementation
 * ({@code ContactRepositoryImpl}) handles the translation between the domain
 * model and the JPA entity layer.</p>
 *
 * <p>All operations work exclusively with the {@link Contact} domain model.
 * Callers are shielded from persistence concerns such as entity management,
 * transaction demarcation, and lazy-loading.</p>
 *
 * <p><strong>Thread safety:</strong> implementations must be thread-safe.
 * The provided {@code ContactRepositoryImpl} delegates to Spring Data JPA,
 * which is thread-safe when used within a transactional context.</p>
 */
public interface ContactRepository {

    /**
     * Retrieves all contacts that belong to the specified user.
     *
     * @param userId the UUID of the user whose contacts should be returned;
     *               must not be {@code null}
     * @return an unmodifiable list of {@link Contact} domain models owned by
     *         the user; empty list if the user has no contacts
     */
    List<Contact> findByUserId(UUID userId);

    /**
     * Retrieves a single contact by its unique identifier.
     *
     * @param id the UUID of the contact to look up; must not be {@code null}
     * @return an {@link Optional} containing the matching {@link Contact}, or
     *         an empty {@link Optional} if no contact with the given id exists
     */
    Optional<Contact> findById(UUID id);

    /**
     * Persists a contact (insert or update) and returns the saved state.
     *
     * <p>If {@code contact.id()} is {@code null} the implementation creates a
     * new record; if an id is present the implementation updates the existing
     * record.  The {@code userId} parameter is required to resolve the
     * {@code UserEntity} association that the JPA layer needs.</p>
     *
     * @param contact the domain model to persist; must not be {@code null}
     * @param userId  the UUID of the user who owns this contact; must
     *                correspond to an existing user record
     * @return the saved {@link Contact} domain model, including any
     *         database-generated values (e.g. the assigned UUID and
     *         {@code createdAt} timestamp)
     * @throws IllegalArgumentException if no user exists with the given
     *                                  {@code userId}
     */
    Contact save(Contact contact, UUID userId);

    /**
     * Permanently removes the contact identified by the given id.
     *
     * <p>If no contact with the supplied id exists the method returns silently
     * without throwing an exception, consistent with idempotent delete
     * semantics.</p>
     *
     * @param id the UUID of the contact to delete; must not be {@code null}
     */
    void deleteById(UUID id);
}
