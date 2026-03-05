package com.briefix.user.model;

/**
 * Represents the subscription plan tier assigned to a Briefix user account.
 *
 * <p>The plan determines which features and resource quotas are available to a user.
 * It is stored as a string value in the database via {@code @Enumerated(EnumType.STRING)}
 * and defaults to {@link #STANDARD} upon account creation.</p>
 *
 * <p>Business logic that gates functionality by plan (e.g., maximum number of profiles,
 * export formats, or API access) should compare against this enum rather than raw strings.</p>
 *
 * <ul>
 *   <li>{@link #STANDARD} – Entry-level plan with baseline feature access.</li>
 *   <li>{@link #PREMIUM} – Elevated plan with access to advanced features and higher quotas.</li>
 * </ul>
 *
 * <p>This enum is not thread-safe to modify (as it is immutable by nature), but is
 * safe to read concurrently from any context.</p>
 */
public enum UserPlan {

    /**
     * The default subscription plan assigned to all newly registered users.
     * Provides access to core Briefix functionality with standard resource limits.
     */
    STANDARD,

    /**
     * An upgraded subscription plan that unlocks advanced features, higher usage quotas,
     * and priority support. Assigned after a successful plan upgrade transaction.
     */
    PREMIUM
}
