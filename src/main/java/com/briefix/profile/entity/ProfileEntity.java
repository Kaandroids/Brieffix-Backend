package com.briefix.profile.entity;

import com.briefix.profile.model.PartyType;
import com.briefix.user.entity.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA persistence entity mapping to the {@code profiles} table in PostgreSQL.
 *
 * <p>This class is an infrastructure concern and must not be used directly outside
 * the persistence layer. Domain logic operates exclusively on the
 * {@link com.briefix.profile.model.Profile} record, which is produced and consumed
 * by {@link com.briefix.profile.mapper.ProfileMapper}.</p>
 *
 * <p>Each profile entity is associated with exactly one {@link UserEntity} through
 * a many-to-one relationship. The association is lazily loaded to avoid unnecessary
 * joins when only scalar profile fields are needed.</p>
 *
 * <p>The entity is managed by Spring Data JPA via
 * {@link com.briefix.profile.repository.ProfileJpaRepository}. Direct access
 * by service or controller classes violates the layered architecture.</p>
 *
 * <p>Thread-safety: JPA entities are not thread-safe. Each instance should be
 * used within a single persistence context and not shared across concurrent threads.</p>
 */
@Entity
@Table(name = "profiles")
public class ProfileEntity {

    /**
     * The primary key of the profile record. Generated as a UUID v4 by the JPA provider
     * at insert time. Marked as non-updatable to prevent accidental primary key reassignment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The owning user of this profile. Mapped as a many-to-one relationship to
     * the {@code users} table via the {@code user_id} foreign key column.
     * Fetched lazily to avoid unnecessary joins in queries that do not require
     * full user data alongside the profile.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * A short, user-defined label identifying this profile (e.g., "My Company", "Freelance Account").
     * Displayed in the UI to help users distinguish between multiple profiles. Must not be null.
     */
    @Column(name = "profile_label", nullable = false)
    private String profileLabel;

    /**
     * Indicates whether this profile is the user's default profile. When {@code true},
     * this profile is pre-selected in document creation and other flows that require
     * a sender identity. Defaults to {@code false} on creation.
     */
    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    /**
     * The legal classification of the party represented by this profile.
     * Stored as a string via {@code @Enumerated(EnumType.STRING)} for schema readability.
     * Drives which fields are relevant and how the profile is rendered on documents.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType type;

    /**
     * Optional formal salutation for individual profiles (e.g., "Herr", "Frau", "Divers").
     * Rendered at the beginning of names on outgoing documents.
     */
    private String salutation;

    /**
     * Optional academic or professional title (e.g., "Dr.", "Prof.", "Ing.").
     * Displayed before the person's name on formal documents.
     */
    private String title;

    /**
     * The first (given) name of the individual or primary contact person.
     * Required for {@link PartyType#INDIVIDUAL} profiles; optional for organizations.
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * The last (family) name of the individual or primary contact person.
     * Required for {@link PartyType#INDIVIDUAL} profiles; optional for organizations.
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * The legal registered name of the organization or company.
     * Required for {@link PartyType#ORGANIZATION} profiles. {@code null} for individual profiles.
     */
    @Column(name = "company_name")
    private String companyName;

    /**
     * An optional department or division name within the organization.
     * Used to further qualify the organizational address on documents.
     */
    private String department;

    /**
     * The street name component of the postal address (excluding the house number).
     */
    private String street;

    /**
     * The house or building number component of the postal address.
     * Stored separately from {@code street} to support address formatting conventions.
     */
    @Column(name = "street_number")
    private String streetNumber;

    /**
     * The postal code (PLZ in Germany) of the address. Format depends on the country.
     */
    @Column(name = "postal_code")
    private String postalCode;

    /**
     * The city or municipality name of the postal address.
     */
    private String city;

    /**
     * The country of the postal address. Defaults to {@code "Deutschland"} when not
     * explicitly specified, reflecting the platform's primary market. The column-level
     * default ensures consistency even for rows inserted outside the application.
     */
    @Column(nullable = false)
    private String country = "Deutschland";

    /**
     * The VAT identification number (Umsatzsteuer-Identifikationsnummer) of the party,
     * typically in the format {@code DE123456789}. Required for cross-border EU invoicing.
     */
    @Column(name = "vat_id")
    private String vatId;

    /**
     * The German tax number (Steuernummer) assigned by the local Finanzamt.
     * Required on domestic invoices in Germany when a VAT ID is not applicable.
     */
    @Column(name = "tax_number")
    private String taxNumber;

    /**
     * The full name(s) of the managing director(s) ({@code Geschäftsführer}) of a GmbH
     * or similar entity. Required by German commercial law to appear on business correspondence.
     */
    @Column(name = "managing_director")
    private String managingDirector;

