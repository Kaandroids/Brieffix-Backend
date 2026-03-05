package com.briefix.contact.repository;

import com.briefix.contact.entity.ContactEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ContactEntity} persistence operations.
 *
 * <p>{@code ContactJpaRepository} extends {@link JpaRepository} to inherit the
 * full set of standard CRUD and pagination operations for the {@code contacts}
 * table.  It adds one custom derived query method,
 * {@link #findByUserId(UUID)}, which Spring Data JPA resolves automatically
 * by inspecting the method name convention.</p>
 *
 * <p>This interface operates at the JPA entity layer and must not be used
 * directly by the service layer.  It is an internal implementation detail of
 * {@code ContactRepositoryImpl}, which translates between
 * {@link ContactEntity} and the domain model {@code Contact}.</p>
 *
 * <p><strong>Thread safety:</strong> Spring Data JPA repository proxies are
 * thread-safe and are intended to be used as singleton-scoped Spring beans.
 * Each method invocation participates in the ambient transaction, or a new
 * one is started according to the default transaction propagation rules.</p>
 */
public interface ContactJpaRepository extends JpaRepository<ContactEntity, UUID> {

    /**
     * Retrieves all {@link ContactEntity} records whose {@code user.id} matches
     * the given {@code userId}.
     *
     * <p>Spring Data JPA derives the query from the method name at application
     * startup; no explicit JPQL or native SQL is required.  The generated query
     * performs an inner join against the {@code users} table on the
     * {@code user_id} foreign key.</p>
     *
     * @param userId the UUID of the user whose contacts should be retrieved;
     *               must not be {@code null}
     * @return a list of {@link ContactEntity} objects belonging to the specified
     *         user; empty list if the user has no contacts
     */
    List<ContactEntity> findByUserId(UUID userId);
}
