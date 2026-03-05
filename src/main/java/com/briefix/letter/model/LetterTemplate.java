package com.briefix.letter.model;

/**
 * Enumeration of the visual design templates available for generating letters
 * in the Briefix application.
 *
 * <p>Each constant maps to a Thymeleaf HTML template under
 * {@code src/main/resources/templates/letters/}, where the template file name
 * is derived by lower-casing the constant name (e.g. {@code CLASSIC} maps to
 * {@code letters/classic.html}).  The {@code LetterServiceImpl} uses this
 * mapping when invoking the template engine.</p>
 *
 * <p>Templates are grouped into two access tiers:</p>
 * <ul>
 *   <li><strong>STANDARD</strong> – available to all users regardless of
 *       subscription plan.</li>
 *   <li><strong>PREMIUM</strong> – only accessible to users with the
 *       {@code PREMIUM} {@link com.briefix.user.model.UserPlan}.  Attempting to
 *       use a premium template without the required plan results in a
 *       {@link com.briefix.letter.exception.PremiumRequiredException}.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> as a Java enum, all constants are
 * singletons and inherently thread-safe.</p>
 */
public enum LetterTemplate {

    /**
     * Classic letter layout with a traditional, conservative design.
     * Available to all users (STANDARD tier).
     */
    CLASSIC,       // STANDARD

    /**
     * Modern letter layout with a clean, contemporary aesthetic.
     * Available to all users (STANDARD tier).
     */
    MODERN,        // STANDARD

    /**
     * Professional letter layout with an executive, polished appearance.
     * Requires a PREMIUM subscription plan.
     */
    PROFESSIONAL,  // PREMIUM

    /**
     * Elegant letter layout featuring refined typography and decorative
     * elements.  Requires a PREMIUM subscription plan.
     */
    ELEGANT        // PREMIUM
}
