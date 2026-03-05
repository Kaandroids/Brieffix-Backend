package com.briefix.contact.model;

import com.briefix.profile.model.PartyType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable domain model representing a contact in the Briefix application.
 *
 * <p>A {@code Contact} is a person or organisation stored in a user's address book
 * that can later be used as the recipient of a generated letter.  This record
 * acts as the canonical in-memory representation of a contact and is the type
 * exchanged between the service and repository layers.  It is distinct from
 * {@code ContactEntity} (the JPA persistence model) and {@code ContactDto}
 * (the API response model).</p>
 *
 * <p>Contacts are always scoped to a single user identified by {@code userId}.
 * The {@code type} discriminator ({@link PartyType}) controls which subset of
 * fields is meaningful: individual-specific fields (first name, last name,
 * salutation) are used for private persons, while company-specific fields
 * (company name, department, contact person) are used for organisations.</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param id                      unique identifier of the contact; {@code null}
 *                                for a contact that has not yet been persisted
 * @param userId                  identifier of the owning user; used for
 *                                ownership validation and scoped queries
 * @param type                    discriminator that indicates whether this
 *                                contact represents an individual or a company
 *                                ({@link PartyType})
 * @param companyName             name of the company; relevant when
 *                                {@code type} is {@code COMPANY}
 * @param contactPerson           name of the primary contact person within the
 *                                company; relevant when {@code type} is
 *                                {@code COMPANY}
 * @param contactPersonSalutation formal salutation for the contact person
 *                                within a company (e.g. "Herr", "Frau")
 * @param department              department or organisational unit within the
 *                                company
 * @param firstName               given name of the contact; relevant when
 *                                {@code type} is {@code INDIVIDUAL}
 * @param lastName                family name of the contact; relevant when
 *                                {@code type} is {@code INDIVIDUAL}
 * @param salutation              formal salutation for the contact
 *                                (e.g. "Herr", "Frau", "Divers")
 * @param street                  street name of the contact's mailing address
 * @param streetNumber            house or building number of the contact's
 *                                mailing address
 * @param postalCode              postal/ZIP code of the contact's mailing address
 * @param city                    city of the contact's mailing address
 * @param country                 country of the contact's mailing address
 * @param email                   electronic mail address of the contact
 * @param phone                   telephone number of the contact
 * @param createdAt               timestamp recording when the contact was first
 *                                persisted; set automatically on insert and
 *                                never modified thereafter
 */
public record Contact(
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
