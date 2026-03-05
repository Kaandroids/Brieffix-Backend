package com.briefix.profile.dto;

import com.briefix.profile.model.PartyType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing the full outbound view of a
 * {@link com.briefix.profile.model.Profile} as returned by the REST API.
 *
 * <p>This record is the canonical API representation of a profile, containing all
 * fields relevant to both the presentation layer (UI rendering) and document generation
 * (invoices, contracts, briefs). Unlike the domain model, this record is designed
 * for safe serialization and may be consumed by frontend clients or external services.</p>
 *
 * <p>Instances are produced exclusively by
 * {@link com.briefix.profile.mapper.ProfileMapper#toDto(com.briefix.profile.model.Profile)}
 * and must be the only profile-related type returned from controller and service methods.
 * Direct exposure of {@link com.briefix.profile.entity.ProfileEntity} or
 * {@link com.briefix.profile.model.Profile} outside the application boundary is prohibited.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 *
 * @param id                Unique identifier of this profile (UUID v4).
 * @param userId            UUID of the user account that owns this profile.
 * @param profileLabel      Short human-readable label identifying this profile to the user.
 * @param isDefault         Whether this profile is pre-selected as the user's default.
 * @param type              The party classification ({@link PartyType#INDIVIDUAL} or {@link PartyType#ORGANIZATION}).
 * @param salutation        Optional salutation for individual profiles (e.g., "Herr", "Frau").
 * @param title             Optional academic or professional title (e.g., "Dr.").
 * @param firstName         First name of the individual or primary contact person.
 * @param lastName          Last name of the individual or primary contact person.
 * @param companyName       Legal company name for organization profiles.
 * @param department        Optional department or division within the organization.
 * @param street            Street name of the postal address.
 * @param streetNumber      House or building number of the postal address.
 * @param postalCode        Postal code of the address.
 * @param city              City of the address.
 * @param country           Country of the address; defaults to {@code "Deutschland"}.
 * @param vatId             VAT identification number (e.g., {@code DE123456789}).
 * @param taxNumber         German tax number (Steuernummer) assigned by the local tax authority.
 * @param managingDirector  Name(s) of the managing director(s) for GmbH entities.
 * @param registerCourt     Commercial register court (Registergericht).
 * @param registerNumber    Commercial register entry number (e.g., "HRB 12345").
 * @param iban              International Bank Account Number for invoicing purposes.
 * @param bic               BIC/SWIFT code associated with the IBAN.
 * @param bankName          Human-readable name of the bank institution.
 * @param website           Optional URL of the party's website.
 * @param phone             Contact phone number for this profile.
 * @param fax               Optional fax number for this profile.
 * @param email             Business contact email address displayed on documents.
 * @param contactPerson     Name of the designated contact person for organization profiles.
 * @param createdAt         Server-assigned timestamp of when this profile was first created.
 */
public record ProfileDto(
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
        LocalDateTime createdAt
) {}
