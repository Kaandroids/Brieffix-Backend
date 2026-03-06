package com.briefix.user.repository;

import com.briefix.user.mapper.UserMapper;
import com.briefix.user.model.User;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * PostgreSQL-backed implementation of the {@link UserRepository} domain port.
 *
 * <p>This class bridges the domain layer and the JPA infrastructure by delegating
 * all persistence operations to {@link UserJpaRepository} and translating between
 * {@link com.briefix.user.entity.UserEntity} and the {@link User} domain record
 * using {@link UserMapper}. It is the only class permitted to interact directly
 * with {@link UserJpaRepository}.</p>
 *
 * <p>This implementation is registered as a Spring {@code @Repository} bean,
 * which enables Spring to apply exception translation, converting JPA-specific
 * exceptions (e.g., {@link jakarta.persistence.PersistenceException}) into
 * Spring's {@link org.springframework.dao.DataAccessException} hierarchy.</p>
 *
 * <p>Thread-safety: This class is stateless and relies exclusively on injected,
 * thread-safe dependencies. It is safe for concurrent use as a Spring singleton.</p>
 */
@Repository
public class UserRepositoryImpl implements UserRepository {

    /**
     * The Spring Data JPA repository used to execute all database queries and mutations
     * against the {@code users} table. Accessed exclusively through this implementation class.
     */
    private final UserJpaRepository jpaRepository;

    /**
     * Mapper responsible for converting between {@link com.briefix.user.entity.UserEntity}
     * and the {@link User} domain record. Injected to maintain clean layer separation.
     */
    private final UserMapper userMapper;

    /**
     * Constructs a {@code UserRepositoryImpl} with the required JPA repository and mapper.
     *
     * <p>Both dependencies are mandatory. Spring will inject them via constructor injection,
     * which is the preferred pattern for testability and immutability of the dependency graph.</p>
     *
     * @param jpaRepository the Spring Data JPA repository for {@link com.briefix.user.entity.UserEntity}; must not be {@code null}
     * @param userMapper    the mapper for converting between entity and domain model; must not be {@code null}
     */
    public UserRepositoryImpl(UserJpaRepository jpaRepository, UserMapper userMapper) {
        this.jpaRepository = jpaRepository;
        this.userMapper = userMapper;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Converts the domain {@link User} to a {@link com.briefix.user.entity.UserEntity},
     * delegates to {@link UserJpaRepository#save(Object)}, and maps the saved entity
     * back to a domain record. The returned instance reflects any server-assigned values
     * such as a generated UUID or the {@code createdAt} timestamp set by the
     * {@code @PrePersist} lifecycle hook.</p>
     *
     * @param user the user aggregate to save; must not be {@code null}
     * @return the saved {@link User} with all server-assigned fields populated
     */
    @Override
    public User save(User user) {
        var entity = userMapper.toEntity(user);
        var saved = jpaRepository.save(entity);
        return userMapper.toModel(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Issues a primary-key lookup against the {@code users} table via
     * {@link UserJpaRepository#findById(Object)} and maps the result to a domain record.</p>
     *
     * @param id the UUID of the user to look up; must not be {@code null}
     * @return an {@link Optional} containing the {@link User} if found, or empty if not
     */
    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(userMapper::toModel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Executes a derived query on the {@code email} column via
     * {@link UserJpaRepository#findByEmail(String)} and maps the result to a domain record.</p>
     *
     * @param email the email address to search for; must not be {@code null}
     * @return an {@link Optional} containing the {@link User} if found, or empty if not
     */
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(userMapper::toModel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates directly to {@link UserJpaRepository#existsByEmail(String)},
     * which issues an efficient existence query without loading the full entity.</p>
     *
     * @param email the email address to check; must not be {@code null}
     * @return {@code true} if a matching record exists; {@code false} otherwise
     */
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link UserJpaRepository#deleteById(Object)}. If no user with
     * the given ID exists, the call completes silently without throwing an exception.</p>
     *
     * @param id the UUID of the user to delete; must not be {@code null}
     */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public Optional<User> findByVerificationToken(String verificationToken) {
        return jpaRepository.findByVerificationToken(verificationToken).map(userMapper::toModel);
    }
}
