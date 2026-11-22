ALTER TABLE movies
    ADD COLUMN age_rating VARCHAR(10),
    ADD COLUMN backdrop_url VARCHAR(500),
    ADD COLUMN source_key VARCHAR(120);

CREATE UNIQUE INDEX ux_movies_source_key
    ON movies (source_key)
    WHERE source_key IS NOT NULL;

ALTER TABLE promotions
    ADD COLUMN image_url VARCHAR(500),
    ADD COLUMN source_key VARCHAR(120);

CREATE UNIQUE INDEX ux_promotions_source_key
    ON promotions (source_key)
    WHERE source_key IS NOT NULL;

CREATE TABLE banners (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(120) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    subtitle TEXT,
    image_url VARCHAR(500) NOT NULL,
    target_path VARCHAR(500),
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_banners_display_order CHECK (display_order >= 0)
);

CREATE INDEX ix_banners_active_order ON banners (active, display_order, id);

CREATE TABLE combos (
    id BIGSERIAL PRIMARY KEY,
    source_key VARCHAR(120) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price NUMERIC(12, 2) NOT NULL,
    image_url VARCHAR(500),
    items_json JSONB,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_combos_price CHECK (price >= 0)
);

CREATE INDEX ix_combos_active ON combos (active, id);
