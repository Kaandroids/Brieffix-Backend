package com.briefix.contact.dto;

import com.briefix.profile.model.PartyType;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound DTO carrying the data required to update an existing contact.
 *
 * <p>{@code UpdateContactRequest} is deserialised from the JSON body of a
 * {@code PUT /api/v1/contacts/{id}} request.  The payload is structurally
 * identical to {@link CreateContactRequest}: every field of the contact is
 * replaced in full (full replacement / PUT semantics) rather than partially
 * patched.  Bean-validation annotations enforce that the mandatory {@code type}
 * field is present.</p>
 *
 * <p>The owning user and the target contact's identifier are not part of this
 * record; they are obtained from the URL path variable and the authenticated
 * principal in the controller layer.</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param type                    party type discriminator identifying whether
 *                                the contact is an individual or a company;
 *                                required — must not be {@code null}
 * @param companyName             updated name of the company; relevant when
 *                                {@code type} is {@code COMPANY}
 * @param contactPerson           updated full name of the primary contact
 *                                person at the company
 * @param contactPersonSalutation updated formal salutation for the company
 *                                contact person
 * @param department              updated department or organisational unit
 *                                within the company
 * @param firstName               updated given name of an individual contact
 * @param lastName                updated family name of an individual contact
 * @param salutation              updated formal salutation used when addressing
 *                                the contact in letters
 * @param street                  updated street name of the contact's postal
 *                                address
 * @param streetNumber            updated house or building number of the
 *                                contact's postal address
 * @param postalCode              updated postal or ZIP code of the contact's
 *                                mailing address
 * @param city                    updated city of the contact's mailing address
 * @param country                 updated country of the contact's mailing
 *                                address
 * @param email                   updated electronic mail address of the contact
 * @param phone                   updated telephone number of the contact
 */
public record UpdateContactRequest(
        @NotNull(message = "Contact type is required")
        PartyType type,

        String companyName,
        String contactPerson,
        String contactPersonSalutation,
        String department,
        String firstName,
        String lastName,
        String salutation,
        String street,
        String streetNumber,
        String postalCode,
        String city,
        String country,
        String email,
        String phone
) {}
