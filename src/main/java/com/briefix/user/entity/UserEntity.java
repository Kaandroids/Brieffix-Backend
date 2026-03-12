package com.briefix.user.entity;

import com.briefix.user.model.AuthProvider;
import com.briefix.user.model.UserPlan;
import com.briefix.user.model.UserRole;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA persistence entity mapping to the {@code users} table in PostgreSQL.
 *
 * <p>This class is an infrastructure concern and must not be used directly
 * outside the persistence layer. All domain logic operates on the
 * {@link com.briefix.user.model.User} record, which is produced and consumed
 * by {@link com.briefix.user.mapper.UserMapper}.</p>
 *
 * <p>The entity is managed by Spring Data JPA via
 * {@link com.briefix.user.repository.UserJpaRepository}. Direct access by
 * service or controller classes violates the layered architecture and should
 * be avoided.</p>
 *
 * <p>Thread-safety: JPA entities are not thread-safe. Each entity instance
 * should be used within a single persistence context and not shared across
 * concurrent transactions.</p>
 */
@Entity
@Table(name = "users")
public class UserEntity {

    /**
     * The primary key of the user record. Generated as a UUID v4 by the JPA provider
     * at insert time. This column is non-updatable to prevent accidental reassignment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /**
     * The user's primary email address. Must be unique across the entire {@code users} table
     * to prevent duplicate account registration. Used as the principal name during authentication.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The bcrypt-hashed password for locally authenticated users. This field is {@code null}
     * for users who registered via an OAuth2 provider ({@link AuthProvider#GOOGLE} or
     * {@link AuthProvider#APPLE}). Never store or log the raw plaintext password.
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * The authentication provider through which this account was created or linked.
     * Stored as a string in the database to ensure schema stability across enum changes.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    /**
     * The subject identifier ({@code sub} claim) issued by the OAuth2 identity provider.
     * This value is {@code null} for locally authenticated users. Used to correlate
     * provider sessions with Briefix accounts during token exchange.
     */
    @Column(name = "provider_id")
    private String providerId;

    /**
     * Indicates whether the user has confirmed ownership of their email address
     * by completing an email verification flow. Defaults to {@code false} on account creation.
     */
    @Column(name = "is_email_verified", nullable = false)
    private boolean isEmailVerified = false;

    /**
     * The user's full display name as provided during registration. Used throughout
     * the UI and in generated documents such as invoices and briefs.
     */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /**
     * An optional contact phone number for the user. Not validated at the entity level;
     * format validation is enforced by the domain model and request DTOs.
     */
    private String phone;

    /**
     * The subscription plan tier currently assigned to this user. Stored as a string
     * and defaults to {@link UserPlan#STANDARD} for all new accounts. The column-level
     * default ensures consistency even when rows are inserted outside the application layer.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'STANDARD'")
    private UserPlan plan = UserPlan.STANDARD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ROLE_USER'")
    private UserRole role = UserRole.ROLE_USER;

    /**
     * The timestamp at which this user record was first persisted. Set automatically
     * by the {@link #onCreate()} lifecycle callback and is non-updatable to preserve
     * an accurate audit trail.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * A one-time UUID token sent to the user via email to prove ownership of their
     * email address. Set at registration and on each resend request; cleared (set to
     * {@code null}) upon successful verification. {@code null} for OAuth2 users and
     * for users who have already verified their email.
     */
    @Column(name = "verification_token")
    private String verificationToken;

    /**
     * The UTC timestamp at which {@link #verificationToken} expires. The token is
     * valid for 24 hours from the moment it is generated. A token whose expiry is in
     * the past must be treated as invalid even if it still exists in the database.
     * {@code null} when there is no active verification token.
     */
    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;

    @Column(name = "billing_name")
    private String billingName;

    @Column(name = "billing_street")
    private String billingStreet;

    @Column(name = "billing_street_no")
    private String billingStreetNo;

    @Column(name = "billing_zip")
    private String billingZip;

    @Column(name = "billing_city")
    private String billingCity;

    @Column(name = "billing_country")
    private String billingCountry;

    /**
     * JPA lifecycle callback invoked before the entity is first inserted into the database.
     *
     * <p>Ensures that {@code createdAt} is always populated with the current server time
     * if it has not already been set programmatically. This guards against accidental
     * null values when entities are constructed without setting this field explicitly.</p>
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // --- Getters ---

    /**
     * Returns the unique identifier of this user entity.
     *
     * @return the UUID primary key; {@code null} before the entity is first persisted
     */
    public UUID getId() { return id; }

    /**
     * Returns the user's primary email address.
     *
     * @return the email address string; never {@code null} for persisted entities
     */
    public String getEmail() { return email; }

    /**
     * Returns the bcrypt-hashed password for locally authenticated users.
     *
     * @return the password hash, or {@code null} if the user authenticated via OAuth2
     */
    public String getPasswordHash() { return passwordHash; }

    /**
     * Returns the authentication provider associated with this account.
     *
     * @return the {@link AuthProvider} enum value; never {@code null} for persisted entities
     */
    public AuthProvider getProvider() { return provider; }

    /**
     * Returns the OAuth2 provider's subject identifier for this user.
     *
     * @return the provider-issued subject ID, or {@code null} for locally authenticated users
     */
    public String getProviderId() { return providerId; }