    /**
     * The commercial register court ({@code Registergericht}) where the organization is registered,
     * e.g., "Amtsgericht München". Required for organization profiles on invoices and contracts.
     */
    @Column(name = "register_court")
    private String registerCourt;

    /**
     * The registration number in the commercial register ({@code Handelsregisternummer}),
     * e.g., "HRB 12345". Required for organization profiles on formal documents.
     */
    @Column(name = "register_number")
    private String registerNumber;

    /**
     * The International Bank Account Number (IBAN) used for payment information on invoices.
     * Conforms to ISO 13616.
     */
    private String iban;

    /**
     * The Bank Identifier Code (BIC / SWIFT code) associated with the bank account.
     * Typically required alongside the IBAN for SEPA transfers.
     */
    private String bic;

    /**
     * The human-readable name of the bank holding the account (e.g., "Deutsche Bank AG").
     * Displayed on invoices alongside the IBAN and BIC.
     */
    @Column(name = "bank_name")
    private String bankName;

    /**
     * An optional URL of the party's public website. Displayed as a hyperlink or
     * plain text on generated documents.
     */
    private String website;

    /**
     * A contact phone number for this profile. May differ from the owning user's
     * personal phone number stored on the user account.
     */
    private String phone;

    /**
     * An optional fax number for this profile. Included on documents for parties
     * that still use fax communication (common in German business contexts).
     */
    private String fax;

    /**
     * A contact email address displayed on generated documents. This field represents
     * the profile's business email and may differ from the user's account login email.
     */
    private String email;

    /**
     * The name of the designated contact person within an organization. Used when
     * correspondence should be addressed to a specific individual rather than the
     * organization as a whole.
     */
    @Column(name = "contact_person")
    private String contactPerson;

    /**
     * Binary content of the profile logo image, stored as a PostgreSQL {@code bytea} column.
     * {@code null} when no logo has been uploaded for this profile.
     * Logo is managed through dedicated upload/delete endpoints and is never modified
     * by the standard create/update profile flow.
     */
    @Lob
    @Column(name = "logo", columnDefinition = "bytea")
    private byte[] logo;

    /**
     * MIME type of the stored logo image (e.g. {@code "image/png"}, {@code "image/svg+xml"}).
     * Always populated when {@link #logo} is non-null; {@code null} otherwise.
     */
    @Column(name = "logo_content_type", length = 50)
    private String logoContentType;

    /**
     * The timestamp at which this profile was first created. Set automatically by the
     * {@link #onCreate()} lifecycle callback on insert and is non-updatable to preserve
     * an accurate audit trail. Never set by the client.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback invoked before this entity is first inserted into the database.
     *
     * <p>Ensures that {@code createdAt} is always populated with the current server time
     * if it has not already been set programmatically. This guards against accidental
     * null values when entities are constructed without explicitly setting this field.</p>
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Getters ---

    /**
     * Returns the unique identifier of this profile entity.
     *
     * @return the UUID primary key; {@code null} before the entity is first persisted
     */
    public UUID getId() { return id; }

    /**
     * Returns the {@link UserEntity} that owns this profile.
     *
     * @return the owning user entity; loaded lazily from the database
     */
    public UserEntity getUser() { return user; }

    /**
     * Returns the human-readable label assigned to this profile by the user.
     *
     * @return the profile label string; never {@code null} for persisted entities
     */
    public String getProfileLabel() { return profileLabel; }

    /**
     * Returns whether this is the user's default profile.
     *
     * @return {@code true} if this profile is the default; {@code false} otherwise
     */
    public boolean isDefault() { return isDefault; }

    /**
     * Returns the party type classification for this profile.
     *
     * @return the {@link PartyType} enum value; never {@code null} for persisted entities
     */
    public PartyType getType() { return type; }

    /**
     * Returns the optional salutation for this profile.
     *
     * @return the salutation string, or {@code null} if not set
     */
    public String getSalutation() { return salutation; }

    /**
     * Returns the optional academic or professional title for this profile.
     *
     * @return the title string, or {@code null} if not set
     */
    public String getTitle() { return title; }

    /**
     * Returns the first name of the individual or contact person for this profile.
     *
     * @return the first name string, or {@code null} if not set
     */
    public String getFirstName() { return firstName; }

    /**
     * Returns the last name of the individual or contact person for this profile.
     *
     * @return the last name string, or {@code null} if not set
     */
    public String getLastName() { return lastName; }

    /**
     * Returns the legal company name for organization profiles.
     *
     * @return the company name string, or {@code null} for individual profiles
     */
    public String getCompanyName() { return companyName; }

    /**
     * Returns the optional department or division within the organization.
     *
     * @return the department string, or {@code null} if not set
     */
    public String getDepartment() { return department; }

