CREATE TABLE housekeeping_plans (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id) ON DELETE CASCADE,
    service_date DATE NOT NULL,
    attendant_count INTEGER NOT NULL CHECK (attendant_count BETWEEN 1 AND 50),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_housekeeping_plan_hotel_date UNIQUE (hotel_id, service_date)
);

CREATE TABLE housekeeping_tasks (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES housekeeping_plans(id) ON DELETE CASCADE,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    departure_reservation_id BIGINT REFERENCES reservations(id) ON DELETE SET NULL,
    arrival_reservation_id BIGINT REFERENCES reservations(id) ON DELETE SET NULL,
    task_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attendant_number INTEGER NOT NULL CHECK (attendant_number BETWEEN 1 AND 50),
    priority INTEGER NOT NULL,
    estimated_minutes INTEGER NOT NULL CHECK (estimated_minutes > 0),
    notes VARCHAR(500),
    completed_at TIMESTAMP,
    CONSTRAINT uk_housekeeping_task_plan_room UNIQUE (plan_id, room_id)
);

CREATE INDEX idx_housekeeping_task_attendant
    ON housekeeping_tasks (plan_id, attendant_number, priority);
