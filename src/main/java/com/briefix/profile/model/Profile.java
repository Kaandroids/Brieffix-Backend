package com.briefix.profile.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a named sender or recipient profile within the Briefix platform.
 *
 * <p>A profile encapsulates all the identity, address, banking, and contact information
 * associated with a party (either an {@link PartyType#INDIVIDUAL} or an
 * {@link PartyType#ORGANIZATION}) that appears on documents such as invoices, contracts,
 * or briefs. Each profile belongs to exactly one user account and users may maintain
 * multiple profiles to represent different business identities or client configurations.</p>
 *
 * <p>Profiles are owned by a {@link com.briefix.user.model.User} (referenced by
 * {@code userId}) and access to any profile is gated by ownership checks in the
 * service layer to prevent cross-user data leakage.</p>
 *
 * <p>Instances of this record are immutable by design. Any modification (such as
 * updating an address or marking a profile as default) requires constructing a new
 * instance rather than mutating an existing one.</p>
 *
 * <p>Thread-safety: Records in Java are inherently immutable and therefore safe
 * for concurrent access without additional synchronization.</p>
 *
 * @param id                Unique identifier of the profile (UUID v4). {@code null} before first persistence.
 * @param userId            The UUID of the owning {@link com.briefix.user.model.User}. Never {@code null}.
 * @param profileLabel      A short human-readable name for this profile (e.g., "My Company", "Freelance"). Never {@code null}.
 * @param isDefault         Whether this profile is pre-selected as the default in the UI and document generation flows.
 * @param type              The party classification ({@link PartyType#INDIVIDUAL} or {@link PartyType#ORGANIZATION}).
 * @param salutation        Optional salutation for individuals (e.g., "Herr", "Frau").
 * @param title             Optional academic or professional title (e.g., "Dr.", "Prof.").
 * @param firstName         First name of the contact person for individual profiles.
 * @param lastName          Last name of the contact person for individual profiles.
 * @param companyName       Legal company name for organization profiles.
 * @param department        Optional department or division within the organization.
 * @param street            Street name of the postal address.
 * @param streetNumber      House or building number of the postal address.
 * @param postalCode        Postal code (PLZ) of the address.
 * @param city              City name of the address.
 * @param country           Country of the address. Defaults to {@code "Deutschland"} when not specified.
 * @param vatId             VAT identification number (Umsatzsteuer-Identifikationsnummer), e.g., {@code DE123456789}.
 * @param taxNumber         German tax number (Steuernummer) issued by the local tax authority.
 * @param managingDirector  Name(s) of the managing director(s) ({@code Geschäftsführer}) for GmbH entities.
 * @param registerCourt     The commercial register court ({@code Registergericht}), e.g., "Amtsgericht München".
 * @param registerNumber    The commercial register entry number, e.g., "HRB 12345".
 * @param iban              International Bank Account Number for payment information on invoices.
 * @param bic               Bank Identifier Code (SWIFT) associated with the IBAN.
 * @param bankName          Human-readable name of the bank institution.
 * @param website           Optional URL of the party's website.
 * @param phone             Contact phone number for this profile.
 * @param fax               Optional fax number for this profile.
 * @param email             Contact email address displayed on documents (may differ from the user's login email).
 * @param contactPerson     Name of the designated contact person for organization profiles.
 * @param createdAt         Server-assigned timestamp of when this profile was first created.
 * @param hasLogo           {@code true} if a logo image has been uploaded for this profile.
 */
public record Profile(
        UUID id,
        UUID userId,
        String profileLabel,
        boolean isDefault,
        PartyType type,
        String salutation,
        String title,
        String firstName,
        String lastName,
        String companyName,
        String department,
        String street,
        String streetNumber,
        String postalCode,
        String city,
        String country,
        String vatId,
        String taxNumber,
        String managingDirector,
        String registerCourt,
        String registerNumber,
        String iban,
        String bic,
        String bankName,
        String website,
        String phone,
        String fax,
        String email,
        String contactPerson,
        LocalDateTime createdAt,
        boolean hasLogo
) {}
