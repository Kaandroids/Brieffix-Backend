package com.briefix.letter.model;

/**
 * Immutable snapshot of the recipient's data captured at the moment a letter
 * is generated.
 *
 * <p>A {@code RecipientSnapshot} freezes the recipient's address and contact
 * details at letter-creation time.  Storing this snapshot as a JSONB column
 * within the letter record ensures that the letter remains historically
 * accurate regardless of subsequent changes to — or deletion of — the
 * underlying contact record.  This is particularly important for audit and
 * legal purposes.</p>
 *
 * <p>Snapshots can originate from two sources, indicated by the {@code type}
 * field:</p>
 * <ul>
 *   <li>{@code "CONTACT"} – populated by
 *       {@code LetterServiceImpl#fromContact(Contact)} when the caller provides
 *       a {@code contactId} in the generation request.</li>
 *   <li>{@code null} / {@code "MANUAL"} – populated directly from the manual
 *       fields in {@link com.briefix.letter.dto.GenerateLetterRequest} when no
 *       contact id is supplied.</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param type          the origin of the recipient data; {@code "CONTACT"} when
 *                      sourced from a saved contact, or {@code null} when
 *                      entered manually
 * @param salutation    formal salutation for addressing the recipient in the
 *                      letter opening (e.g. "Herr", "Frau")
 * @param firstName     given name of the recipient individual; may be
 *                      {@code null} for company recipients
 * @param lastName      family name of the recipient individual; may be
 *                      {@code null} for company recipients
 * @param companyName   legal name of the recipient's company; populated for
 *                      company recipients; may be {@code null}
 * @param contactPerson full name of the primary contact person at the
 *                      recipient's company; may be {@code null}
 * @param street        street name of the recipient's mailing address
 * @param streetNumber  house or building number of the recipient's mailing
 *                      address
 * @param postalCode    postal or ZIP code of the recipient's mailing address
 * @param city          city of the recipient's mailing address
 * @param country       country of the recipient's mailing address
 * @param email         electronic mail address of the recipient; may be
 *                      {@code null}
 * @param phone         telephone number of the recipient; may be {@code null}
 */
public record RecipientSnapshot(
        String type,
        String salutation,
        String firstName,
        String lastName,
        String companyName,
        String contactPerson,
        String street,
        String streetNumber,
        String postalCode,
        String city,
        String country,
        String email,
        String phone
) {}
