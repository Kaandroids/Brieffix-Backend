package com.briefix.letter.entity;

import com.briefix.letter.model.LetterTemplate;
import com.briefix.letter.model.RecipientSnapshot;
import com.briefix.letter.model.SenderSnapshot;
import com.briefix.user.entity.UserEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity that maps to the {@code letters} database table.
 *
 * <p>{@code LetterEntity} is the persistence representation of a generated
 * letter in the Briefix system.  It is managed exclusively by the repository
 * layer and must not be exposed directly to the service or controller layers.
 * The repository converts between this entity and the domain model
 * {@link com.briefix.letter.model.Letter} via
 * {@link com.briefix.letter.mapper.LetterMapper}.</p>
 *
 * <p>Notable persistence characteristics:</p>
 * <ul>
 *   <li>The {@code senderSnapshot} and {@code recipientSnapshot} fields are
 *       stored as PostgreSQL {@code jsonb} columns using Hibernate's
 *       {@code @JdbcTypeCode(SqlTypes.JSON)} mapping.  This allows the full
 *       snapshot objects to be stored and retrieved without requiring separate
 *       normalised tables.</li>
 *   <li>The {@code template} field is stored as the enum constant name
 *       (STRING strategy) for readability in the database.</li>
 *   <li>The {@code createdAt} timestamp is auto-populated by the
 *       {@link #onCreate()} lifecycle callback and is never modified
 *       afterwards ({@code updatable = false}).</li>
 * </ul>
 *
 * <p><strong>Thread safety:</strong> not thread-safe.  Instances are intended
 * to be used within a single JPA persistence context and must not be shared
 * across threads.</p>
 */
@Entity
@Table(name = "letters")
public class LetterEntity {

    /**
     * Surrogate primary key of the letter, generated as a random UUID by the
     * database on first insert.  Marked {@code updatable = false} to prevent
     * accidental modification after creation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who owns this letter.  Loaded lazily to avoid unnecessary joins
     * when only the letter's scalar data is required.  The foreign key column
     * {@code user_id} is mandatory.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * User-supplied subject or title of the letter.  Used in the letter header
     * and as the basis for the PDF download file name.  Must not be blank.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Full text body of the letter as entered by the user.  Stored as a
     * TEXT column to accommodate letters of arbitrary length.  Must not be
     * blank.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * The date printed on the letter.  Defaults to the current date at
     * generation time if the user does not supply an explicit date.  Stored
     * as a SQL {@code DATE} type.
     */
    @Column(name = "letter_date", nullable = false)
    private LocalDate letterDate;

    /**
     * Immutable snapshot of the sender's profile data at the time the letter
     * was generated.  Stored as a PostgreSQL {@code jsonb} column so that the
     * letter is not affected by subsequent changes to the sender's profile.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sender_snapshot", columnDefinition = "jsonb", nullable = false)
    private SenderSnapshot senderSnapshot;

    /**
     * Immutable snapshot of the recipient's address data at the time the
     * letter was generated.  Stored as a PostgreSQL {@code jsonb} column so
     * that the letter is not affected by subsequent changes to or deletion of
     * the underlying contact record.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recipient_snapshot", columnDefinition = "jsonb", nullable = false)
    private RecipientSnapshot recipientSnapshot;

    /**
     * The visual design template applied when rendering this letter to PDF.
     * Stored as the enum constant name (STRING strategy) for database
     * readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LetterTemplate template;

    /**
     * URL pointing to a pre-generated PDF file stored in external object
     * storage.  {@code null} when the PDF is generated on-the-fly and not
     * stored externally.
     */
    @Column(name = "pdf_url")
    private String pdfUrl;

    /**
     * UUID of the sender profile used when generating this letter.
     * Stored to allow re-loading the current logo when re-rendering the PDF.
     * Nullable for backward compatibility with letters saved before this field was added.
     */
    @Column(name = "profile_id")
    private UUID profileId;

    /**
     * Timestamp recording when this letter entity was first persisted.  Set
     * automatically by the {@link #onCreate()} lifecycle callback; never
     * modified afterwards ({@code updatable = false}).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback invoked immediately before the entity is first
     * inserted into the database.  Initialises {@code createdAt} to the
     * current system time if it has not already been set.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // --- Getters ---

    /**
     * Returns the surrogate primary key of this letter.
     *
     * @return the UUID identifier, or {@code null} if the entity has not yet
     *         been persisted
     */
    public UUID getId() { return id; }

    /**
     * Returns the {@link UserEntity} that owns this letter.
     *
     * @return the owning user entity; never {@code null} for a persisted entity
     */
    public UserEntity getUser() { return user; }

    /**
     * Returns the subject or title of this letter.
     *
     * @return the letter title; never {@code null} for a valid entity
     */
    public String getTitle() { return title; }

    /**
     * Returns the full text body of this letter.
     *
     * @return the letter body; never {@code null} for a valid entity
     */
    public String getBody() { return body; }

    /**
     * Returns the date printed on this letter.
     *
     * @return the letter date; never {@code null} for a valid entity
     */
    public LocalDate getLetterDate() { return letterDate; }

    /**
     * Returns the sender snapshot containing the profile data at generation
     * time.
     *
     * @return the sender snapshot; never {@code null} for a valid entity
     */
    public SenderSnapshot getSenderSnapshot() { return senderSnapshot; }

    /**
     * Returns the recipient snapshot containing the address data at generation
     * time.
     *
     * @return the recipient snapshot; never {@code null} for a valid entity
     */
    public RecipientSnapshot getRecipientSnapshot() { return recipientSnapshot; }

    /**
     * Returns the visual design template used when rendering this letter to PDF.
     *
     * @return the {@link LetterTemplate}; never {@code null}
     */
    public LetterTemplate getTemplate() { return template; }

    /**
     * Returns the URL of the pre-generated PDF file, if available.
     *
     * @return the PDF URL, or {@code null} if the PDF has not been stored
     *         externally
     */
    public String getPdfUrl() { return pdfUrl; }

    /**
     * Returns the UUID of the sender profile that was active when this letter was
     * generated.
     *
     * <p>This value is retained so that the PDF renderer can re-load the associated
     * logo image when the letter is re-rendered on demand.  May be {@code null} for
     * letters created before this field was introduced or when no profile was selected.</p>
     *
     * @return the profile UUID, or {@code null} if not associated with a profile
     */
    public UUID getProfileId() { return profileId; }

    /**
     * Returns the timestamp at which this letter was first persisted.
     *
     * @return the creation timestamp; never {@code null} for a persisted entity
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---

    /**
     * Sets the surrogate primary key of this letter.
     *
     * @param id the UUID to assign; typically managed by the JPA provider
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * Sets the owning user of this letter.
     *
     * @param user the {@link UserEntity} that owns this letter; must not be
     *             {@code null}
     */
    public void setUser(UserEntity user) { this.user = user; }

    /**
     * Sets the subject or title of this letter.
     *
     * @param title the letter title; must not be blank
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Sets the full text body of this letter.
     *
     * @param body the letter body; must not be blank
     */
    public void setBody(String body) { this.body = body; }

    /**
     * Sets the date printed on this letter.
     *
     * @param letterDate the letter date; must not be {@code null}
     */
    public void setLetterDate(LocalDate letterDate) { this.letterDate = letterDate; }

    /**
     * Sets the sender snapshot for this letter.
     *
     * @param senderSnapshot the snapshot of the sender's profile data; must
     *                       not be {@code null}
     */
    public void setSenderSnapshot(SenderSnapshot senderSnapshot) { this.senderSnapshot = senderSnapshot; }

    /**
     * Sets the recipient snapshot for this letter.
     *
     * @param recipientSnapshot the snapshot of the recipient's address data;
     *                          must not be {@code null}
     */
    public void setRecipientSnapshot(RecipientSnapshot recipientSnapshot) { this.recipientSnapshot = recipientSnapshot; }

    /**
     * Sets the visual design template for this letter.
     *
     * @param template the {@link LetterTemplate} to apply; must not be
     *                 {@code null}
     */
    public void setTemplate(LetterTemplate template) { this.template = template; }

    /**
     * Sets the URL of the pre-generated PDF file.
     *
     * @param pdfUrl the URL of the stored PDF, or {@code null} to clear the
     *               reference
     */
    public void setPdfUrl(String pdfUrl) { this.pdfUrl = pdfUrl; }

    /**
     * Sets the UUID of the sender profile associated with this letter.
     *
     * @param profileId the profile UUID to associate, or {@code null} to clear the
     *                  association
     */
    public void setProfileId(UUID profileId) { this.profileId = profileId; }

    /**
     * Sets the creation timestamp of this letter.  In normal operation this
     * value is managed by the {@link #onCreate()} JPA callback and should not
     * be set manually.
     *
     * @param createdAt the timestamp to assign; must not be {@code null} for a
     *                  valid persisted entity
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
