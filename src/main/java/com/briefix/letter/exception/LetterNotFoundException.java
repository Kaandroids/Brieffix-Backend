package com.briefix.letter.exception;

import java.util.UUID;

/**
 * Unchecked exception thrown when a requested letter cannot be found in the
 * system.
 *
 * <p>{@code LetterNotFoundException} is raised by the service layer when a
 * letter lookup by id yields no result.  It is intended to be caught by a
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
public class LetterNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code LetterNotFoundException} with a detail message
     * that includes the id of the letter that could not be found.
     *
     * @param id the UUID of the letter that was not found; must not be
     *           {@code null}
     */
    public LetterNotFoundException(UUID id) {
        super("Letter not found: " + id);
    }
}
