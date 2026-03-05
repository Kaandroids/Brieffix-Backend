package com.briefix.contact.repository;

import com.briefix.contact.mapper.ContactMapper;
import com.briefix.contact.model.Contact;
import com.briefix.user.repository.UserJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Concrete implementation of {@link ContactRepository} that delegates all
 * persistence work to the Spring Data JPA layer.
 *
 * <p>{@code ContactRepositoryImpl} is the adapter that bridges the
 * domain-model world used by the service layer and the JPA-entity world used
 * by the database layer.  It performs the following responsibilities:</p>
 * <ol>
 *   <li>Invokes {@link ContactJpaRepository} for all CRUD operations.</li>
 *   <li>Uses {@link ContactMapper} to convert between {@code ContactEntity}
 *       and the {@link Contact} domain model.</li>
 *   <li>Resolves the required {@code UserEntity} association by querying
 *       {@code UserJpaRepository} when saving a contact.</li>
 * </ol>
 *
 * <p><strong>Thread safety:</strong> this class is stateless beyond its
 * injected dependencies, all of which are thread-safe Spring beans.  It is
 * safe to use as a singleton-scoped Spring bean.</p>
 */
@Repository
public class ContactRepositoryImpl implements ContactRepository {

    /**
     * Spring Data JPA repository that provides all low-level entity operations
     * against the {@code contacts} table.
     */
    private final ContactJpaRepository jpaRepository;

    /**
     * Mapper used to convert between {@code ContactEntity} and the
     * {@link Contact} domain model in both directions.
     */
    private final ContactMapper contactMapper;

    /**
     * JPA repository for {@code UserEntity}, used to resolve the managed user
     * reference required when building a {@code ContactEntity} for save
     * operations.
     */
    private final UserJpaRepository userJpaRepository;

    /**
     * Constructs a new {@code ContactRepositoryImpl} with the required
     * collaborators injected by Spring.
     *
     * @param jpaRepository     the Spring Data JPA repository for
     *                          {@code ContactEntity}; must not be {@code null}
     * @param contactMapper     the mapper responsible for converting between
     *                          entity and domain model; must not be {@code null}
     * @param userJpaRepository the JPA repository used to resolve the owning
     *                          {@code UserEntity} on save; must not be
     *                          {@code null}
     */
    public ContactRepositoryImpl(ContactJpaRepository jpaRepository,
                                  ContactMapper contactMapper,
                                  UserJpaRepository userJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.contactMapper = contactMapper;
        this.userJpaRepository = userJpaRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link ContactJpaRepository#findByUserId(UUID)} and maps
     * each resulting {@code ContactEntity} to a {@link Contact} domain model
     * using {@link ContactMapper#toModel(ContactEntity)}.</p>
     *
     * @param userId the UUID of the owning user; must not be {@code null}
     * @return list of contacts belonging to the specified user; never
     *         {@code null}, but may be empty
     */
    @Override
    public List<Contact> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(contactMapper::toModel)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link ContactJpaRepository#findById(Object)} and maps
     * the resulting entity (if present) to a {@link Contact} domain model.</p>
     *
     * @param id the UUID of the contact to find; must not be {@code null}
     * @return an {@link Optional} containing the contact if found, otherwise
     *         empty
     */
    @Override
    public Optional<Contact> findById(UUID id) {
        return jpaRepository.findById(id).map(contactMapper::toModel);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the owning {@code UserEntity} from {@code userJpaRepository}
     * using the supplied {@code userId}, converts the domain model to a
     * {@code ContactEntity} via the mapper, persists it via
     * {@link ContactJpaRepository#save(Object)}, and maps the saved entity back
     * to a domain model before returning.</p>
     *
     * @param contact the domain model to persist; must not be {@code null}
     * @param userId  the UUID of the owning user; must correspond to an
     *                existing user record
     * @return the persisted {@link Contact} with any database-generated values
     *         populated
     * @throws IllegalArgumentException if no user with the given {@code userId}
     *                                  exists in the database
     */
    @Override
    public Contact save(Contact contact, UUID userId) {
        var user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        var entity = contactMapper.toEntity(contact, user);
        var saved = jpaRepository.save(entity);
        return contactMapper.toModel(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates directly to
     * {@link ContactJpaRepository#deleteById(Object)}.  If no contact with the
     * given id exists the call returns silently.</p>
     *
     * @param id the UUID of the contact to delete; must not be {@code null}
     */
    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
