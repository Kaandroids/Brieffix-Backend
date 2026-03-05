package com.briefix.profile.model;

/**
 * Classifies the legal or organizational nature of the party represented by a {@link Profile}.
 *
 * <p>This enum drives which fields are relevant for a given profile and how the profile
 * is rendered in generated documents such as invoices or contracts. For example, an
 * {@link #INDIVIDUAL} party typically requires {@code firstName} and {@code lastName},
 * while an {@link #ORGANIZATION} party requires {@code companyName},
 * {@code managingDirector}, and commercial register details.</p>
 *
 * <p>The value is persisted as a string in the {@code profiles} table via
 * {@code @Enumerated(EnumType.STRING)} for schema readability and resilience
 * against enum reordering.</p>
 *
 * <ul>
 *   <li>{@link #INDIVIDUAL} – A private person acting in their own capacity.</li>
 *   <li>{@link #ORGANIZATION} – A legal entity such as a company, association, or institution.</li>
 * </ul>
 *
 * <p>This enum is immutable and safe for concurrent access in all contexts.</p>
 */
public enum PartyType {

    /**
     * Represents a natural person acting in a private capacity.
     * Profiles of this type typically populate personal name fields ({@code firstName},
     * {@code lastName}, {@code salutation}, {@code title}) and omit company-specific fields.
     */
    INDIVIDUAL,

    /**
     * Represents a legal entity such as a GmbH, AG, e.V., or other registered organization.
     * Profiles of this type typically populate {@code companyName}, {@code managingDirector},
     * {@code registerCourt}, {@code registerNumber}, and tax-related fields ({@code vatId},
     * {@code taxNumber}).
     */
    ORGANIZATION
}
