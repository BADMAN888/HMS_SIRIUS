ALTER TABLE hotels
    ADD COLUMN IF NOT EXISTS early_check_in_time TIME,
    ADD COLUMN IF NOT EXISTS late_check_out_time TIME;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS hotel_id BIGINT REFERENCES hotels(id);

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS hotel_id BIGINT REFERENCES hotels(id);

UPDATE users
SET hotel_id = (SELECT MIN(id) FROM hotels)
WHERE hotel_id IS NULL
  AND role = 'USER'
  AND (SELECT COUNT(*) FROM hotels) = 1;

UPDATE profiles guest_profile
SET hotel_id = (
    SELECT MIN(guest_hotel.hotel_id)
    FROM (
        SELECT room.hotel_id
        FROM reservations reservation
        JOIN rooms room ON room.id = reservation.room_id
        WHERE reservation.primary_guest_id = guest_profile.id
        UNION
        SELECT room.hotel_id
        FROM reservation_guests reservation_guest
        JOIN reservations reservation ON reservation.id = reservation_guest.reservation_id
        JOIN rooms room ON room.id = reservation.room_id
        WHERE reservation_guest.guest_id = guest_profile.id
    ) guest_hotel
    HAVING COUNT(DISTINCT guest_hotel.hotel_id) = 1
)
WHERE guest_profile.hotel_id IS NULL;

CREATE TABLE IF NOT EXISTS service_components (
    id BIGSERIAL PRIMARY KEY,
    rate_id BIGINT REFERENCES rates(id),
    parent_facility_id BIGINT REFERENCES facilities(id),
    facility_id BIGINT NOT NULL REFERENCES facilities(id),
    price_source VARCHAR(30) NOT NULL,
    quantity_basis VARCHAR(40) NOT NULL,
    multiplier NUMERIC(10, 3) NOT NULL,
    included BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_service_component_one_parent CHECK (
        (rate_id IS NOT NULL AND parent_facility_id IS NULL)
        OR (rate_id IS NULL AND parent_facility_id IS NOT NULL)
    ),
    CONSTRAINT ck_service_component_positive_multiplier CHECK (multiplier > 0)
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_service_component_one_parent') THEN
        ALTER TABLE service_components ADD CONSTRAINT ck_service_component_one_parent CHECK (
            (rate_id IS NOT NULL AND parent_facility_id IS NULL)
            OR (rate_id IS NULL AND parent_facility_id IS NOT NULL)
        );
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_service_component_positive_multiplier') THEN
        ALTER TABLE service_components ADD CONSTRAINT ck_service_component_positive_multiplier CHECK (multiplier > 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_service_component_rate
    ON service_components(rate_id, active, sort_order);
CREATE INDEX IF NOT EXISTS idx_service_component_parent
    ON service_components(parent_facility_id, active, sort_order);

ALTER TABLE accruals
    ADD COLUMN IF NOT EXISTS service_component_id BIGINT REFERENCES service_components(id),
    ADD COLUMN IF NOT EXISTS source_rate_component_id BIGINT REFERENCES service_components(id),
    ADD COLUMN IF NOT EXISTS included_component BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS component_price_source VARCHAR(30),
    ADD COLUMN IF NOT EXISTS component_quantity_basis VARCHAR(40),
    ADD COLUMN IF NOT EXISTS component_multiplier NUMERIC(10, 3);

-- Convert every existing rate to a data-driven accommodation component.
INSERT INTO facilities (hotel_id, name, unit, type, tax_status, active)
SELECT DISTINCT category.hotel_id, 'Проживання', 'ніч', 'STANDARD', NULL, TRUE
FROM rates rate
JOIN room_categories category ON category.id = rate.category_id
WHERE NOT EXISTS (
    SELECT 1
    FROM facilities facility
    WHERE facility.hotel_id = category.hotel_id
      AND facility.name = 'Проживання'
      AND facility.type = 'STANDARD'
);

INSERT INTO service_components (
    rate_id, parent_facility_id, facility_id, price_source,
    quantity_basis, multiplier, included, sort_order, active
)
SELECT rate.id, NULL, facility.id, 'RATE_PRICE', 'PER_NIGHT', 1.000, TRUE, 0, TRUE
FROM rates rate
JOIN room_categories category ON category.id = rate.category_id
JOIN LATERAL (
    SELECT id FROM facilities facility
    WHERE facility.hotel_id = category.hotel_id
      AND facility.name = 'Проживання'
      AND facility.type = 'STANDARD'
    ORDER BY id
    LIMIT 1
) facility ON TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM service_components component
    WHERE component.rate_id = rate.id
      AND component.price_source = 'RATE_PRICE'
);

ALTER TABLE rates DROP COLUMN IF EXISTS breakfast_included;
