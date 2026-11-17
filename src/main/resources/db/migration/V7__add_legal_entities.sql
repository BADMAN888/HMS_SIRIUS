CREATE TABLE legal_entities (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(500) NOT NULL,
    short_name VARCHAR(255) NOT NULL,
    edrpou VARCHAR(8) NOT NULL,
    registration_date DATE,
    organizational_legal_form VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_legal_entity_edrpou UNIQUE (edrpou),
    CONSTRAINT ck_legal_entity_edrpou CHECK (edrpou ~ '^[0-9]{8}$')
);

INSERT INTO legal_entities (
    full_name,
    short_name,
    edrpou,
    registration_date,
    organizational_legal_form,
    active,
    created_at,
    updated_at
)
SELECT
    full_name,
    short_name,
    edrpou,
    registration_date,
    organizational_legal_form,
    active,
    created_at,
    updated_at
FROM companies;

ALTER TABLE companies ADD COLUMN legal_entity_id BIGINT;

UPDATE companies c
SET legal_entity_id = le.id
FROM legal_entities le
WHERE le.edrpou = c.edrpou;

ALTER TABLE companies ALTER COLUMN legal_entity_id SET NOT NULL;
ALTER TABLE companies
    ADD CONSTRAINT uk_company_legal_entity UNIQUE (legal_entity_id),
    ADD CONSTRAINT fk_company_legal_entity
        FOREIGN KEY (legal_entity_id) REFERENCES legal_entities (id);

ALTER TABLE companies DROP CONSTRAINT IF EXISTS uk_company_edrpou;
ALTER TABLE companies
    DROP COLUMN full_name,
    DROP COLUMN short_name,
    DROP COLUMN edrpou,
    DROP COLUMN registration_date,
    DROP COLUMN organizational_legal_form,
    DROP COLUMN created_at,
    DROP COLUMN updated_at;

CREATE TABLE hotels_legal_entities (
    hotel_id BIGINT NOT NULL REFERENCES hotels (id) ON DELETE CASCADE,
    legal_entity_id BIGINT NOT NULL REFERENCES legal_entities (id) ON DELETE CASCADE,
    PRIMARY KEY (hotel_id, legal_entity_id)
);

ALTER TABLE folios ADD COLUMN service_provider_id BIGINT;
ALTER TABLE folios
    ADD CONSTRAINT fk_folio_service_provider
        FOREIGN KEY (service_provider_id) REFERENCES legal_entities (id);
