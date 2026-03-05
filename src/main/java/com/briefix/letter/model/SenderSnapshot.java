package com.briefix.letter.model;

/**
 * Immutable snapshot of the sender's profile data captured at the moment a
 * letter is generated.
 *
 * <p>A {@code SenderSnapshot} preserves the exact state of the user's selected
 * profile at letter-creation time.  Because it is stored as a JSONB column
 * inside the letter record, the letter remains historically accurate even if
 * the originating profile is later edited or deleted.  This design avoids any
 * referential dependency between a persisted letter and a mutable profile.</p>
 *
 * <p>The snapshot is produced by
 * {@code LetterServiceImpl#buildSenderSnapshot(Profile)} and includes all
 * fields that may appear in the header, footer, or address block of a
 * generated letter template.</p>
 *
 * <p><strong>Thread safety:</strong> instances are immutable Java records and
 * therefore inherently thread-safe.</p>
 *
 * @param type             the party type of the profile as a string (e.g.
 *                         {@code "INDIVIDUAL"} or {@code "COMPANY"}); used by
 *                         letter templates to conditionally render fields
 * @param profileLabel     the user-defined display label for the profile
 * @param salutation       formal salutation of the sender
 *                         (e.g. "Herr", "Frau")
 * @param title            academic or professional title of the sender
 *                         (e.g. "Dr.", "Prof.")
 * @param firstName        given name of the sender individual
 * @param lastName         family name of the sender individual
 * @param companyName      legal name of the sender's company; populated for
 *                         company profiles
 * @param department       department or division of the sender within their
 *                         organisation
 * @param street           street name of the sender's business address
 * @param streetNumber     house or building number of the sender's business
 *                         address
 * @param postalCode       postal or ZIP code of the sender's business address
 * @param city             city of the sender's business address
 * @param country          country of the sender's business address
 * @param phone            primary telephone number of the sender
 * @param fax              fax number of the sender; may be {@code null}
 * @param email            electronic mail address of the sender
 * @param website          public website URL of the sender or their company;
 *                         may be {@code null}
 * @param vatId            value-added tax identification number of the sender's
 *                         company; required for some legal letter formats;
 *                         may be {@code null}
 * @param taxNumber        tax number of the sender's company; may be
 *                         {@code null}
 * @param managingDirector name(s) of the managing director(s) of the sender's
 *                         company; used in commercial letter footers; may be
 *                         {@code null}
 * @param registerCourt    the court in which the sender's company is registered
 *                         (e.g. "Amtsgericht München"); may be {@code null}
 * @param registerNumber   the commercial register number of the sender's
 *                         company (e.g. "HRB 123456"); may be {@code null}
 * @param iban             international bank account number of the sender's
 *                         bank account; may be {@code null}
 * @param bic              bank identifier code (SWIFT code) of the sender's
 *                         bank; may be {@code null}
 * @param bankName         name of the sender's bank; may be {@code null}
 * @param contactPerson    name of the primary contact person at the sender's
 *                         company; may be {@code null}
 */
public record SenderSnapshot(
        String type,
        String profileLabel,
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
        String phone,
        String fax,
        String email,
        String website,
        String vatId,
        String taxNumber,
        String managingDirector,
        String registerCourt,
        String registerNumber,
        String iban,
        String bic,
        String bankName,
        String contactPerson
) {}
