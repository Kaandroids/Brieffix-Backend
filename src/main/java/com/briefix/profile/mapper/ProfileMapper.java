package com.briefix.profile.mapper;

import com.briefix.profile.dto.ProfileDto;
import com.briefix.profile.entity.ProfileEntity;
import com.briefix.profile.model.Profile;
import com.briefix.user.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for converting between the {@link Profile} domain model,
 * its JPA persistence entity {@link ProfileEntity}, and its outbound DTO {@link ProfileDto}.
 *
 * <p>This component acts as an anti-corruption layer between the domain, persistence,
 * and presentation layers of the profile domain. By handling all field-level translations
 * in one place, it ensures that neither JPA annotations nor API serialization concerns
 * bleed into domain logic.</p>
 *
 * <p>Manual mapping is used by design to give full, explicit control over how each
 * field is mapped, including the {@code country} defaulting logic applied during
 * entity construction. This avoids hidden behavior that reflection-based frameworks
 * such as MapStruct or ModelMapper might introduce.</p>
 *
 * <p>Note that {@link #toEntity(Profile, UserEntity)} requires a fully loaded
 * {@link UserEntity} rather than just a UUID, because JPA requires a managed entity
 * reference to populate the {@code user} foreign key on {@link ProfileEntity}.</p>
 *
 * <p>Thread-safety: This class is stateless and registered as a singleton Spring
 * {@code @Component}. It is safe for concurrent use across all threads.</p>
 */
@Component
public class ProfileMapper {

    /**
     * Converts a {@link Profile} domain record to a {@link ProfileDto} for API responses.
     *
     * <p>All fields from the domain model are mapped one-to-one to the DTO. No fields
     * are omitted or transformed in this direction since the profile DTO is not considered
     * sensitive (unlike the user DTO, which omits credential fields).</p>
     *
     * @param profile the domain model to convert; must not be {@code null}
     * @return a {@link ProfileDto} reflecting all fields of the given domain record
     */
    public ProfileDto toDto(Profile profile) {
        return new ProfileDto(
                profile.id(),
                profile.userId(),
                profile.profileLabel(),
                profile.isDefault(),
                profile.type(),
                profile.salutation(),
                profile.title(),
                profile.firstName(),
                profile.lastName(),
                profile.companyName(),
                profile.department(),
                profile.street(),
                profile.streetNumber(),
                profile.postalCode(),
                profile.city(),
                profile.country(),
                profile.vatId(),
                profile.taxNumber(),
                profile.managingDirector(),
                profile.registerCourt(),
                profile.registerNumber(),
                profile.iban(),
                profile.bic(),
                profile.bankName(),
                profile.website(),
                profile.phone(),
                profile.fax(),
                profile.email(),
                profile.contactPerson(),
                profile.createdAt(),
                profile.hasLogo()
        );
    }

    /**
     * Converts a {@link ProfileEntity} JPA persistence entity to a {@link Profile} domain record.
     *
     * <p>The owning user's UUID is extracted from the associated {@link UserEntity} via
     * {@link UserEntity#getId()}. This requires the {@code user} association to be initialized;
     * callers must ensure the entity is loaded within an active persistence context or that
     * the association is eagerly fetched, otherwise a {@link org.hibernate.LazyInitializationException}
     * may occur.</p>
     *
     * @param entity the JPA entity to convert; must not be {@code null},
     *               and its {@code user} association must be initialized
     * @return a fully populated {@link Profile} domain record reflecting the entity's state
     */
    public Profile toModel(ProfileEntity entity) {
        return new Profile(
                entity.getId(),
                entity.getUser().getId(),
                entity.getProfileLabel(),
                entity.isDefault(),
                entity.getType(),
                entity.getSalutation(),
                entity.getTitle(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getCompanyName(),
                entity.getDepartment(),
                entity.getStreet(),
                entity.getStreetNumber(),
                entity.getPostalCode(),
                entity.getCity(),
                entity.getCountry(),
                entity.getVatId(),
                entity.getTaxNumber(),
                entity.getManagingDirector(),
                entity.getRegisterCourt(),
                entity.getRegisterNumber(),
                entity.getIban(),
                entity.getBic(),
                entity.getBankName(),
                entity.getWebsite(),
                entity.getPhone(),
                entity.getFax(),
                entity.getEmail(),
                entity.getContactPerson(),
                entity.getCreatedAt(),
                entity.getLogo() != null
        );
    }

    /**
     * Converts a {@link Profile} domain record to a {@link ProfileEntity} JPA entity
     * ready for persistence operations (insert or update).
     *
     * <p>The provided {@link UserEntity} is set as the owning user association,
     * satisfying the JPA {@code @ManyToOne} relationship on {@link ProfileEntity}.
     * The caller is responsible for ensuring the {@code user} entity is managed within
     * the current persistence context to prevent detached entity exceptions.</p>
     *
     * <p>The {@code country} field falls back to {@code "Deutschland"} if the domain
     * model's value is {@code null}, preserving the system's default country convention
     * at the persistence boundary.</p>
     *
     * @param profile the domain model to convert; must not be {@code null}
     * @param user    the managed {@link UserEntity} to associate with the profile;
     *                must not be {@code null} and must be a valid entity reference
     * @return a detached {@link ProfileEntity} populated from the domain record and
     *         the provided user entity, ready to be saved via the JPA repository
     */
    public ProfileEntity toEntity(Profile profile, UserEntity user) {
        var entity = new ProfileEntity();
        entity.setId(profile.id());
        entity.setUser(user);
        entity.setProfileLabel(profile.profileLabel());
        entity.setDefault(profile.isDefault());
        entity.setType(profile.type());
        entity.setSalutation(profile.salutation());
        entity.setTitle(profile.title());
        entity.setFirstName(profile.firstName());
        entity.setLastName(profile.lastName());
        entity.setCompanyName(profile.companyName());
        entity.setDepartment(profile.department());
        entity.setStreet(profile.street());
        entity.setStreetNumber(profile.streetNumber());
        entity.setPostalCode(profile.postalCode());
        entity.setCity(profile.city());
        entity.setCountry(profile.country() != null ? profile.country() : "Deutschland");
        entity.setVatId(profile.vatId());
        entity.setTaxNumber(profile.taxNumber());
        entity.setManagingDirector(profile.managingDirector());
        entity.setRegisterCourt(profile.registerCourt());
        entity.setRegisterNumber(profile.registerNumber());
        entity.setIban(profile.iban());
        entity.setBic(profile.bic());
        entity.setBankName(profile.bankName());
        entity.setWebsite(profile.website());
        entity.setPhone(profile.phone());
        entity.setFax(profile.fax());
        entity.setEmail(profile.email());
        entity.setContactPerson(profile.contactPerson());
        entity.setCreatedAt(profile.createdAt());
        return entity;
    }
}
