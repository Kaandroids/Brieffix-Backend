package com.briefix.common;

import com.briefix.auth.exception.EmailAlreadyRegisteredException;
import com.briefix.auth.exception.EmailNotVerifiedException;
import com.briefix.auth.exception.InvalidVerificationTokenException;
import com.briefix.contact.exception.ContactNotFoundException;
import com.briefix.letter.exception.LetterNotFoundException;
import com.briefix.letter.exception.PremiumRequiredException;
import com.briefix.profile.exception.ProfileNotFoundException;
import com.briefix.user.exception.UserNotFoundException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized exception handler that translates domain, infrastructure, and security
 * exceptions into RFC 7807 {@link ProblemDetail} HTTP responses.
 *
 * <p>Annotated with {@link RestControllerAdvice}, this class intercepts exceptions
 * thrown from any {@code @RestController} in the application and maps them to
 * structured HTTP error responses. Using the RFC 7807 {@link ProblemDetail} format
 * ensures that all error responses share a consistent shape with {@code status},
 * {@code title}, and {@code detail} fields.</p>
 *
 * <p>Exception-to-status mapping summary:
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} → {@code 400 Bad Request}</li>
 *   <li>{@link UserNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link ProfileNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link ContactNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link LetterNotFoundException} → {@code 404 Not Found}</li>
 *   <li>{@link EmailAlreadyRegisteredException} → {@code 409 Conflict}</li>
 *   <li>{@link BadCredentialsException} → {@code 401 Unauthorized}</li>
 *   <li>{@link JwtException} → {@code 401 Unauthorized}</li>
 *   <li>{@link AccessDeniedException} → {@code 403 Forbidden}</li>
 *   <li>{@link PremiumRequiredException} → {@code 402 Payment Required}</li>
 * </ul>
 * </p>
 *
 * <p>Any exception not explicitly handled here propagates to Spring Boot's default
 * error handling infrastructure, which returns a generic {@code 500 Internal Server Error}
 * response.</p>
 *
 * <p><b>Thread safety:</b> This class is a stateless Spring singleton. All handler
 * methods operate on method-local variables and are safe for concurrent use.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles bean validation failures originating from {@code @Valid}-annotated
     * request body parameters.
     *
     * <p>Collects all field-level constraint violations from the binding result and
     * concatenates them into a single comma-separated detail string of the form
     * {@code "fieldName: message, fieldName2: message2, ..."}. Returns HTTP 400.</p>
     *
     * @param ex the {@link MethodArgumentNotValidException} thrown by the Spring MVC
     *           validation infrastructure when one or more {@code @Valid} constraints fail
     * @return a {@link ProblemDetail} with status {@code 400} and a detail string
     *         listing all violated field constraints
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    /**
     * Handles lookup failures when a requested user account does not exist in the system.
     *
     * <p>Returns HTTP 404 Not Found with the exception's detail message, which typically
     * identifies the user by ID or email.</p>
     *
     * @param ex the {@link UserNotFoundException} thrown when no user matches the
     *           requested identifier
     * @return a {@link ProblemDetail} with status {@code 404} and the exception's message
     *         as the detail
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles duplicate email registration attempts where the requested email address is
     * already associated with an existing account.
     *
     * <p>Returns HTTP 409 Conflict to signal that the resource (the email address) already
     * exists, enabling clients to distinguish this case from a generic bad-request error.</p>
     *
     * @param ex the {@link EmailAlreadyRegisteredException} thrown by the registration
     *           service when the requested email is already in use
     * @return a {@link ProblemDetail} with status {@code 409} and the exception's message
     *         identifying the conflicting email address
     */
    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailTaken(EmailAlreadyRegisteredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles authentication failures caused by invalid email/password combinations.
     *
     * <p>Returns HTTP 401 Unauthorized with a generic message ({@code "Invalid email or password"})
     * rather than the exception's own message to avoid disclosing whether the email exists
     * in the system (user enumeration protection).</p>
     *
     * @param ex the {@link BadCredentialsException} thrown by Spring Security's
     *           {@link org.springframework.security.authentication.AuthenticationManager}
     *           when credentials do not match
     * @return a {@link ProblemDetail} with status {@code 401} and a generic credential
     *         failure message
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    /**
     * Handles JWT-related failures including malformed tokens, invalid signatures,
     * and expired tokens.
     *
     * <p>Returns HTTP 401 Unauthorized. This handler covers exceptions thrown by the
     * JJWT library (e.g., during token validation in {@link com.briefix.auth.service.AuthServiceImpl#refresh})
     * and by the service layer when a refresh token fails validation.</p>
     *
     * @param ex the {@link JwtException} (or any subclass) thrown when JWT parsing or
     *           validation fails
     * @return a {@link ProblemDetail} with status {@code 401} and the exception's
     *         message describing the specific JWT failure
     */
    @ExceptionHandler(JwtException.class)
    public ProblemDetail handleJwtException(JwtException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /**
     * Handles lookup failures when a requested user profile does not exist in the system.
     *
     * <p>Returns HTTP 404 Not Found with the exception's detail message, which typically
     * identifies the profile by its owner's user ID.</p>
     *
     * @param ex the {@link ProfileNotFoundException} thrown when no profile record is
     *           found for the requested user
     * @return a {@link ProblemDetail} with status {@code 404} and the exception's message
     *         as the detail
     */
    @ExceptionHandler(ProfileNotFoundException.class)
    public ProblemDetail handleProfileNotFound(ProfileNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles lookup failures when a requested contact does not exist or is not accessible
     * to the authenticated user.
     *
     * <p>Returns HTTP 404 Not Found with the exception's detail message.</p>
     *
     * @param ex the {@link ContactNotFoundException} thrown when no contact record matches
     *           the requested identifier within the current user's contact list
     * @return a {@link ProblemDetail} with status {@code 404} and the exception's message
     *         as the detail
     */
    @ExceptionHandler(ContactNotFoundException.class)
    public ProblemDetail handleContactNotFound(ContactNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles authorization failures when an authenticated user attempts to access a
     * resource that belongs to another user or requires elevated permissions.
     *
     * <p>Returns HTTP 403 Forbidden. This handler is triggered by Spring Security's
     * access-decision infrastructure when a method annotated with {@code @PreAuthorize}
     * or similar denies access.</p>
     *
     * @param ex the {@link AccessDeniedException} thrown by Spring Security's
     *           authorization infrastructure when the authenticated principal lacks
     *           the required permissions for the requested resource
     * @return a {@link ProblemDetail} with status {@code 403} and the exception's
     *         message as the detail
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /**
     * Handles lookup failures when a requested letter does not exist or is not accessible
     * to the authenticated user.
     *
     * <p>Returns HTTP 404 Not Found with the exception's detail message.</p>
     *
     * @param ex the {@link LetterNotFoundException} thrown when no letter record matches
     *           the requested identifier within the current user's letter collection
     * @return a {@link ProblemDetail} with status {@code 404} and the exception's message
     *         as the detail
     */
    @ExceptionHandler(LetterNotFoundException.class)
    public ProblemDetail handleLetterNotFound(LetterNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles attempts by a standard-plan user to access features that require a
     * premium subscription.
     *
     * <p>Returns HTTP 402 Payment Required to signal to the client that the requested
     * feature is gated behind a paid plan upgrade.</p>
     *
     * @param ex the {@link PremiumRequiredException} thrown when a non-premium user
     *           attempts to invoke a feature restricted to premium accounts
     * @return a {@link ProblemDetail} with status {@code 402} and the exception's message
     *         describing the premium-gated feature
     */
    @ExceptionHandler(PremiumRequiredException.class)
    public ProblemDetail handlePremiumRequired(PremiumRequiredException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ProblemDetail handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ProblemDetail handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
