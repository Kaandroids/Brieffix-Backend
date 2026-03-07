package com.briefix.letter.dto;

import com.briefix.letter.model.LetterTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Inbound DTO carrying all data required to generate or preview a letter.
 *
 * <p>{@code GenerateLetterRequest} is deserialised from the JSON body of both
 * the {@code POST /api/v1/letters} (create) and
 * {@code POST /api/v1/letters/preview} (preview) endpoints.  Bean-validation
 * annotations enforce mandatory fields before the request reaches the service
 * layer.</p>
 *
 * <p>The recipient can be specified in two mutually exclusive ways:</p>
 * <ol>
 *   <li><strong>By contact reference:</strong> provide {@code contactId} with
 *       the UUID of an existing contact in the user's address book.  The
 *       service will resolve and snapshot the contact's current data.  All
 *       {@code recipient*} fields are ignored in this case.</li>
 *   <li><strong>Manually:</strong> leave {@code contactId} as {@code null} and
 *       populate the {@code recipient*} fields directly.  The service will
 *       build a {@link com.briefix.letter.model.RecipientSnapshot} from these
 *       values.</li>
 * </ol>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param profileId                the UUID of the sender profile to use for
 *                                 this letter; must not be {@code null}.  The
 *                                 profile must belong to the authenticated user.
 * @param contactId                optional UUID of a saved contact to use as
 *                                 the recipient; when provided, all manual
 *                                 {@code recipient*} fields are ignored
 * @param recipientSalutation      formal salutation of the manual recipient
 *                                 (e.g. "Herr", "Frau"); ignored when
 *                                 {@code contactId} is set
 * @param recipientFirstName       given name of the manual recipient; ignored
 *                                 when {@code contactId} is set
 * @param recipientLastName        family name of the manual recipient; ignored
 *                                 when {@code contactId} is set
 * @param recipientCompany         company name of the manual recipient; ignored
 *                                 when {@code contactId} is set
 * @param recipientContactPerson   contact person at the recipient's company;
 *                                 ignored when {@code contactId} is set
 * @param recipientStreet          street name of the manual recipient's postal
 *                                 address; ignored when {@code contactId} is set
 * @param recipientStreetNumber    house or building number of the manual
 *                                 recipient's postal address; ignored when
 *                                 {@code contactId} is set
 * @param recipientPostalCode      postal code of the manual recipient's mailing
 *                                 address; ignored when {@code contactId} is set
 * @param recipientCity            city of the manual recipient's mailing
 *                                 address; ignored when {@code contactId} is set
 * @param recipientCountry         country of the manual recipient's mailing
 *                                 address; ignored when {@code contactId} is set
 * @param recipientEmail           email address of the manual recipient; ignored
 *                                 when {@code contactId} is set
 * @param recipientPhone           phone number of the manual recipient; ignored
 *                                 when {@code contactId} is set
 * @param title                    subject or title of the letter; must not be
 *                                 blank; used in the letter header and as the
 *                                 PDF download file name
 * @param body                     full text body of the letter; must not be
 *                                 blank
 * @param letterDate               the date to print on the letter; when
 *                                 {@code null} the service defaults to the
 *                                 current date at generation time
 * @param template                 the visual design template to apply when
 *                                 rendering the PDF; must not be {@code null};
 *                                 premium templates require a PREMIUM user plan
 */
public record GenerateLetterRequest(
        @NotNull UUID profileId,

        // Recipient — either a saved contact or manually entered fields
        UUID contactId,
        String recipientEntityType,
        String recipientSalutation,
        String recipientFirstName,
        String recipientLastName,
        String recipientCompany,
        String recipientContactPerson,
        String recipientDepartment,
        String recipientStreet,
        String recipientStreetNumber,
        String recipientPostalCode,
        String recipientCity,
        String recipientCountry,
        String recipientEmail,
        String recipientPhone,

        // Letter content
        @NotBlank String title,
        @NotBlank String body,
        LocalDate letterDate,

        // Template selection
        @NotNull LetterTemplate template
) {}
