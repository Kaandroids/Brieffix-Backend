package com.briefix.contact.service;

import com.briefix.contact.dto.ContactDto;
import com.briefix.contact.dto.CreateContactRequest;
import com.briefix.contact.dto.UpdateContactRequest;
import com.briefix.contact.exception.ContactNotFoundException;
import com.briefix.contact.mapper.ContactMapper;
import com.briefix.contact.model.Contact;
import com.briefix.contact.repository.ContactRepository;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link ContactService} providing the full
 * business logic for contact management in the Briefix application.
 *
 * <p>{@code ContactServiceImpl} orchestrates the following concerns:</p>
 * <ul>
 *   <li><strong>User resolution:</strong> translates an authenticated user's
 *       email address to their internal {@code User} domain model before
 *       delegating to the repository.</li>
 *   <li><strong>Ownership enforcement:</strong> every read and write operation
 *       on a specific contact verifies that the requesting user is the
 *       recorded owner.  Unauthorised access results in an
 *       {@link AccessDeniedException}.</li>
 *   <li><strong>Domain model construction:</strong> assembles {@link Contact}
 *       records from inbound DTOs before persisting them.</li>
 *   <li><strong>DTO mapping:</strong> converts persisted domain models into
 *       {@link ContactDto} objects before returning them to callers.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> this service is stateless; all mutable
 * state is held within the injected collaborators (which are themselves
 * thread-safe Spring singletons).  It is therefore safe to use as a
 * singleton-scoped Spring bean.</p>
 */
@Service
public class ContactServiceImpl implements ContactService {

    /**
     * Repository providing CRUD access to the contact domain model.
     * Abstracts away all JPA and database details from the service layer.
     */
    private final ContactRepository contactRepository;

    /**
     * Mapper used to convert between the {@link Contact} domain model and
     * {@link ContactDto} API representation.
     */
    private final ContactMapper contactMapper;

    /**
     * Repository used to resolve a user's domain model from their email
     * address, as provided by the authentication principal.
     */
    private final UserRepository userRepository;

    /**
     * Constructs a new {@code ContactServiceImpl} with its required
     * collaborators injected by the Spring container.
     *
     * @param contactRepository the contact repository for persistence
     *                          operations; must not be {@code null}
     * @param contactMapper     the mapper for domain-to-DTO conversion;
     *                          must not be {@code null}
     * @param userRepository    the user repository for resolving the
     *                          authenticated principal; must not be {@code null}
     */
    public ContactServiceImpl(ContactRepository contactRepository,
                               ContactMapper contactMapper,
                               UserRepository userRepository) {
        this.contactRepository = contactRepository;
        this.contactMapper = contactMapper;
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the user by email, then retrieves all contacts whose
     * {@code userId} matches the resolved user's id.  Each contact is mapped to
     * a {@link ContactDto} before being returned.</p>
     *
     * @param email the authenticated user's email address; must not be
     *              {@code null}
     * @return list of {@link ContactDto} objects for the user's contacts; never
     *         {@code null}, but may be empty
     * @throws UserNotFoundException if no user with the given email exists
     */
    @Override
    public List<ContactDto> getMyContacts(String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return contactRepository.findByUserId(user.id()).stream()
                .map(contactMapper::toDto)
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the contact by id, then delegates to
     * {@link #checkOwnership(Contact, String)} to verify that the requesting
     * user is the owner.  The contact is mapped to a DTO on success.</p>
     *
     * @param id    the UUID of the contact to retrieve; must not be {@code null}
     * @param email the authenticated user's email address; must not be
     *              {@code null}
     * @return the {@link ContactDto} for the requested contact
     * @throws ContactNotFoundException if no contact with the given id exists
     * @throws AccessDeniedException    if the authenticated user does not own
     *                                  the contact
     */
    @Override
    public ContactDto getById(UUID id, String email) {
        var contact = contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
        checkOwnership(contact, email);
        return contactMapper.toDto(contact);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Resolves the authenticated user, builds a new {@link Contact} domain
     * model (with a {@code null} id so that the repository performs an insert),
     * persists it, and returns the saved state as a {@link ContactDto}.</p>
     *
     * @param req   the validated create request; must not be {@code null}
     * @param email the authenticated user's email address; must not be
     *              {@code null}
     * @return the {@link ContactDto} representing the newly persisted contact
     * @throws UserNotFoundException if no user with the given email exists
     */
    @Override
    public ContactDto create(CreateContactRequest req, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        var contact = new Contact(
                null,
                user.id(),
                req.type(),
                req.companyName(),
                req.contactPerson(),
                req.contactPersonSalutation(),
                req.department(),
                req.firstName(),
                req.lastName(),
                req.salutation(),
                req.street(),
                req.streetNumber(),
                req.postalCode(),
                req.city(),
                req.country(),
                req.email(),
                req.phone(),
                null
        );
        return contactMapper.toDto(contactRepository.save(contact, user.id()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the existing contact, verifies ownership, then constructs an
     * updated {@link Contact} record that preserves the original id, userId,
     * and creation timestamp while replacing all other fields with the values
     * from {@code req}.  The updated record is persisted and returned as a
     * {@link ContactDto}.</p>
     *
     * @param id    the UUID of the contact to update; must not be {@code null}
     * @param req   the validated update request; must not be {@code null}
     * @param email the authenticated user's email address; must not be
     *              {@code null}
     * @return the {@link ContactDto} reflecting the updated contact state
     * @throws ContactNotFoundException if no contact with the given id exists
     * @throws AccessDeniedException    if the authenticated user does not own
     *                                  the contact
     */
    @Override
    public ContactDto update(UUID id, UpdateContactRequest req, String email) {
        var existing = contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
        checkOwnership(existing, email);
        var updated = new Contact(
                existing.id(),
                existing.userId(),
                req.type(),
                req.companyName(),
                req.contactPerson(),
                req.contactPersonSalutation(),
                req.department(),
                req.firstName(),
                req.lastName(),
                req.salutation(),
                req.street(),
                req.streetNumber(),
                req.postalCode(),
                req.city(),
                req.country(),
                req.email(),
                req.phone(),
                existing.createdAt()
        );
        return contactMapper.toDto(contactRepository.save(updated, existing.userId()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Loads the contact, verifies ownership, then delegates to the
     * repository to permanently remove the record.</p>
     *
     * @param id    the UUID of the contact to delete; must not be {@code null}
     * @param email the authenticated user's email address; must not be
     *              {@code null}
     * @throws ContactNotFoundException if no contact with the given id exists
     * @throws AccessDeniedException    if the authenticated user does not own
     *                                  the contact
     */
    @Override
    public void delete(UUID id, String email) {
        var contact = contactRepository.findById(id)
                .orElseThrow(() -> new ContactNotFoundException(id));
        checkOwnership(contact, email);
        contactRepository.deleteById(id);
    }

    /**
     * Verifies that the authenticated user (identified by {@code email}) is the
     * owner of the given contact.
     *
     * <p>The user is resolved from the repository and their id is compared
     * against the contact's {@code userId} field.  If they do not match an
     * {@link AccessDeniedException} is thrown with a message identifying the
     * contact.</p>
     *
     * @param contact the contact whose ownership is to be verified; must not be
     *                {@code null}
     * @param email   the email address of the authenticated user; must not be
     *                {@code null}
     * @throws UserNotFoundException  if no user with the given email exists
     * @throws AccessDeniedException if the resolved user is not the owner of
     *                               the contact
     */
    private void checkOwnership(Contact contact, String email) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        if (!contact.userId().equals(user.id())) {
            throw new AccessDeniedException("Access denied to contact: " + contact.id());
        }
    }
}
