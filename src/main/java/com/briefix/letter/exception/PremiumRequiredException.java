package com.briefix.letter.exception;

/**
 * Unchecked exception thrown when a user attempts to use a premium
 * {@link com.briefix.letter.model.LetterTemplate} without holding the required
 * PREMIUM subscription plan.
 *
 * <p>{@code PremiumRequiredException} is raised by
 * {@code LetterServiceImpl#checkPlanAccess(LetterTemplate, com.briefix.user.model.UserPlan)}
 * when the requested template is one of the PREMIUM-tier options
 * ({@code PROFESSIONAL} or {@code ELEGANT}) and the authenticated user's plan
 * is not {@code PREMIUM}.  It is intended to be caught by a global exception
 * handler (e.g. an {@code @ControllerAdvice}) which maps it to an appropriate
 * HTTP error response (e.g. {@code 403 Forbidden} or {@code 402 Payment
 * Required}).</p>
 *
 * <p>Extending {@link RuntimeException} means callers are not required to
 * declare or handle this exception explicitly.</p>
 *
 * <p><strong>Thread safety:</strong> instances are effectively immutable after
 * construction and are therefore thread-safe.</p>
 */
public class PremiumRequiredException extends RuntimeException {

    /**
     * Constructs a new {@code PremiumRequiredException} with a detail message
     * identifying which template triggered the restriction.
     *
     * @param templateName the name of the premium template that the user
     *                     attempted to use (e.g. {@code "PROFESSIONAL"} or
     *                     {@code "ELEGANT"}); must not be {@code null}
     */
    public PremiumRequiredException(String templateName) {
        super("Template '" + templateName + "' requires a PREMIUM plan");
    }
}
