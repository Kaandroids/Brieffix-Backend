package com.briefix.user.mapper;

import com.briefix.user.dto.UserDto;
import com.briefix.user.entity.UserEntity;
import com.briefix.user.model.User;
import org.springframework.stereotype.Component;

/**
 * Mapper responsible for converting between the {@link User} domain model,
 * its JPA persistence entity {@link UserEntity}, and its outbound DTO {@link UserDto}.
 *
 * <p>This component acts as an anti-corruption layer between the domain model
 * and the persistence/presentation layers. It ensures that sensitive domain fields
 * (e.g., {@code passwordHash}, {@code providerId}) are never inadvertently exposed
 * through outbound API representations, and that infrastructure details such as
 * JPA annotations never bleed into domain logic.</p>
 *
 * <p>Manual mapping is used here by design to maintain full, explicit control over
 * field visibility and transformation rules, and to avoid hidden behavior introduced
 * by reflection-based mapping frameworks such as MapStruct or ModelMapper.</p>
 *
 * <p>Thread-safety: This class is stateless and is registered as a singleton
 * Spring {@code @Component}. It is safe for concurrent use across all threads.</p>
 */
@Component
public class UserMapper {

    /**
     * Converts a {@link User} domain record to a {@link UserDto} suitable for API responses.
     *
     * <p>Sensitive fields such as {@code passwordHash} and {@code providerId}
     * are intentionally excluded from the resulting DTO to prevent credential leakage.
     * Only fields that are safe to expose to authenticated API consumers are mapped.</p>
     *
     * @param user the domain model to convert; must not be {@code null}
     * @return a sanitized {@link UserDto} safe for use in the presentation layer
     */
    public UserDto toDto(User user) {
        return new UserDto(
                user.id(),
                user.email(),
                user.provider(),
                user.isEmailVerified(),
                user.fullName(),
                user.phone(),
                user.plan(),
                user.createdAt()
        );
    }

    /**
     * Converts a {@link UserEntity} JPA persistence entity to a {@link User} domain record.
     *
     * <p>All fields, including sensitive ones such as {@code passwordHash} and
     * {@code providerId}, are mapped to allow the domain layer to perform full
     * business logic such as authentication and authorization checks.</p>
     *
     * @param entity the JPA entity to convert; must not be {@code null}
     * @return a fully populated {@link User} domain record representing the persisted state
     */
    public User toModel(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getEmail(),
                entity.getPasswordHash(),
                entity.getProvider(),
                entity.getProviderId(),
                entity.isEmailVerified(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getPlan(),
                entity.getCreatedAt(),
                entity.getVerificationToken(),
                entity.getVerificationTokenExpiry()
        );
    }

    /**
     * Converts a {@link User} domain record to a {@link UserEntity} JPA entity
     * intended for persistence operations (insert or update).
     *
     * <p>All fields, including {@code passwordHash} and {@code providerId}, are
     * mapped because this conversion is used exclusively within the persistence layer
     * where complete data fidelity is required. The resulting entity is not managed
     * by a JPA context and must be explicitly saved via the repository.</p>
     *
     * @param user the domain model to convert; must not be {@code null}
     * @return a detached {@link UserEntity} populated with all fields from the domain record,
     *         ready to be passed to {@link com.briefix.user.repository.UserJpaRepository#save(Object)}
     */
    public UserEntity toEntity(User user) {
        var entity = new UserEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setPasswordHash(user.passwordHash());
        entity.setProvider(user.provider());
        entity.setProviderId(user.providerId());
        entity.setEmailVerified(user.isEmailVerified());
        entity.setFullName(user.fullName());
        entity.setPhone(user.phone());
        entity.setPlan(user.plan());
        entity.setCreatedAt(user.createdAt());
        entity.setVerificationToken(user.verificationToken());
        entity.setVerificationTokenExpiry(user.verificationTokenExpiry());
        return entity;
    }
}
