package com.briefix.user.repository;

import com.briefix.user.model.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository port defining the persistence contract for the {@link User} aggregate.
 *
 * <p>This interface follows the repository pattern from Domain-Driven Design (DDD).
 * It acts as a domain-facing port, decoupling the core domain from any specific
 * persistence technology. Implementations are provided in the infrastructure layer
 * (e.g., {@link UserRepositoryImpl} backed by JPA/PostgreSQL) and are injected
 * via Spring's dependency injection mechanism.</p>
 *
 * <p>All methods in this interface operate on domain objects ({@link User}) and
 * primitive identifiers ({@link UUID}, {@link String}). Infrastructure-specific
 * concerns such as entity mapping, transaction management, caching, and query
 * optimization are the sole responsibility of the implementing class.</p>
 *
 * <p>Callers at the service layer should always program to this interface, never
 * to a concrete implementation, in order to preserve testability and architectural
 * flexibility.</p>
 *
 * <p>Thread-safety: Implementations are expected to be stateless and safe for
 * concurrent use as Spring-managed singletons.</p>
 */
public interface UserRepository {

    /**
     * Persists a new user or merges updates to an existing user record.
     *
     * <p>If the provided {@link User} has a non-null {@code id} that matches an
     * existing record, the operation behaves as an update. If the {@code id} is
     * {@code null} or unknown, a new record is created with a generated UUID.</p>
     *
     * @param user the user aggregate to save; must not be {@code null}
     * @return the saved {@link User} instance, potentially enriched with
     *         a generated {@code id} or server-assigned {@code createdAt} timestamp
     */
    User save(User user);

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the UUID of the user to look up; must not be {@code null}
     * @return an {@link Optional} containing the matching {@link User} if found,
     *         or {@link Optional#empty()} if no user exists with the given ID
     */
    Optional<User> findById(UUID id);

    /**
     * Retrieves a user by their primary email address.
     *
     * <p>Used primarily during authentication flows to locate an account
     * associated with a given email prior to credential verification or
     * token issuance.</p>
     *
     * @param email the email address to search for; must not be {@code null} or blank
     * @return an {@link Optional} containing the matching {@link User} if found,
     *         or {@link Optional#empty()} if no account is registered with that email
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user account with the given email address already exists.
     *
     * <p>Intended for duplicate registration checks. Prefer this method over
     * {@link #findByEmail(String)} when only existence needs to be determined,
     * as it avoids loading and mapping the full aggregate.</p>
     *
     * @param email the email address to check; must not be {@code null} or blank
     * @return {@code true} if a user with this email exists in the system;
     *         {@code false} otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Permanently removes the user record identified by the given UUID.
     *
     * <p>This is a hard delete with no soft-delete or tombstone mechanism.
     * Callers must ensure that any applicable business rules around user removal
     * (e.g., data retention policies, cascade deletion of related profiles or documents)
     * are enforced at the service layer before invoking this method.</p>
     *
     * <p>If no user with the given ID exists, the operation completes silently
     * without throwing an exception.</p>
     *
     * @param id the UUID of the user to delete; must not be {@code null}
     */
    void deleteById(UUID id);
}
