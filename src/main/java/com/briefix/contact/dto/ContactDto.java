package com.briefix.contact.dto;

import com.briefix.profile.model.PartyType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Data Transfer Object representing a contact returned to API consumers.
 *
 * <p>{@code ContactDto} is the read-facing representation of a {@code Contact}
 * that is serialised to JSON and returned in HTTP responses.  It carries the
 * full set of fields a client needs to display or further process a contact,
 * including metadata such as the owning user's identifier and the creation
 * timestamp.</p>
 *
 * <p>Instances are produced exclusively by {@code ContactMapper#toDto(Contact)}
 * and should never be created directly in service or controller code.</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param id                      unique identifier of the contact
 * @param userId                  identifier of the user who owns this contact;
 *                                included to allow client-side ownership checks
 * @param type                    party type discriminator ({@link PartyType}),
 *                                indicating individual or company
 * @param companyName             name of the company; populated for company
 *                                contacts
 * @param contactPerson           full name of the primary contact person at the
 *                                company
 * @param contactPersonSalutation formal salutation for the contact person
 *                                (e.g. "Herr", "Frau")
 * @param department              department or organisational unit of the
 *                                contact within their company
 * @param firstName               given name of the contact; populated for
 *                                individual contacts
 * @param lastName                family name of the contact; populated for
 *                                individual contacts
 * @param salutation              formal salutation used when addressing the
 *                                contact in letters
 * @param street                  street name of the contact's postal address
 * @param streetNumber            house or building number of the contact's
 *                                postal address
 * @param postalCode              postal or ZIP code of the contact's mailing
 *                                address
 * @param city                    city of the contact's mailing address
 * @param country                 country of the contact's mailing address
 * @param email                   electronic mail address of the contact
 * @param phone                   telephone number of the contact
 * @param createdAt               timestamp at which the contact was first
 *                                created in the system
 */
public record ContactDto(
        UUID id,
        UUID userId,
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
        String phone,
        LocalDateTime createdAt
) {}
