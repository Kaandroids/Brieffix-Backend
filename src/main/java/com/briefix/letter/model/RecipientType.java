package com.briefix.letter.model;

/**
 * Enumeration indicating the origin of a letter's recipient data.
 *
 * <p>When generating a letter the caller can supply the recipient in one of
 * two ways:</p>
 * <ul>
 *   <li>{@link #CONTACT} – the recipient's data is sourced from an existing
 *       contact record stored in the user's address book.  The service
 *       resolves the full contact details by id and snapshots them into the
 *       letter at creation time.</li>
 *   <li>{@link #MANUAL} – the recipient's data is entered directly in the
 *       {@link com.briefix.letter.dto.GenerateLetterRequest} without referencing
 *       a saved contact.  This mode is used for one-off recipients that the
 *       user does not wish to save as a contact.</li>
 * </ul>
 *
 * <p>The {@code RecipientType} is stored inside the
 * {@link RecipientSnapshot} JSONB column so that the origin of the recipient
 * data is auditable even after the originating contact record may have been
 * deleted or modified.</p>
 *
 * <p><strong>Thread safety:</strong> as a Java enum, all constants are
 * singletons and inherently thread-safe.</p>
 */
public enum RecipientType {

    /**
     * Recipient data was sourced from a saved contact in the user's address
     * book.  The contact's data was snapshotted at letter-creation time.
     */
    CONTACT,

    /**
     * Recipient data was entered manually by the user at letter-generation
     * time and is not linked to any saved contact record.
     */
    MANUAL
}
