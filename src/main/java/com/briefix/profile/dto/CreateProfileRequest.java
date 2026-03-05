package com.briefix.profile.dto;

import com.briefix.profile.model.PartyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound request record for creating a new profile associated with the authenticated user.
 *
 * <p>This record captures all client-supplied fields required to create a
 * {@link com.briefix.profile.model.Profile} domain object. It is bound from the
 * HTTP request body by Spring MVC and validated via Jakarta Bean Validation before
 * reaching the service layer.</p>
 *
 * <p>The {@code userId} is intentionally excluded from this record. The owning user
 * is derived from the authenticated security context (via {@code Authentication.getName()})
 * in the service layer, preventing clients from creating profiles on behalf of other users.</p>
 *
 * <p>Optional fields (address, banking, registration details) may be {@code null} and
 * should be populated according to the chosen {@link #type()}: individual profiles
 * typically require personal name fields, while organization profiles require company
 * and registration details.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 *
 * @param profileLabel      A short, user-defined name for this profile (e.g., "My Company"). Required.
 * @param isDefault         Whether this profile should be set as the user's default. Defaults to {@code false}.
 * @param type              The party classification ({@link PartyType#INDIVIDUAL} or {@link PartyType#ORGANIZATION}). Required.
 * @param salutation        Optional salutation (e.g., "Herr", "Frau") for individual profiles.
 * @param title             Optional academic or professional title (e.g., "Dr.").
 * @param firstName         First name of the individual or primary contact person.
 * @param lastName          Last name of the individual or primary contact person.
 * @param companyName       Legal company name for organization profiles.
 * @param department        Optional department or division within the organization.
 * @param street            Street name component of the postal address.
 * @param streetNumber      House or building number of the postal address.
 * @param postalCode        Postal code of the address.
 * @param city              City of the address.
 * @param country           Country of the address. If {@code null}, defaults to {@code "Deutschland"} in the service layer.
 * @param vatId             VAT identification number (e.g., {@code DE123456789}).
 * @param taxNumber         German tax number (Steuernummer).
 * @param managingDirector  Name(s) of the managing director(s) for GmbH entities.
 * @param registerCourt     Commercial register court (Registergericht).
 * @param registerNumber    Commercial register entry number (e.g., "HRB 12345").
 * @param iban              International Bank Account Number for payment information on invoices.
 * @param bic               BIC/SWIFT code associated with the IBAN.
 * @param bankName          Name of the bank institution.
 * @param website           Optional URL of the party's public website.
 * @param phone             Contact phone number for documents.
 * @param fax               Optional fax number.
 * @param email             Business contact email address to appear on documents.
 * @param contactPerson     Designated contact person name for organization profiles.
 */
public record CreateProfileRequest(
        @NotBlank(message = "Profile label is required")
        String profileLabel,

        boolean isDefault,

        @NotNull(message = "Profile type is required")
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
        String contactPerson
) {}
