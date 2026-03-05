package com.briefix.contact.mapper;

import com.briefix.contact.dto.ContactDto;
import com.briefix.contact.entity.ContactEntity;
import com.briefix.contact.model.Contact;
import com.briefix.user.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Spring-managed component responsible for converting between the different
 * representations of a contact in the Briefix application.
 *
 * <p>The contact domain uses three distinct types, each suited to a different
 * layer of the application:</p>
 * <ul>
 *   <li>{@link Contact} – the immutable domain model used between the service
 *       and repository layers.</li>
 *   <li>{@link ContactEntity} – the JPA-managed persistence entity that maps
 *       to the {@code contacts} database table.</li>
 *   <li>{@link ContactDto} – the read-facing Data Transfer Object serialised
 *       into API responses.</li>
 * </ul>
 *
 * <p>{@code ContactMapper} provides the three pairwise conversions that are
 * needed at layer boundaries.  All conversions are pure data translations; no
 * business logic is applied.</p>
 *
 * <p><strong>Thread safety:</strong> this component is stateless and therefore
 * inherently thread-safe.  It is safe to inject as a singleton-scoped Spring
 * bean.</p>
 */
@Component
public class ContactMapper {

    /**
     * Converts a domain model {@link Contact} into a {@link ContactDto}
     * suitable for serialisation in an API response.
     *
     * <p>All fields are copied directly from the source record; no
     * transformation of values takes place.</p>
     *
     * @param contact the domain model to convert; must not be {@code null}
     * @return a new {@link ContactDto} populated with the same field values as
     *         the supplied {@code contact}
     */
    public ContactDto toDto(Contact contact) {
        return new ContactDto(
                contact.id(),
                contact.userId(),
                contact.type(),
                contact.companyName(),
                contact.contactPerson(),
                contact.contactPersonSalutation(),
                contact.department(),
                contact.firstName(),
                contact.lastName(),
                contact.salutation(),
                contact.street(),
                contact.streetNumber(),
                contact.postalCode(),
                contact.city(),
                contact.country(),
                contact.email(),
                contact.phone(),
                contact.createdAt()
        );
    }

    /**
     * Converts a JPA {@link ContactEntity} into the immutable domain model
     * {@link Contact}.
     *
     * <p>The owning user's identifier is extracted from the lazily-loaded
     * {@code UserEntity} via {@code entity.getUser().getId()}.  The caller
     * must therefore ensure that the persistence context is still active (i.e.
     * the entity has not been detached) when this method is invoked.</p>
     *
     * @param entity the JPA entity to convert; must not be {@code null} and
     *               must have its {@code user} association initialised
     * @return a new {@link Contact} domain record populated with the entity's
     *         field values
     */
    public Contact toModel(ContactEntity entity) {
        return new Contact(
                entity.getId(),
                entity.getUser().getId(),
                entity.getType(),
                entity.getCompanyName(),
                entity.getContactPerson(),
                entity.getContactPersonSalutation(),
                entity.getDepartment(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getSalutation(),
                entity.getStreet(),
                entity.getStreetNumber(),
                entity.getPostalCode(),
                entity.getCity(),
                entity.getCountry(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCreatedAt()
        );
    }

    /**
     * Converts a domain model {@link Contact} and a resolved {@link UserEntity}
     * into a JPA {@link ContactEntity} ready for persistence.
     *
     * <p>The {@link UserEntity} must be supplied separately because the domain
     * model only carries the user's UUID, not the full entity reference required
     * by JPA to maintain the many-to-one relationship.</p>
     *
     * @param contact the domain model providing the contact's field values;
     *                must not be {@code null}
     * @param user    the managed {@link UserEntity} that will be set as the
     *                owner of the resulting entity; must not be {@code null}
     * @return a new {@link ContactEntity} populated with values from
     *         {@code contact} and linked to {@code user}
     */
    public ContactEntity toEntity(Contact contact, UserEntity user) {
        var entity = new ContactEntity();
        entity.setId(contact.id());
        entity.setUser(user);
        entity.setType(contact.type());
        entity.setCompanyName(contact.companyName());
        entity.setContactPerson(contact.contactPerson());
        entity.setContactPersonSalutation(contact.contactPersonSalutation());
        entity.setDepartment(contact.department());
        entity.setFirstName(contact.firstName());
        entity.setLastName(contact.lastName());
        entity.setSalutation(contact.salutation());
        entity.setStreet(contact.street());
        entity.setStreetNumber(contact.streetNumber());
        entity.setPostalCode(contact.postalCode());
        entity.setCity(contact.city());
        entity.setCountry(contact.country());
        entity.setEmail(contact.email());
        entity.setPhone(contact.phone());
        entity.setCreatedAt(contact.createdAt());
        return entity;
    }
}
