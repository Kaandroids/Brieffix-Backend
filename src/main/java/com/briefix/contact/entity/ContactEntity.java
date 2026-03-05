package com.briefix.contact.entity;

import com.briefix.profile.model.PartyType;
import com.briefix.user.entity.UserEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity that maps to the {@code contacts} database table.
 *
 * <p>{@code ContactEntity} is the persistence representation of a contact in the
 * Briefix system.  It is managed exclusively by the repository layer and must
 * not be exposed directly to higher layers (service or controller).  The
 * repository converts between this entity and the domain model {@code Contact}
 * via {@code ContactMapper}.</p>
 *
 * <p>Each contact is owned by exactly one user, expressed through a
 * many-to-one relationship to {@link UserEntity}.  The owning user's identifier
 * is also stored in a dedicated column ({@code user_id}) so that scoped queries
 * can be executed without joining to the users table.</p>
 *
 * <p>The {@code createdAt} timestamp is automatically populated in the
 * {@link #onCreate()} lifecycle callback and is never updated afterwards
 * ({@code updatable = false}).</p>
 *
 * <p><strong>Thread safety:</strong> not thread-safe.  Instances are intended
 * to be used within a single JPA persistence context and should not be shared
 * across threads.</p>
 */
@Entity
@Table(name = "contacts")
public class ContactEntity {

    /**
     * Surrogate primary key of the contact, generated as a random UUID by the
     * database on first insert.  The column is marked {@code updatable = false}
     * to prevent accidental modification after creation.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user who owns this contact.  Loaded lazily to avoid unnecessary joins
     * when only the contact's scalar data is required.  The foreign key column
     * {@code user_id} is mandatory.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * Discriminator indicating whether the contact represents an individual
     * person or a company.  Stored as the enum constant name (STRING strategy)
     * for readability in the database.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType type;

    /**
     * Name of the company; populated when {@code type} is {@code COMPANY}.
     * May be {@code null} for individual contacts.
     */
    @Column(name = "company_name")
    private String companyName;

    /**
     * Full name of the primary contact person within the company.  Populated
     * when {@code type} is {@code COMPANY}.  May be {@code null}.
     */
    @Column(name = "contact_person")
    private String contactPerson;

    /**
     * Formal salutation for the contact person (e.g. "Herr", "Frau").
     * Used when addressing correspondence to a named individual at a company.
     */
    @Column(name = "contact_person_salutation")
    private String contactPersonSalutation;

    /**
     * Department or organisational unit of the contact within their company.
     * Optional; may be {@code null}.
     */
    private String department;

    /**
     * Given (first) name of an individual contact.  Populated when
     * {@code type} is {@code INDIVIDUAL}.  May be {@code null} for companies.
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Family (last) name of an individual contact.  Populated when
     * {@code type} is {@code INDIVIDUAL}.  May be {@code null} for companies.
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Formal salutation for the contact (e.g. "Herr", "Frau", "Divers").
     * Used when generating the opening line of a letter.
     */
    private String salutation;

    /**
     * Street name component of the contact's postal address.
     */
    private String street;

    /**
     * House or building number component of the contact's postal address.
     */
    @Column(name = "street_number")
    private String streetNumber;

    /**
     * Postal or ZIP code component of the contact's mailing address.
     */
    @Column(name = "postal_code")
    private String postalCode;

    /**
     * City component of the contact's mailing address.
     */
    private String city;

    /**
     * Country component of the contact's mailing address.
     */
    private String country;

    /**
     * Electronic mail address of the contact.  Optional; may be {@code null}.
     */
    private String email;

    /**
     * Telephone number of the contact.  Optional; may be {@code null}.
     */
    private String phone;

    /**
     * Timestamp recording when this entity was first persisted.  Set
     * automatically by the {@link #onCreate()} lifecycle callback; never
     * modified afterwards ({@code updatable = false}).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback invoked before the entity is first inserted into
     * the database.  If {@code createdAt} has not already been set (e.g. when
     * migrating existing data), it is initialised to the current system time.
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Getters ---

    /**
     * Returns the surrogate primary key of this contact.
     *
     * @return the UUID identifier, or {@code null} if the entity has not yet
     *         been persisted
     */
    public UUID getId() { return id; }

    /**
     * Returns the {@link UserEntity} that owns this contact.
     *
     * @return the owning user entity; never {@code null} for a persisted entity
     */
    public UserEntity getUser() { return user; }

    /**
     * Returns the type discriminator that indicates whether this contact is an
     * individual or a company.
     *
     * @return the {@link PartyType}; never {@code null}
     */
    public PartyType getType() { return type; }

    /**
     * Returns the company name of this contact.
     *
     * @return the company name, or {@code null} if not applicable
     */
    public String getCompanyName() { return companyName; }

    /**
     * Returns the name of the primary contact person within the company.
     *
     * @return the contact person's full name, or {@code null} if not applicable
     */
    public String getContactPerson() { return contactPerson; }

    /**
     * Returns the formal salutation for the contact person within the company.
     *
     * @return the contact person salutation, or {@code null} if not applicable
     */
    public String getContactPersonSalutation() { return contactPersonSalutation; }

    /**
     * Returns the department of the contact within their organisation.
     *
     * @return the department name, or {@code null} if not set
     */
    public String getDepartment() { return department; }

    /**
     * Returns the given (first) name of the contact.
     *
     * @return the first name, or {@code null} for company contacts
     */
    public String getFirstName() { return firstName; }

    /**
     * Returns the family (last) name of the contact.
     *
     * @return the last name, or {@code null} for company contacts
     */
    public String getLastName() { return lastName; }

    /**
     * Returns the formal salutation used when addressing the contact.
     *
     * @return the salutation string, or {@code null} if not set
     */
    public String getSalutation() { return salutation; }

    /**
     * Returns the street name of the contact's postal address.
     *
     * @return the street name, or {@code null} if not set
     */
    public String getStreet() { return street; }

    /**
     * Returns the house/building number of the contact's postal address.
     *
     * @return the street number, or {@code null} if not set
     */
    public String getStreetNumber() { return streetNumber; }

    /**
     * Returns the postal code of the contact's mailing address.
     *
     * @return the postal code, or {@code null} if not set
     */
    public String getPostalCode() { return postalCode; }

    /**
     * Returns the city of the contact's mailing address.
     *
     * @return the city name, or {@code null} if not set
     */
    public String getCity() { return city; }

    /**
     * Returns the country of the contact's mailing address.
     *
     * @return the country name, or {@code null} if not set
     */
    public String getCountry() { return country; }

    /**
     * Returns the email address of the contact.
     *
     * @return the email address, or {@code null} if not set
     */
    public String getEmail() { return email; }

    /**
     * Returns the phone number of the contact.
     *
     * @return the phone number, or {@code null} if not set
     */
    public String getPhone() { return phone; }

    /**
     * Returns the timestamp at which this contact was first persisted.
     *
     * @return the creation timestamp; never {@code null} for a persisted entity
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---

    /**
     * Sets the surrogate primary key of this contact.
     *
     * @param id the UUID to assign; typically managed by the JPA provider
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * Sets the owning user of this contact.
     *
     * @param user the {@link UserEntity} that owns this contact; must not be
     *             {@code null}
     */
    public void setUser(UserEntity user) { this.user = user; }

    /**
     * Sets the type discriminator for this contact.
     *
     * @param type the {@link PartyType} indicating individual or company;
     *             must not be {@code null}
     */
    public void setType(PartyType type) { this.type = type; }

    /**
     * Sets the company name for this contact.
     *
     * @param companyName the company name; may be {@code null}
     */
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    /**
     * Sets the primary contact person's full name within the company.
     *
     * @param contactPerson the contact person's name; may be {@code null}
     */
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    /**
     * Sets the formal salutation for the contact person within the company.
     *
     * @param contactPersonSalutation the salutation string; may be {@code null}
     */
    public void setContactPersonSalutation(String contactPersonSalutation) { this.contactPersonSalutation = contactPersonSalutation; }

    /**
     * Sets the department of the contact within their organisation.
     *
     * @param department the department name; may be {@code null}
     */
    public void setDepartment(String department) { this.department = department; }

    /**
     * Sets the given (first) name of the contact.
     *
     * @param firstName the first name; may be {@code null} for company contacts
     */
    public void setFirstName(String firstName) { this.firstName = firstName; }

    /**
     * Sets the family (last) name of the contact.
     *
     * @param lastName the last name; may be {@code null} for company contacts
     */
    public void setLastName(String lastName) { this.lastName = lastName; }

    /**
     * Sets the formal salutation used when addressing the contact.
     *
     * @param salutation the salutation string; may be {@code null}
     */
    public void setSalutation(String salutation) { this.salutation = salutation; }

    /**
     * Sets the street name of the contact's postal address.
     *
     * @param street the street name; may be {@code null}
     */
    public void setStreet(String street) { this.street = street; }

    /**
     * Sets the house/building number of the contact's postal address.
     *
     * @param streetNumber the street number; may be {@code null}
     */
    public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }

    /**
     * Sets the postal code of the contact's mailing address.
     *
     * @param postalCode the postal code; may be {@code null}
     */
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    /**
     * Sets the city of the contact's mailing address.
     *
     * @param city the city name; may be {@code null}
     */
    public void setCity(String city) { this.city = city; }

    /**
     * Sets the country of the contact's mailing address.
     *
     * @param country the country name; may be {@code null}
     */
    public void setCountry(String country) { this.country = country; }

    /**
     * Sets the email address of the contact.
     *
     * @param email the email address; may be {@code null}
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Sets the phone number of the contact.
     *
     * @param phone the phone number; may be {@code null}
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Sets the creation timestamp of this contact.  In normal operation this
     * value is managed by the {@link #onCreate()} JPA callback and should not
     * be set manually.
     *
     * @param createdAt the timestamp to assign; must not be {@code null} for a
     *                  valid persisted entity
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