    /**
     * Returns the street name component of the address.
     *
     * @return the street name string, or {@code null} if not set
     */
    public String getStreet() { return street; }

    /**
     * Returns the house or building number component of the address.
     *
     * @return the street number string, or {@code null} if not set
     */
    public String getStreetNumber() { return streetNumber; }

    /**
     * Returns the postal code of the address.
     *
     * @return the postal code string, or {@code null} if not set
     */
    public String getPostalCode() { return postalCode; }

    /**
     * Returns the city of the address.
     *
     * @return the city name string, or {@code null} if not set
     */
    public String getCity() { return city; }

    /**
     * Returns the country of the address. Defaults to {@code "Deutschland"}.
     *
     * @return the country name string; never {@code null} for persisted entities
     */
    public String getCountry() { return country; }

    /**
     * Returns the VAT identification number for this profile.
     *
     * @return the VAT ID string, or {@code null} if not applicable
     */
    public String getVatId() { return vatId; }

    /**
     * Returns the German tax number (Steuernummer) for this profile.
     *
     * @return the tax number string, or {@code null} if not applicable
     */
    public String getTaxNumber() { return taxNumber; }

    /**
     * Returns the name(s) of the managing director(s) for organization profiles.
     *
     * @return the managing director string, or {@code null} if not set
     */
    public String getManagingDirector() { return managingDirector; }

    /**
     * Returns the commercial register court for this organization profile.
     *
     * @return the register court string, or {@code null} if not set
     */
    public String getRegisterCourt() { return registerCourt; }

    /**
     * Returns the commercial register entry number for this organization profile.
     *
     * @return the register number string, or {@code null} if not set
     */
    public String getRegisterNumber() { return registerNumber; }

    /**
     * Returns the IBAN for this profile's bank account.
     *
     * @return the IBAN string, or {@code null} if not set
     */
    public String getIban() { return iban; }

    /**
     * Returns the BIC/SWIFT code for this profile's bank account.
     *
     * @return the BIC string, or {@code null} if not set
     */
    public String getBic() { return bic; }

    /**
     * Returns the name of the bank associated with this profile's bank account.
     *
     * @return the bank name string, or {@code null} if not set
     */
    public String getBankName() { return bankName; }

    /**
     * Returns the website URL for this profile.
     *
     * @return the website URL string, or {@code null} if not set
     */
    public String getWebsite() { return website; }

    /**
     * Returns the contact phone number for this profile.
     *
     * @return the phone number string, or {@code null} if not set
     */
    public String getPhone() { return phone; }

    /**
     * Returns the fax number for this profile.
     *
     * @return the fax number string, or {@code null} if not set
     */
    public String getFax() { return fax; }

    /**
     * Returns the business contact email address for this profile.
     *
     * @return the email address string, or {@code null} if not set
     */
    public String getEmail() { return email; }

    /**
     * Returns the name of the designated contact person for this profile.
     *
     * @return the contact person name string, or {@code null} if not set
     */
    public String getContactPerson() { return contactPerson; }

    /**
     * Returns the timestamp when this profile record was first created.
     *
     * @return the creation timestamp; set automatically on first insert
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---

    /**
     * Sets the unique identifier for this entity. Typically used by the mapper
     * when reconstructing an entity from a domain model that already carries an ID.
     *
     * @param id the UUID to assign as the primary key
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * Sets the owning user entity for this profile.
     *
     * @param user the {@link UserEntity} to associate with this profile; must not be {@code null}
     */
    public void setUser(UserEntity user) { this.user = user; }

    /**
     * Sets the human-readable label for this profile.
     *
     * @param profileLabel the label to assign; must not be {@code null} or blank
     */
    public void setProfileLabel(String profileLabel) { this.profileLabel = profileLabel; }

