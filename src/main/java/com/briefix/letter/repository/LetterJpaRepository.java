package com.briefix.letter.repository;

import com.briefix.letter.entity.LetterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LetterEntity} persistence operations.
 *
 * <p>{@code LetterJpaRepository} extends {@link JpaRepository} to inherit the
 * full set of standard CRUD and pagination operations for the {@code letters}
 * table.  It adds one custom derived query method,
 * {@link #findByUserId(UUID)}, which Spring Data JPA resolves automatically
 * by inspecting the method name convention at application startup.</p>
 *
 * <p>This interface operates at the JPA entity layer and must not be used
 * directly by the service layer.  It is an internal implementation detail of
 * {@code LetterRepositoryImpl}, which translates between
 * {@link LetterEntity} and the domain model
 * {@link com.briefix.letter.model.Letter}.</p>
 *
 * <p><strong>Thread safety:</strong> Spring Data JPA repository proxies are
 * thread-safe and intended to be used as singleton-scoped Spring beans.  Each
 * method invocation participates in the ambient transaction, or a new one is
 * started according to the default transaction propagation rules.</p>
 */
public interface LetterJpaRepository extends JpaRepository<LetterEntity, UUID> {

    /**
     * Retrieves all {@link LetterEntity} records whose {@code user.id} matches
     * the given {@code userId}.
     *
     * <p>Spring Data JPA derives the query from the method name at application
     * startup; no explicit JPQL or native SQL is required.  The generated query
     * performs an inner join against the {@code users} table on the
     * {@code user_id} foreign key column.</p>
     *
     * @param userId the UUID of the user whose letters should be retrieved;
     *               must not be {@code null}
     * @return a list of {@link LetterEntity} objects belonging to the specified
     *         user; empty list if the user has no letters
     */
    List<LetterEntity> findByUserId(UUID userId);
}
