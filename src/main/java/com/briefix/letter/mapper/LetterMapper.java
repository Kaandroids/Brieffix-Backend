package com.briefix.letter.mapper;

import com.briefix.letter.dto.LetterDto;
import com.briefix.letter.entity.LetterEntity;
import com.briefix.letter.model.Letter;
import com.briefix.user.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Spring-managed component responsible for converting between the different
 * representations of a letter in the Briefix application.
 *
 * <p>The letter domain uses three distinct types, each suited to a different
 * layer of the application:</p>
 * <ul>
 *   <li>{@link Letter} – the immutable domain model exchanged between the
 *       service and repository layers.</li>
 *   <li>{@link LetterEntity} – the JPA-managed persistence entity that maps
 *       to the {@code letters} database table, including JSONB columns for the
 *       sender and recipient snapshots.</li>
 *   <li>{@link LetterDto} – the read-facing Data Transfer Object serialised
 *       into API responses.</li>
 * </ul>
 *
 * <p>{@code LetterMapper} provides the three pairwise conversions needed at
 * layer boundaries.  All conversions are pure data translations; no business
 * logic is applied.</p>
 *
 * <p><strong>Thread safety:</strong> this component is stateless and therefore
 * inherently thread-safe.  It is safe to inject as a singleton-scoped Spring
 * bean.</p>
 */
@Component
public class LetterMapper {

    /**
     * Converts a domain model {@link Letter} into a {@link LetterDto} suitable
     * for serialisation in an API response.
     *
     * <p>All fields — including the nested {@code SenderSnapshot} and
     * {@code RecipientSnapshot} records — are copied directly from the source
     * record without any transformation.</p>
     *
     * @param letter the domain model to convert; must not be {@code null}
     * @return a new {@link LetterDto} populated with the same field values as
     *         the supplied {@code letter}
     */
    public LetterDto toDto(Letter letter) {
        return new LetterDto(
                letter.id(),
                letter.userId(),
                letter.title(),
                letter.body(),
                letter.letterDate(),
                letter.senderSnapshot(),
                letter.recipientSnapshot(),
                letter.template(),
                letter.pdfUrl(),
                letter.createdAt()
        );
    }

    /**
     * Converts a JPA {@link LetterEntity} into the immutable domain model
     * {@link Letter}.
     *
     * <p>The owning user's identifier is extracted from the lazily-loaded
     * {@code UserEntity} via {@code entity.getUser().getId()}.  The caller
     * must therefore ensure that the persistence context is still active (i.e.
     * the entity has not been detached) when this method is invoked, or that
     * the {@code user} association has already been initialised.</p>
     *
     * @param entity the JPA entity to convert; must not be {@code null} and
     *               must have its {@code user} association initialised
     * @return a new {@link Letter} domain record populated with the entity's
     *         field values, including the de-serialised snapshot objects
     */
    public Letter toModel(LetterEntity entity) {
        return new Letter(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTitle(),
                entity.getBody(),
                entity.getLetterDate(),
                entity.getSenderSnapshot(),
                entity.getRecipientSnapshot(),
                entity.getTemplate(),
                entity.getPdfUrl(),
                entity.getCreatedAt()
        );
    }

    /**
     * Converts a domain model {@link Letter} and a resolved {@link UserEntity}
     * into a JPA {@link LetterEntity} ready for persistence.
     *
     * <p>The {@link UserEntity} must be supplied separately because the domain
     * model only carries the user's UUID, not the full entity reference required
     * by JPA to maintain the many-to-one relationship.  The snapshot objects
     * ({@code SenderSnapshot} and {@code RecipientSnapshot}) are set directly
     * on the entity and will be serialised to JSONB by the Hibernate type
     * mapping.</p>
     *
     * @param letter the domain model providing the letter's field values;
     *               must not be {@code null}
     * @param user   the managed {@link UserEntity} that will be set as the
     *               owner of the resulting entity; must not be {@code null}
     * @return a new {@link LetterEntity} populated with values from
     *         {@code letter} and linked to {@code user}
     */
    public LetterEntity toEntity(Letter letter, UserEntity user) {
        var entity = new LetterEntity();
        entity.setId(letter.id());
        entity.setUser(user);
        entity.setTitle(letter.title());
        entity.setBody(letter.body());
        entity.setLetterDate(letter.letterDate());
        entity.setSenderSnapshot(letter.senderSnapshot());
        entity.setRecipientSnapshot(letter.recipientSnapshot());
        entity.setTemplate(letter.template());
        entity.setPdfUrl(letter.pdfUrl());
        entity.setCreatedAt(letter.createdAt());
        return entity;
    }
}
