package com.briefix.letter.repository;

import com.briefix.letter.mapper.LetterMapper;
import com.briefix.letter.model.Letter;
import com.briefix.user.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of {@link LetterRepository} that delegates all
 * persistence work to the Spring Data JPA layer.
 *
 * <p>{@code LetterRepositoryImpl} is the adapter that bridges the domain-model
 * world used by the service layer and the JPA-entity world used by the database
 * layer.  It performs the following responsibilities:</p>
 * <ol>
 *   <li>Invokes {@link LetterJpaRepository} for all CRUD operations.</li>
 *   <li>Uses {@link LetterMapper} to convert between {@code LetterEntity} and
 *       the {@link Letter} domain model in both directions.</li>
 *   <li>Resolves the required {@code UserEntity} association by querying
 *       {@code UserJpaRepository} when saving a letter.</li>
 * </ol>
 *
 * <p><strong>Thread safety:</strong> this class is stateless beyond its
 * injected dependencies, all of which are thread-safe Spring beans.  It is
 * safe to use as a singleton-scoped Spring bean.</p>
 */
@Repository
public class LetterRepositoryImpl implements LetterRepository {

    /**
     * Spring Data JPA repository providing all low-level entity operations
     * against the {@code letters} table, including JSONB persistence for
     * snapshot columns.
     */
    private final LetterJpaRepository jpaRepository;

    /**
     * Mapper used to convert between {@code LetterEntity} and the
     * {@link Letter} domain model in both directions.
     */
    private final LetterMapper letterMapper;

    /**
     * JPA repository for {@code UserEntity}, used to resolve the managed user
     * reference required when building a {@code LetterEntity} for save
     * operations.
     */
    private final UserJpaRepository userJpaRepository;

    /**
     * Constructs a new {@code LetterRepositoryImpl} with the required
     * collaborators injected by Spring.
     *
     * @param jpaRepository     the Spring Data JPA repository for
     *                          {@code LetterEntity}; must not be {@code null}
     * @param letterMapper      the mapper responsible for converting between
     *                          entity and domain model; must not be {@code null}
     * @param userJpaRepository the JPA repository used to resolve the owning
     *                          {@code UserEntity} on save; must not be
     *                          {@code null}
     */
    public LetterRepositoryImpl(LetterJpaRepository jpaRepository,
                                LetterMapper letterMapper,
                                UserJpaRepository userJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.letterMapper = letterMapper;
        this.userJpaRepository = userJpaRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link LetterJpaRepository#findByUserId(UUID)} and maps
     * each resulting {@code LetterEntity} to a {@link Letter} domain model
     * using {@link LetterMapper#toModel(LetterEntity)}.</p>
     *
     * @param userId the UUID of the owning user; must not be {@code null}
     * @return list of letters belonging to the specified user; never
     *         {@code null}, but may be empty
     */
    @Override
    public List<Letter> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(letterMapper::toModel)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link LetterJpaRepository#findById(Object)} and maps
     * the resulting entity (if present) to a {@link Letter} domain model.</p>
     *
     * @param id the UUID of the letter to find; must not be {@code null}
     * @return an {@link Optional} containing the letter if found, otherwise
     *         empty
     */
    @Override
    public Optional<Letter> findById(UUID id) {
        return jpaRepository.findById(id).map(letterMapper::toModel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the owning {@code UserEntity} from {@code userJpaRepository}
     * using the supplied {@code userId}, converts the domain model to a
     * {@code LetterEntity} via the mapper, persists it via
     * {@link LetterJpaRepository#save(Object)}, and maps the saved entity back
     * to a domain model before returning.  The JSONB serialisation of the
     * snapshot objects is handled transparently by the Hibernate type
     * mapping.</p>
     *
     * @param letter the domain model to persist; must not be {@code null}
     * @param userId the UUID of the owning user; must correspond to an existing
     *               user record
     * @return the persisted {@link Letter} with any database-generated values
     *         populated
     * @throws IllegalStateException if no user with the given {@code userId}
     *                               exists in the database
     */
    @Override
    public Letter save(Letter letter, UUID userId) {
        var userEntity = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + userId));
        var entity = letterMapper.toEntity(letter, userEntity);
        return letterMapper.toModel(jpaRepository.save(entity));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates directly to
     * {@link LetterJpaRepository#deleteById(Object)}.  If no letter with the
     * given id exists the call returns silently.</p>
     *
     * @param id the UUID of the letter to delete; must not be {@code null}
     */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
