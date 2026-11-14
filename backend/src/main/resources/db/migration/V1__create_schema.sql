-- Hotel Booking Portal schema (MySQL 8.0.16+ so CHECK constraints are enforced)

CREATE TABLE hotels (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(120)  NOT NULL,
    location        VARCHAR(80)   NOT NULL,
    description     VARCHAR(1000) NOT NULL,
    amenities       VARCHAR(500)  NOT NULL,
    room_type       VARCHAR(80)   NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    total_rooms     INT           NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_hotels_price CHECK (price_per_night > 0),
    CONSTRAINT chk_hotels_rooms CHECK (total_rooms > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_hotels_location ON hotels (location);

-- One row per hotel per night that has at least one booking.
-- booked_rooms <= total_rooms is enforced by the database as a final safety net against overbooking.
CREATE TABLE room_inventory (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id     BIGINT NOT NULL,
    stay_date    DATE   NOT NULL,
    total_rooms  INT    NOT NULL,
    booked_rooms INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_inventory_hotel_date UNIQUE (hotel_id, stay_date),
    CONSTRAINT fk_inventory_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_inventory_booked CHECK (booked_rooms >= 0 AND booked_rooms <= total_rooms)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_inventory_date ON room_inventory (stay_date);

CREATE TABLE bookings (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    reference       VARCHAR(12)   NOT NULL,
    hotel_id        BIGINT        NOT NULL,
    guest_name      VARCHAR(100)  NOT NULL,
    guest_email     VARCHAR(150)  NOT NULL,
    check_in        DATE          NOT NULL,
    check_out       DATE          NOT NULL,
    nights          INT           NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    total_amount    DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20)   NOT NULL,
    created_at      DATETIME(6)   NOT NULL,
    cancelled_at    DATETIME(6)   NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_bookings_reference UNIQUE (reference),
    CONSTRAINT fk_bookings_hotel FOREIGN KEY (hotel_id) REFERENCES hotels (id),
    CONSTRAINT chk_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT chk_bookings_nights CHECK (nights > 0),
    CONSTRAINT chk_bookings_total CHECK (total_amount > 0),
    CONSTRAINT chk_bookings_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_bookings_hotel_dates ON bookings (hotel_id, check_in, check_out);
CREATE INDEX idx_bookings_email ON bookings (guest_email);
