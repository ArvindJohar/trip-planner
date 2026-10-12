CREATE TABLE trips (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    travellers INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_budget NUMERIC(12, 2) NOT NULL,

    CONSTRAINT chk_trip_dates
        CHECK (start_date <= end_date),

    CONSTRAINT chk_trip_travellers
        CHECK (travellers > 0),

    CONSTRAINT chk_trip_budget
        CHECK (total_budget > 0)
);


CREATE TABLE destinations (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL,
    city VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    CONSTRAINT fk_destination_trip
        FOREIGN KEY (trip_id)
        REFERENCES trips(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_destination_dates
        CHECK (start_date <= end_date)
);


CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    destination_id BIGINT NOT NULL,
    city VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    cost NUMERIC(12, 2) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    CONSTRAINT fk_activity_destination
        FOREIGN KEY (destination_id)
        REFERENCES destinations(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_activity_times
        CHECK (start_time <= end_time),

    CONSTRAINT chk_activity_cost
        CHECK (cost >= 0)
);