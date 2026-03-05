package com.briefix.contact.controller;

import com.briefix.contact.dto.ContactDto;
import com.briefix.contact.dto.CreateContactRequest;
import com.briefix.contact.dto.UpdateContactRequest;
import com.briefix.contact.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing CRUD endpoints for the contact resource.
 *
 * <p>{@code ContactController} handles all HTTP requests under the base path
 * {@code /api/v1/contacts} and delegates every operation to
 * {@link ContactService}.  It is responsible for:</p>
 * <ul>
 *   <li>Deserialising and validating inbound JSON request bodies.</li>
 *   <li>Extracting the authenticated principal's email address from the
 *       {@link Authentication} object and forwarding it to the service layer,
 *       which uses it for user resolution and ownership checks.</li>
 *   <li>Setting appropriate HTTP response status codes.</li>
 * </ul>
 *
 * <p>All endpoints require an authenticated user.  Authentication is enforced
 * by the application's Spring Security configuration; this controller does not
 * perform any security checks itself.</p>
 *
 * <p><strong>Thread safety:</strong> this controller is stateless; it is safe
 * to use as a singleton-scoped Spring bean.</p>
 */
@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {

    /**
     * Business-logic delegate that handles all contact operations, including
     * user resolution, ownership validation, and persistence.
     */
    private final ContactService contactService;

    /**
     * Constructs a new {@code ContactController} with the required service
     * dependency injected by Spring.
     *
     * @param contactService the contact service that provides business logic;
     *                       must not be {@code null}
     */
    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Retrieves all contacts that belong to the currently authenticated user.
     *
     * <p>HTTP: {@code GET /api/v1/contacts}</p>
     *
     * @param authentication the Spring Security authentication object for the
     *                       current request; used to extract the caller's email
     *                       address via {@link Authentication#getName()}
     * @return a list of {@link ContactDto} objects representing the user's
     *         contacts; empty list if none exist
     */
    @GetMapping
    public List<ContactDto> getMyContacts(Authentication authentication) {
        return contactService.getMyContacts(authentication.getName());
    }

    /**
     * Creates a new contact for the currently authenticated user.
     *
     * <p>HTTP: {@code POST /api/v1/contacts}</p>
     * <p>Response status: {@code 201 Created}</p>
     *
     * @param req            the validated request body containing the contact
     *                       data to persist; must not be {@code null} and must
     *                       pass bean-validation constraints
     * @param authentication the Spring Security authentication object for the
     *                       current request; provides the caller's email address
     * @return the {@link ContactDto} representing the newly created contact,
     *         including the generated id and creation timestamp
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactDto create(@Valid @RequestBody CreateContactRequest req,
                             Authentication authentication) {
        return contactService.create(req, authentication.getName());
    }

    /**
     * Retrieves a single contact by its unique identifier.
     *
     * <p>HTTP: {@code GET /api/v1/contacts/{id}}</p>
     * <p>Returns {@code 403 Forbidden} if the authenticated user does not own
     * the requested contact, and {@code 404 Not Found} if no contact with the
     * given id exists.</p>
     *
     * @param id             the UUID of the contact to retrieve, extracted from
     *                       the URL path variable
     * @param authentication the Spring Security authentication object for the
     *                       current request; provides the caller's email address
     *                       for ownership verification
     * @return the {@link ContactDto} for the requested contact
     */
    @GetMapping("/{id}")
    public ContactDto getById(@PathVariable UUID id, Authentication authentication) {
        return contactService.getById(id, authentication.getName());
    }

    /**
     * Replaces all mutable fields of an existing contact with the supplied data.
     *
     * <p>HTTP: {@code PUT /api/v1/contacts/{id}}</p>
     * <p>Returns {@code 403 Forbidden} if the authenticated user does not own
     * the contact, and {@code 404 Not Found} if the contact does not exist.</p>
     *
     * @param id             the UUID of the contact to update, extracted from
     *                       the URL path variable
     * @param req            the validated request body containing the updated
     *                       contact data; must pass bean-validation constraints
     * @param authentication the Spring Security authentication object for the
     *                       current request; provides the caller's email address
     *                       for ownership verification
     * @return the {@link ContactDto} representing the contact after the update
     */
    @PutMapping("/{id}")
    public ContactDto update(@PathVariable UUID id,
                             @Valid @RequestBody UpdateContactRequest req,
                             Authentication authentication) {
        return contactService.update(id, req, authentication.getName());
    }

    /**
     * Permanently deletes the contact identified by the given id.
     *
     * <p>HTTP: {@code DELETE /api/v1/contacts/{id}}</p>
     * <p>Response status: {@code 204 No Content}</p>
     * <p>Returns {@code 403 Forbidden} if the authenticated user does not own
     * the contact, and {@code 404 Not Found} if the contact does not exist.</p>
     *
     * @param id             the UUID of the contact to delete, extracted from
     *                       the URL path variable
     * @param authentication the Spring Security authentication object for the
     *                       current request; provides the caller's email address
     *                       for ownership verification
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        contactService.delete(id, authentication.getName());
    }
}
