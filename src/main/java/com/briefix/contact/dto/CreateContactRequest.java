package com.briefix.contact.dto;

import com.briefix.profile.model.PartyType;
import jakarta.validation.constraints.NotNull;

/**
 * Inbound DTO carrying the data required to create a new contact.
 *
 * <p>{@code CreateContactRequest} is deserialised from the JSON body of a
 * {@code POST /api/v1/contacts} request.  Bean-validation annotations enforce
 * that mandatory fields are present before the request reaches the service
 * layer.  All other fields are optional and may be {@code null} depending on
 * the contact's {@link com.briefix.profile.model.PartyType}.</p>
 *
 * <p>The owning user is not part of this record; it is derived from the
 * authenticated principal in the controller and passed separately to the
 * service.</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param type                    party type discriminator that identifies
 *                                whether this contact is an individual or a
 *                                company; required — must not be {@code null}
 * @param companyName             name of the company; relevant when
 *                                {@code type} is {@code COMPANY}
 * @param contactPerson           full name of the primary contact person at the
 *                                company
 * @param contactPersonSalutation formal salutation for the company contact
 *                                person (e.g. "Herr", "Frau")
 * @param department              department or organisational unit within the
 *                                company
 * @param firstName               given name of an individual contact; relevant
 *                                when {@code type} is {@code INDIVIDUAL}
 * @param lastName                family name of an individual contact; relevant
 *                                when {@code type} is {@code INDIVIDUAL}
 * @param salutation              formal salutation for addressing the contact
 *                                in letters (e.g. "Herr", "Frau", "Divers")
 * @param street                  street name of the contact's postal address
 * @param streetNumber            house or building number of the contact's
 *                                postal address
 * @param postalCode              postal or ZIP code of the contact's mailing
 *                                address
 * @param city                    city of the contact's mailing address
 * @param country                 country of the contact's mailing address
 * @param email                   electronic mail address of the contact
 * @param phone                   telephone number of the contact
 */
public record CreateContactRequest(
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
