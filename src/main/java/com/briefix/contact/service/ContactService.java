package com.briefix.contact.service;

import com.briefix.contact.dto.ContactDto;
import com.briefix.contact.dto.CreateContactRequest;
import com.briefix.contact.dto.UpdateContactRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining the business operations available for managing
 * contacts in the Briefix application.
 *
 * <p>{@code ContactService} is the primary entry point for all contact-related
 * business logic.  It enforces ownership rules — every operation that accesses
 * or modifies a specific contact verifies that the authenticated user
 * (identified by their email address) is the owner of that contact.  Violations
 * result in an {@link org.springframework.security.access.AccessDeniedException}.</p>
 *
 * <p>All methods accept the caller's email address (as extracted from the JWT
 * or security context) rather than an internal user ID, so that the controller
 * layer does not need to perform any user resolution itself.</p>
 *
 * <p>Implementations are expected to be Spring {@code @Service} components and
 * should handle their own user-resolution and access-control logic.</p>
 *
 * <p><strong>Thread safety:</strong> implementations must be thread-safe.
 * The provided {@code ContactServiceImpl} is stateless and therefore
 * thread-safe.</p>
 */
public interface ContactService {

    /**
     * Retrieves all contacts belonging to the authenticated user.
     *
     * @param email the email address of the authenticated user, as extracted
     *              from the security context; must not be {@code null}
     * @return an unmodifiable list of {@link ContactDto} objects owned by the
     *         user; empty list if the user has no contacts
     * @throws com.briefix.user.exception.UserNotFoundException if no user with
     *         the given email exists
     */
    List<ContactDto> getMyContacts(String email);

    /**
     * Retrieves a single contact by its unique identifier, enforcing ownership.
     *
     * @param id    the UUID of the contact to retrieve; must not be {@code null}
     * @param email the email address of the authenticated user; used to verify
     *              that the caller owns the requested contact
     * @return the {@link ContactDto} for the requested contact
     * @throws com.briefix.contact.exception.ContactNotFoundException if no
     *         contact with the given id exists
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         authenticated user is not the owner of the contact
     */
    ContactDto getById(UUID id, String email);

    /**
     * Creates a new contact for the authenticated user and persists it.
     *
     * @param req   the validated request payload containing the contact data;
     *              must not be {@code null}
     * @param email the email address of the authenticated user who will own the
     *              new contact; must not be {@code null}
     * @return the {@link ContactDto} representing the newly created contact,
     *         including the database-generated id and creation timestamp
     * @throws com.briefix.user.exception.UserNotFoundException if no user with
     *         the given email exists
     */
    ContactDto create(CreateContactRequest req, String email);

    /**
     * Replaces all mutable fields of an existing contact with the values
     * supplied in the request, enforcing ownership.
     *
     * <p>The contact's id and creation timestamp are preserved unchanged.  All
     * other fields are overwritten with the values from {@code req}.</p>
     *
     * @param id    the UUID of the contact to update; must not be {@code null}
     * @param req   the validated request payload containing the updated contact
     *              data; must not be {@code null}
     * @param email the email address of the authenticated user; used to verify
     *              ownership before applying the update
     * @return the {@link ContactDto} representing the contact after the update
     * @throws com.briefix.contact.exception.ContactNotFoundException if no
     *         contact with the given id exists
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         authenticated user is not the owner of the contact
     */
    ContactDto update(UUID id, UpdateContactRequest req, String email);

    /**
     * Permanently deletes the contact identified by the given id, enforcing
     * ownership.
     *
     * @param id    the UUID of the contact to delete; must not be {@code null}
     * @param email the email address of the authenticated user; used to verify
     *              ownership before deletion
     * @throws com.briefix.contact.exception.ContactNotFoundException if no
     *         contact with the given id exists
     * @throws org.springframework.security.access.AccessDeniedException if the
     *         authenticated user is not the owner of the contact
     */
    void delete(UUID id, String email);
}
