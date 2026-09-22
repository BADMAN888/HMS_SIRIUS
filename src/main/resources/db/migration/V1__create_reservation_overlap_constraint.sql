CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservations
    ADD CONSTRAINT reservations_room_dates_no_overlap
        EXCLUDE USING gist (
        room_id WITH =,
        daterange(check_in_date, check_out_date, '[)') WITH &&
        )
        WHERE (status IN ('RESERVED', 'CONFIRMED', 'CHECKED_IN'));