    /**
     * Returns whether the user's email address has been verified.
     *
     * @return {@code true} if the email has been confirmed, {@code false} otherwise
     */
    public boolean isEmailVerified() { return isEmailVerified; }

    /**
     * Returns the user's full display name.
     *
     * @return the full name string; never {@code null} for persisted entities
     */
    public String getFullName() { return fullName; }

    /**
     * Returns the user's optional phone number.
     *
     * @return the phone number string, or {@code null} if not provided
     */
    public String getPhone() { return phone; }

    /**
     * Returns the subscription plan tier assigned to this user.
     *
     * @return the {@link UserPlan} enum value; defaults to {@link UserPlan#STANDARD}
     */
    public UserPlan getPlan() { return plan; }

    /**
     * Returns the timestamp when this user record was created.
     *
     * @return the creation timestamp; set automatically on first insert
     */
    public LocalDateTime getCreatedAt() { return createdAt; }

    // --- Setters ---

    /**
     * Sets the unique identifier for this entity. Typically used only by the
     * mapper when reconstructing an entity from a domain model that already has an ID.
     *
     * @param id the UUID to assign as the primary key
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * Sets the user's primary email address.
     *
     * @param email the email address to assign; must be unique across the {@code users} table
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * Sets the bcrypt-hashed password. Should only be called with a properly hashed
     * value; never store raw plaintext passwords via this setter.
     *
     * @param passwordHash the bcrypt hash of the user's password, or {@code null} for OAuth2 users
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /**
     * Sets the authentication provider for this user account.
     *
     * @param provider the {@link AuthProvider} to assign; must not be {@code null}
     */
    public void setProvider(AuthProvider provider) { this.provider = provider; }

    /**
     * Sets the OAuth2 provider's subject identifier.
     *
     * @param providerId the provider-issued subject ID, or {@code null} for local users
     */
    public void setProviderId(String providerId) { this.providerId = providerId; }

    /**
     * Sets the email verification status for this user.
     *
     * @param emailVerified {@code true} if the user's email has been confirmed; {@code false} otherwise
     */
    public void setEmailVerified(boolean emailVerified) { this.isEmailVerified = emailVerified; }

    /**
     * Sets the user's full display name.
     *
     * @param fullName the full name to assign; must not be {@code null} or blank
     */
    public void setFullName(String fullName) { this.fullName = fullName; }

    /**
     * Sets the user's optional phone number.
     *
     * @param phone the phone number to assign, or {@code null} to clear it
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * Sets the subscription plan tier for this user.
     *
     * @param plan the {@link UserPlan} to assign; must not be {@code null}
     */
    public void setPlan(UserPlan plan) { this.plan = plan; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    /**
     * Sets the creation timestamp. Normally managed automatically by {@link #onCreate()};
     * this setter exists for the mapper to restore the original timestamp during update operations.
     *
     * @param createdAt the creation timestamp to assign
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * Returns the pending email-verification token for this user.
     *
     * @return the verification token string, or {@code null} if no active token exists
     */
    public String getVerificationToken() { return verificationToken; }

    /**
     * Sets the email-verification token. Pass {@code null} to clear the token after
     * successful verification.
     *
     * @param verificationToken the new verification token, or {@code null} to clear it
     */
    public void setVerificationToken(String verificationToken) { this.verificationToken = verificationToken; }

    /**
     * Returns the expiry timestamp of the current email-verification token.
     *
     * @return the expiry timestamp, or {@code null} if no active token exists
     */
    public LocalDateTime getVerificationTokenExpiry() { return verificationTokenExpiry; }

    /**
     * Sets the expiry timestamp for the email-verification token. Pass {@code null} to
     * clear the expiry when the token is cleared after successful verification.
     *
     * @param verificationTokenExpiry the new expiry timestamp, or {@code null} to clear it
     */
    public void setVerificationTokenExpiry(LocalDateTime verificationTokenExpiry) { this.verificationTokenExpiry = verificationTokenExpiry; }

    public String getPasswordResetToken() { return passwordResetToken; }
    public void setPasswordResetToken(String passwordResetToken) { this.passwordResetToken = passwordResetToken; }

    public LocalDateTime getPasswordResetTokenExpiry() { return passwordResetTokenExpiry; }
    public void setPasswordResetTokenExpiry(LocalDateTime passwordResetTokenExpiry) { this.passwordResetTokenExpiry = passwordResetTokenExpiry; }

    public String getBillingName() { return billingName; }
    public void setBillingName(String billingName) { this.billingName = billingName; }

    public String getBillingStreet() { return billingStreet; }
    public void setBillingStreet(String billingStreet) { this.billingStreet = billingStreet; }

    public String getBillingStreetNo() { return billingStreetNo; }
    public void setBillingStreetNo(String billingStreetNo) { this.billingStreetNo = billingStreetNo; }

    public String getBillingZip() { return billingZip; }
    public void setBillingZip(String billingZip) { this.billingZip = billingZip; }

    public String getBillingCity() { return billingCity; }
    public void setBillingCity(String billingCity) { this.billingCity = billingCity; }

    public String getBillingCountry() { return billingCountry; }
    public void setBillingCountry(String billingCountry) { this.billingCountry = billingCountry; }
}
