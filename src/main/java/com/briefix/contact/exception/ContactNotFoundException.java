package com.briefix.contact.exception;

import java.util.UUID;

/**
 * Unchecked exception thrown when a requested contact cannot be found in the
 * system.
 *
 * <p>{@code ContactNotFoundException} is raised by the service layer when a
 * contact lookup by id yields no result.  It is intended to be caught by a
 * global exception handler (e.g. an {@code @ControllerAdvice}) which maps it
 * to an HTTP {@code 404 Not Found} response.</p>
 *
 * <p>Extending {@link RuntimeException} means callers are not required to
 * declare or handle this exception explicitly, keeping service and controller
 * method signatures clean.</p>
 *
 * <p><strong>Thread safety:</strong> instances are effectively immutable after
 * construction and are therefore thread-safe.</p>
 */
public class ContactNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code ContactNotFoundException} with a detail message
     * that includes the id of the contact that could not be found.
     *
     * @param id the UUID of the contact that was not found; must not be
     *           {@code null}
     */
    public ContactNotFoundException(UUID id) {
        super("Contact not found: " + id);
    }
}