    /**
     * Sets whether this profile is the user's default profile.
     *
     * @param isDefault {@code true} to mark this profile as default; {@code false} otherwise
     */
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }

    /**
     * Sets the party type classification for this profile.
     *
     * @param type the {@link PartyType} to assign; must not be {@code null}
     */
    public void setType(PartyType type) { this.type = type; }

    /**
     * Sets the optional salutation for this profile.
     *
     * @param salutation the salutation string to assign, or {@code null} to clear it
     */
    public void setSalutation(String salutation) { this.salutation = salutation; }

    /**
     * Sets the optional academic or professional title for this profile.
     *
     * @param title the title string to assign, or {@code null} to clear it
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Sets the first name of the individual or contact person for this profile.
     *
     * @param firstName the first name to assign, or {@code null} to clear it
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }

    /**
     * Sets the last name of the individual or contact person for this profile.
     *
     * @param lastName the last name to assign, or {@code null} to clear it
     */
    public void setLastName(String lastName) { this.lastName = lastName; }

    /**
     * Sets the legal company name for this organization profile.
     *
     * @param companyName the company name to assign, or {@code null} to clear it
     */
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    /**
     * Sets the optional department or division for this profile.
     *
     * @param department the department name to assign, or {@code null} to clear it
     */
    public void setDepartment(String department) { this.department = department; }

    /**
     * Sets the street name component of the address.
     *
     * @param street the street name to assign, or {@code null} to clear it
     */
    public void setStreet(String street) { this.street = street; }

    /**
     * Sets the house or building number component of the address.
     *
     * @param streetNumber the street number to assign, or {@code null} to clear it
     */
    public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }

    /**
     * Sets the postal code of the address.
     *
     * @param postalCode the postal code to assign, or {@code null} to clear it
     */
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    /**
     * Sets the city of the address.
     *
     * @param city the city name to assign, or {@code null} to clear it
     */
    public void setCity(String city) { this.city = city; }

    /**
     * Sets the country of the address.
     *
     * @param country the country name to assign; defaults to {@code "Deutschland"} if not specified
     */
    public void setCountry(String country) { this.country = country; }

    /**
     * Sets the VAT identification number for this profile.
     *
     * @param vatId the VAT ID to assign, or {@code null} to clear it
     */
    public void setVatId(String vatId) { this.vatId = vatId; }

    /**
     * Sets the German tax number for this profile.
     *
     * @param taxNumber the tax number to assign, or {@code null} to clear it
     */
    public void setTaxNumber(String taxNumber) { this.taxNumber = taxNumber; }

    /**
     * Sets the managing director name(s) for this organization profile.
     *
     * @param managingDirector the managing director name(s) to assign, or {@code null} to clear it
     */
    public void setManagingDirector(String managingDirector) { this.managingDirector = managingDirector; }

    /**
     * Sets the commercial register court for this organization profile.
     *
     * @param registerCourt the register court name to assign, or {@code null} to clear it
     */
    public void setRegisterCourt(String registerCourt) { this.registerCourt = registerCourt; }

    /**
     * Sets the commercial register entry number for this organization profile.
     *
     * @param registerNumber the register number to assign, or {@code null} to clear it
     */
    public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }

    /**
     * Sets the IBAN for this profile's bank account.
     *
     * @param iban the IBAN to assign, or {@code null} to clear it
     */
    public void setIban(String iban) { this.iban = iban; }

    /**
     * Sets the BIC/SWIFT code for this profile's bank account.
     *
     * @param bic the BIC to assign, or {@code null} to clear it
     */
    public void setBic(String bic) { this.bic = bic; }

    /**
     * Sets the name of the bank for this profile's bank account.
     *
     * @param bankName the bank name to assign, or {@code null} to clear it
     */
    public void setBankName(String bankName) { this.bankName = bankName; }

    /**
     * Sets the website URL for this profile.
     *
     * @param website the website URL to assign, or {@code null} to clear it
     */
    public void setWebsite(String website) { this.website = website; }

    /**
     * Sets the contact phone number for this profile.
     *
     * @param phone the phone number to assign, or {@code null} to clear it
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Sets the fax number for this profile.
     *
     * @param fax the fax number to assign, or {@code null} to clear it
     */
    public void setFax(String fax) { this.fax = fax; }

    /**
     * Sets the business contact email address for this profile.
     *
     * @param email the email address to assign, or {@code null} to clear it
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Sets the designated contact person name for this profile.
     *
     * @param contactPerson the contact person name to assign, or {@code null} to clear it
     */
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    /**
     * Returns the raw bytes of the profile logo image, or {@code null} if no logo has been uploaded.
     *
     * @return the logo byte array, or {@code null}
     */
    public byte[] getLogo() { return logo; }

    /**
     * Sets the raw bytes of the profile logo image.
     *
     * @param logo the logo byte array, or {@code null} to clear the logo
     */
    public void setLogo(byte[] logo) { this.logo = logo; }

    /**
     * Returns the MIME content type of the stored logo (e.g. {@code "image/png"}).
     *
     * @return the content type string, or {@code null} if no logo is stored
     */
    public String getLogoContentType() { return logoContentType; }

    /**
     * Sets the MIME content type of the logo image.
     *
     * @param logoContentType the content type to assign, or {@code null} to clear it
     */
    public void setLogoContentType(String logoContentType) { this.logoContentType = logoContentType; }

    /**
     * Sets the creation timestamp for this profile. Normally managed automatically
     * by the {@link #onCreate()} lifecycle callback; this setter exists for the mapper
     * to preserve the original timestamp during update operations.
     *
     * @param createdAt the creation timestamp to assign
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
