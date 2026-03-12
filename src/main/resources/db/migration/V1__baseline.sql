-- =============================================================================
-- V1__baseline.sql  –  Briefix schema baseline
--
-- This script captures the full schema as it existed before Flyway was
-- introduced.  It is executed only on a CLEAN database (i.e. a fresh
-- environment where no tables exist yet).
--
-- On an EXISTING database (production / staging) Flyway is configured with
--   spring.flyway.baseline-on-migrate: true
--   spring.flyway.baseline-version: 1
-- which marks this version as already applied and skips its execution,
-- preserving all existing data.
-- =============================================================================

-- Enable the pgcrypto extension so uuid_generate_v4() is available if needed
-- (Spring Boot / Hibernate uses gen_random_uuid() via UUID GenerationType, but
--  pgcrypto is a safe, harmless addition.)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- -----------------------------------------------------------------------------
-- users
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                          UUID         NOT NULL DEFAULT gen_random_uuid(),
    email                       VARCHAR(255) NOT NULL,
    password_hash               VARCHAR(255),
    provider                    VARCHAR(255) NOT NULL,
    provider_id                 VARCHAR(255),
    is_email_verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    full_name                   VARCHAR(255) NOT NULL,
    phone                       VARCHAR(255),
    plan                        VARCHAR(255) NOT NULL DEFAULT 'STANDARD',
    role                        VARCHAR(255) NOT NULL DEFAULT 'ROLE_USER',
    created_at                  TIMESTAMP    NOT NULL,
    verification_token          VARCHAR(255),
    verification_token_expiry   TIMESTAMP,
    password_reset_token        VARCHAR(255),
    password_reset_token_expiry TIMESTAMP,
    billing_name                VARCHAR(255),
    billing_street              VARCHAR(255),
    billing_street_no           VARCHAR(255),
    billing_zip                 VARCHAR(255),
    billing_city                VARCHAR(255),
    billing_country             VARCHAR(255),
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

-- -----------------------------------------------------------------------------
-- contacts
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS contacts (
    id                       UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL,
    type                     VARCHAR(255) NOT NULL,
    company_name             VARCHAR(255),
    contact_person           VARCHAR(255),
    contact_person_salutation VARCHAR(255),
    department               VARCHAR(255),
    first_name               VARCHAR(255),
    last_name                VARCHAR(255),
    salutation               VARCHAR(255),
    street                   VARCHAR(255),
    street_number            VARCHAR(255),
    postal_code              VARCHAR(255),
    city                     VARCHAR(255),
    country                  VARCHAR(255),
    email                    VARCHAR(255),
    phone                    VARCHAR(255),
    created_at               TIMESTAMP    NOT NULL,
    CONSTRAINT pk_contacts PRIMARY KEY (id),
    CONSTRAINT fk_contacts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- -----------------------------------------------------------------------------
-- profiles
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS profiles (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID         NOT NULL,
    profile_label     VARCHAR(255) NOT NULL,
    is_default        BOOLEAN      NOT NULL DEFAULT FALSE,
    type              VARCHAR(255) NOT NULL,
    salutation        VARCHAR(255),
    title             VARCHAR(255),
    first_name        VARCHAR(255),
    last_name         VARCHAR(255),
    company_name      VARCHAR(255),
    department        VARCHAR(255),
    street            VARCHAR(255),
    street_number     VARCHAR(255),
    postal_code       VARCHAR(255),
    city              VARCHAR(255),
    country           VARCHAR(255) NOT NULL DEFAULT 'Deutschland',
    vat_id            VARCHAR(255),
    tax_number        VARCHAR(255),
    managing_director VARCHAR(255),
    register_court    VARCHAR(255),
    register_number   VARCHAR(255),
    iban              VARCHAR(255),
    bic               VARCHAR(255),
    bank_name         VARCHAR(255),
    website           VARCHAR(255),
    phone             VARCHAR(255),
    fax               VARCHAR(255),
    email             VARCHAR(255),
    contact_person    VARCHAR(255),
    logo              BYTEA,
    logo_content_type VARCHAR(50),
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT pk_profiles PRIMARY KEY (id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- -----------------------------------------------------------------------------
-- letters
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS letters (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id            UUID         NOT NULL,
    title              VARCHAR(255) NOT NULL,
    body               TEXT         NOT NULL,
    letter_date        DATE         NOT NULL,
    sender_snapshot    JSONB        NOT NULL,
    recipient_snapshot JSONB        NOT NULL,
    template           VARCHAR(255) NOT NULL,
    pdf_url            VARCHAR(255),
    profile_id         UUID,
    created_at         TIMESTAMP    NOT NULL,
    CONSTRAINT pk_letters PRIMARY KEY (id),
    CONSTRAINT fk_letters_user FOREIGN KEY (user_id) REFERENCES users (id)
);
