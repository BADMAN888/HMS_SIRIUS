CREATE TABLE group_bookings (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(30) NOT NULL UNIQUE,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id),
    group_name VARCHAR(100) NOT NULL,
    organizer_name VARCHAR(150),
    contact_phone VARCHAR(30),
    contact_email VARCHAR(100),
    check_in TIMESTAMP NOT NULL,
    check_out TIMESTAMP NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL
);

ALTER TABLE reservations
    ADD COLUMN group_booking_id BIGINT REFERENCES group_bookings(id);

CREATE INDEX idx_group_bookings_hotel_checkin ON group_bookings(hotel_id, check_in);
CREATE INDEX idx_reservations_group_booking ON reservations(group_booking_id);
