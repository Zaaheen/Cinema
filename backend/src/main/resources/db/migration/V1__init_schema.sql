CREATE TABLE roles (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(255) UNIQUE,
    phone         VARCHAR(20)  UNIQUE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role_id       BIGINT       NOT NULL REFERENCES roles (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'LOCKED'))
);

CREATE TABLE members (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    member_code     VARCHAR(20)  NOT NULL UNIQUE,
    birth_date      DATE,
    gender          VARCHAR(10),
    address         VARCHAR(255),
    identity_number VARCHAR(20),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash VARCHAR(64)  NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE movies (
    id               BIGSERIAL PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    genre            VARCHAR(100),
    release_date     DATE,
    duration_minutes INT          NOT NULL,
    version          VARCHAR(50)  NOT NULL DEFAULT '2D',
    cast_text        TEXT,
    director         VARCHAR(255),
    studio           VARCHAR(255),
    synopsis         TEXT,
    trailer_url      VARCHAR(500),
    poster_url       VARCHAR(500),
    status           VARCHAR(20)  NOT NULL DEFAULT 'COMING_SOON',
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_movies_duration CHECK (duration_minutes > 0),
    CONSTRAINT ck_movies_status CHECK (status IN ('COMING_SOON', 'NOW_SHOWING', 'STOPPED'))
);

CREATE INDEX ix_movies_status ON movies (status);

CREATE TABLE rooms (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(50) NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_rooms_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE seats (
    id          BIGSERIAL PRIMARY KEY,
    room_id     BIGINT      NOT NULL REFERENCES rooms (id) ON DELETE CASCADE,
    row_label   VARCHAR(5)  NOT NULL,
    seat_number INT         NOT NULL,
    seat_type   VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    status      VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ux_seats_position UNIQUE (room_id, row_label, seat_number),
    CONSTRAINT ck_seats_type CHECK (seat_type IN ('STANDARD', 'VIP')),
    CONSTRAINT ck_seats_status CHECK (status IN ('AVAILABLE', 'UNAVAILABLE')),
    CONSTRAINT ck_seats_number CHECK (seat_number > 0)
);

CREATE TABLE showtimes (
    id         BIGSERIAL PRIMARY KEY,
    movie_id   BIGINT      NOT NULL REFERENCES movies (id),
    room_id    BIGINT      NOT NULL REFERENCES rooms (id),
    starts_at  TIMESTAMPTZ NOT NULL,
    ends_at    TIMESTAMPTZ NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_showtimes_time CHECK (ends_at > starts_at),
    CONSTRAINT ck_showtimes_status CHECK (status IN ('SCHEDULED', 'ONGOING', 'FINISHED', 'CANCELLED'))
);

CREATE INDEX ix_showtimes_movie ON showtimes (movie_id, starts_at);
CREATE INDEX ix_showtimes_room ON showtimes (room_id, starts_at);

CREATE TABLE seat_prices (
    id            BIGSERIAL PRIMARY KEY,
    seat_type     VARCHAR(20)    NOT NULL,
    movie_version VARCHAR(50),
    price         NUMERIC(12, 2) NOT NULL,
    valid_from    DATE           NOT NULL,
    valid_to      DATE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_seat_prices_type CHECK (seat_type IN ('STANDARD', 'VIP')),
    CONSTRAINT ck_seat_prices_price CHECK (price >= 0),
    CONSTRAINT ck_seat_prices_range CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE TABLE promotions (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(255)   NOT NULL,
    description    TEXT,
    discount_type  VARCHAR(20)    NOT NULL,
    discount_value NUMERIC(12, 2) NOT NULL,
    starts_at      TIMESTAMPTZ    NOT NULL,
    ends_at        TIMESTAMPTZ    NOT NULL,
    status         VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_promotions_type CHECK (discount_type IN ('PERCENT', 'FIXED')),
    CONSTRAINT ck_promotions_value CHECK (discount_value >= 0),
    CONSTRAINT ck_promotions_range CHECK (ends_at > starts_at),
    CONSTRAINT ck_promotions_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'))
);

CREATE TABLE bookings (
    id              BIGSERIAL PRIMARY KEY,
    booking_code    VARCHAR(20)    NOT NULL UNIQUE,
    member_id       BIGINT         NOT NULL REFERENCES members (id),
    showtime_id     BIGINT         NOT NULL REFERENCES showtimes (id),
    status          VARCHAR(30)    NOT NULL DEFAULT 'PENDING_PAYMENT',
    subtotal        NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    points_used     INT            NOT NULL DEFAULT 0,
    total_amount    NUMERIC(12, 2) NOT NULL,
    expires_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_bookings_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_bookings_amounts CHECK (subtotal >= 0 AND discount_amount >= 0 AND total_amount >= 0 AND points_used >= 0)
);

CREATE INDEX ix_bookings_member ON bookings (member_id, created_at DESC);
CREATE INDEX ix_bookings_showtime ON bookings (showtime_id);
CREATE INDEX ix_bookings_expires ON bookings (status, expires_at);

CREATE TABLE booking_seats (
    id          BIGSERIAL PRIMARY KEY,
    booking_id  BIGINT         NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    showtime_id BIGINT         NOT NULL REFERENCES showtimes (id),
    seat_id     BIGINT         NOT NULL REFERENCES seats (id),
    unit_price  NUMERIC(12, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'HELD',
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_booking_seats_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT ck_booking_seats_price CHECK (unit_price >= 0)
);

CREATE UNIQUE INDEX ux_booking_seats_active
    ON booking_seats (showtime_id, seat_id)
    WHERE status IN ('HELD', 'CONFIRMED');
CREATE INDEX ix_booking_seats_booking ON booking_seats (booking_id);

CREATE TABLE booking_promotions (
    booking_id      BIGINT         NOT NULL REFERENCES bookings (id) ON DELETE CASCADE,
    promotion_id    BIGINT         NOT NULL REFERENCES promotions (id),
    discount_amount NUMERIC(12, 2) NOT NULL,
    PRIMARY KEY (booking_id, promotion_id),
    CONSTRAINT ck_booking_promotions_amount CHECK (discount_amount >= 0)
);

CREATE TABLE payments (
    id                 BIGSERIAL PRIMARY KEY,
    booking_id         BIGINT         NOT NULL REFERENCES bookings (id),
    method             VARCHAR(30)    NOT NULL,
    amount             NUMERIC(12, 2) NOT NULL,
    status             VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    provider_reference VARCHAR(100),
    paid_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_payments_method CHECK (method IN ('CASH', 'VNPAY', 'MOMO', 'SIMULATED')),
    CONSTRAINT ck_payments_status CHECK (status IN ('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')),
    CONSTRAINT ck_payments_amount CHECK (amount >= 0)
);

CREATE INDEX ix_payments_booking ON payments (booking_id);

CREATE TABLE tickets (
    id              BIGSERIAL PRIMARY KEY,
    booking_seat_id BIGINT      NOT NULL UNIQUE REFERENCES booking_seats (id) ON DELETE CASCADE,
    ticket_code     VARCHAR(30) NOT NULL UNIQUE,
    status          VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    issued_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    checked_in_at   TIMESTAMPTZ,
    CONSTRAINT ck_tickets_status CHECK (status IN ('ISSUED', 'CHECKED_IN', 'CANCELLED'))
);

CREATE TABLE point_transactions (
    id             BIGSERIAL PRIMARY KEY,
    member_id      BIGINT      NOT NULL REFERENCES members (id) ON DELETE CASCADE,
    booking_id     BIGINT      REFERENCES bookings (id),
    points         INT         NOT NULL,
    type           VARCHAR(20) NOT NULL,
    description    VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_point_transactions_type CHECK (type IN ('EARN', 'REDEEM', 'ADJUST')),
    CONSTRAINT ck_point_transactions_points CHECK (points <> 0)
);

CREATE INDEX ix_point_transactions_member ON point_transactions (member_id, created_at DESC);
CREATE UNIQUE INDEX ux_point_transactions_earn_booking
    ON point_transactions (booking_id)
    WHERE type = 'EARN';

CREATE TABLE audit_logs (
    id            BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT REFERENCES users (id),
    action        VARCHAR(50) NOT NULL,
    entity_type   VARCHAR(50) NOT NULL,
    entity_id     BIGINT,
    metadata      JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_logs_actor ON audit_logs (actor_user_id, created_at DESC);
